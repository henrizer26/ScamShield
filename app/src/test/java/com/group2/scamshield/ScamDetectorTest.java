package com.group2.scamshield;

import com.group2.scamshield.utils.DetectionRule;
import com.group2.scamshield.utils.ScamDetector;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ScamDetectorTest {

    // ---------------------------------------------------------------- basics

    @Test
    public void ordinaryMessage_isSafe() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Nimefika salama nyumbani. Tutaonana kesho.", "+255700000000");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertEquals(0, result.score);
        assertFalse(result.shouldAlert());
    }

    @Test
    public void nullMessage_doesNotCrashAndIsSafe() {
        ScamDetector.ScamResult result = ScamDetector.detect(null, null);

        assertEquals(ScamDetector.SAFE, result.classification);
        assertEquals(0, result.score);
    }

    @Test
    public void ordinaryMoneyConversation_doesNotMatchTransferRule() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Nimetuma pesa ya kodi ya nyumba.", "+255700000000");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertFalse(result.matchedRules.contains("R04_MONEY_REQUEST"));
    }

    @Test
    public void moneyRequestAlone_isSuspicious() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Nitumie pesa kwa M-Pesa", "+255700000000");

        assertEquals(ScamDetector.SUSPICIOUS, result.classification);
        assertEquals(30, result.score);
        assertTrue(result.shouldAlert());
        assertTrue(result.matchedRules.contains("R04_MONEY_REQUEST"));
    }

    @Test
    public void knownScamSender_isHighConfidenceScam() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Wasiliana nasi kwa maelezo", "0614159000");

        assertEquals(ScamDetector.SCAM, result.classification);
        assertEquals(70, result.score);
        assertTrue(result.matchedRules.contains("R01_KNOWN_NUMBER"));
    }

    @Test
    public void score_isBoundedAtOneHundred() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Hongera umeshinda zawadi. Tuma pesa sasa na OTP kupitia "
                        + "https://fake.example. Akaunti yako imefungwa. Mtaalam wa tiba.",
                "0614159000");

        assertEquals(ScamDetector.MAX_SCORE, result.score);
    }

    // ------------------------------------------------- combinations of rules

    @Test
    public void credentialRequestWithLink_isScam() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Thibitisha OTP yako kupitia https://fake.example", "BANK");

        assertEquals(ScamDetector.SCAM, result.classification);
        assertTrue(result.score >= ScamDetector.SCAM_THRESHOLD);
        assertTrue(result.matchedRules.contains("R02_CREDENTIAL_REQUEST"));
        assertTrue(result.matchedRules.contains("R05_LINK"));
        assertTrue(hasRule(result, "C02_CREDENTIAL_LINK"));
    }

    @Test
    public void prizeWithPayment_isScamAndScoresAsDocumented() {
        // 30 prize + 30 payment request + 20 combination, plus 12 amount and 12
        // gift wording as supporting signals, which carries the total past the
        // ceiling: a message this explicit scores the maximum.
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Hongera umeshinda zawadi. Lipa shilingi 5000 sasa.", "PROMO");

        assertEquals(ScamDetector.SCAM, result.classification);
        assertEquals(ScamDetector.MAX_SCORE, result.score);
        assertTrue(hasRule(result, "C01_PRIZE_PAYMENT"));
    }

    private static final String BANK_PHISH =
            "Benki yako inasema akaunti yako imefungwa. "
                    + "Thibitisha PIN yako kupitia http://example.com";

    @Test
    public void institutionClaimFromPersonalNumber_addsMismatchBonus() {
        ScamDetector.ScamResult result = ScamDetector.detect(BANK_PHISH, "0715222333");

        assertTrue(hasRule(result, "C04_BRAND_MISMATCH"));
        assertEquals(ScamDetector.SCAM, result.classification);
    }

    @Test
    public void sameMessageFromRegisteredSenderId_doesNotRaiseMismatch() {
        ScamDetector.ScamResult result = ScamDetector.detect(BANK_PHISH, "CRDB");

        assertFalse(hasRule(result, "C04_BRAND_MISMATCH"));
    }

    @Test
    public void weakSignalsAlone_cannotReachScam() {
        // Link, amount, personal number, urgency and a brand name: five
        // supporting signals worth 64 raw, capped so they can only warn.
        ScamDetector.ScamResult result = ScamDetector.detect(
                "OFA MPYA LEO! Bofya www.duka.co.tz, bei ni Tsh 20,000, "
                        + "piga 0715222333 haraka, huduma ya Vodacom.", "DUKA");

        assertTrue(result.score < ScamDetector.SCAM_THRESHOLD);
        assertEquals(ScamDetector.SUSPICIOUS, result.classification);
    }

    // ------------------------------------------------------------- obfuscation

    @Test
    public void lookalikeCharacters_areFoldedBackBeforeMatching() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Tuma P1N yako ya M-Pesa sasa hivi", "0712000111");

        assertEquals("tuma pin yako ya m-pesa sasa hivi", result.canonicalMessage);
        assertEquals(ScamDetector.SCAM, result.classification);
        assertTrue(result.matchedRules.contains("R02_CREDENTIAL_REQUEST"));
    }

    @Test
    public void separatorsPushedBetweenLetters_areRemoved() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Ingiza 0.T.P yako kwenye http://bit.ly/xy", "0712000111");

        assertTrue(result.canonicalMessage.contains("otp"));
        assertTrue(result.matchedRules.contains("R02_CREDENTIAL_REQUEST"));
        assertTrue(result.matchedRules.contains("R11_UNSAFE_LINK"));
        assertEquals(ScamDetector.SCAM, result.classification);
    }

    @Test
    public void fullySpacedOutWords_areRejoinedAsWords() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "T u m a  p e s a  h a r a k a  kwa mpesa", "0712000111");

        assertEquals("tuma pesa haraka kwa mpesa", result.canonicalMessage);
        assertTrue(result.matchedRules.contains("R04_MONEY_REQUEST"));
        assertTrue(result.shouldAlert());
    }

    // ------------------------------------------------------ genuine traffic

    @Test
    public void bankSecurityAdvice_isNotReadAsACredentialRequest() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Your OTP is 483920. Do not share it with anyone.", "CRDB");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertFalse(result.matchedRules.contains("R02_CREDENTIAL_REQUEST"));
    }

    @Test
    public void swahiliSecurityAdvice_isSuppressed() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Hatutakuomba PIN yako. Usimpe mtu yeyote namba ya siri ya akaunti yako.",
                "NMB");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertTrue(result.matchedRules.contains("S01_SECURITY_ADVICE"));
    }

    @Test
    public void transactionReceipt_isSafe() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Umepokea Tsh 50,000 kutoka JOHN MAKALA. Muamala namba QR12345. "
                        + "Salio lako ni Tsh 62,300.", "M-PESA");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertTrue(result.matchedRules.contains("S02_TRANSACTION_RECEIPT"));
    }

    @Test
    public void copiedSecurityAdvice_cannotHideARealRequest() {
        // Scammers paste the bank's own warning under their phishing link.
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Akaunti yako imefungwa, thibitisha PIN hapa http://bit.ly/x. "
                        + "Hatutakuomba PIN yako.", "BANK");

        assertEquals(ScamDetector.SCAM, result.classification);
        assertTrue(result.matchedRules.contains("S01_SECURITY_ADVICE"));
        assertTrue(result.score >= ScamDetector.SCAM_THRESHOLD);
    }

    // ------------------------------------------------------------ link rules

    @Test
    public void shortenedLink_outweighsAnOrdinaryLink() {
        ScamDetector.ScamResult plain = ScamDetector.detect(
                "Soma habari hapa www.bbc.com/swahili", "+255715000111");
        ScamDetector.ScamResult hidden = ScamDetector.detect(
                "Soma habari hapa http://bit.ly/abc", "+255715000111");

        assertEquals(ScamDetector.SAFE, plain.classification);
        assertTrue(hidden.score > plain.score);
        assertTrue(hidden.matchedRules.contains("R11_UNSAFE_LINK"));
    }

    @Test
    public void brandLookalikeDomain_isDetectedButOfficialDomainIsNot() {
        ScamDetector.ScamResult fake = ScamDetector.detect(
                "Sasisha taarifa zako kwenye http://mpesa-verify.com", "INFO");
        ScamDetector.ScamResult real = ScamDetector.detect(
                "Angalia huduma zetu kwenye https://vodacom.co.tz", "VODACOM");

        assertTrue(fake.matchedRules.contains("R12_BRAND_LOOKALIKE"));
        assertFalse(real.matchedRules.contains("R12_BRAND_LOOKALIKE"));
    }

    // ----------------------------------------------------------- rule table

    @Test
    public void everyRuleHasAUniqueIdAndAnExplanation() {
        Set<String> ids = new HashSet<>();
        for (DetectionRule rule : ScamDetector.rules()) {
            assertTrue("duplicate rule id: " + rule.getId(), ids.add(rule.getId()));
            assertNotNull(rule.getExplanation());
            assertFalse(rule.getExplanation().isEmpty());
            assertNotEquals("rule " + rule.getId() + " has no weight", 0, rule.getWeight());
        }
        assertTrue(ids.size() >= 20);
    }

    @Test
    public void everyFiredRuleReportsTheTextThatFiredIt() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Hongera umeshinda zawadi, tuma pesa haraka", "0700111222");

        assertFalse(result.indicators.isEmpty());
        assertEquals("indicators and matchedRules must stay index-aligned",
                result.matchedRules.size(), result.indicators.size());

        // The reason shown to the user names no rule, so which entries are
        // combinations is read from the audit trail beside it.
        for (int i = 0; i < result.indicators.size(); i++) {
            String indicator = result.indicators.get(i);
            if (result.matchedRules.get(i).startsWith("C")) {
                continue; // combination rules quote no single fragment
            }
            assertTrue("indicator carries no evidence: " + indicator,
                    indicator.contains("\""));
        }
    }

    @Test
    public void normalizationAndTokenization_areStable() {
        String normalized = ScamDetector.normalize("  TUMA\t  PESA  SASA  ");
        assertEquals("tuma pesa sasa", normalized);
        assertEquals(3, ScamDetector.tokenize(normalized).size());
    }

    @Test
    public void accentsAndInvisibleCharacters_areRemoved() {
        assertEquals("pin", ScamDetector.normalize("P\u200bIN"));
        assertEquals("otp", ScamDetector.normalize("\u00d3TP"));
    }

    /**
     * Combination rules are asserted through the audit trail, not the on-screen
     * text: the indicators shown to the user carry reasons only, by design.
     */
    private boolean hasRule(ScamDetector.ScamResult result, String id) {
        return result.matchedRules.contains(id);
    }
}

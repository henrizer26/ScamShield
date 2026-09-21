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

    // Each of the three below is about the wording, so each is scored from an
    // ordinary mobile number. Sent from the bank's own sender ID they would be
    // answered by the trusted-sender gate before a single rule ran, and the
    // assertion about which rule fired would pass without meaning anything.
    // That the gate answers them is worth testing too, and is tested separately
    // in trustedSender_* below.

    @Test
    public void bankSecurityAdvice_isNotReadAsACredentialRequest() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Your OTP is 483920. Do not share it with anyone.", "0712000111");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertFalse(result.matchedRules.contains("R02_CREDENTIAL_REQUEST"));
    }

    @Test
    public void swahiliSecurityAdvice_isSuppressed() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Hatutakuomba PIN yako. Usimpe mtu yeyote namba ya siri ya akaunti yako.",
                "0712000111");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertTrue(result.matchedRules.contains("S01_SECURITY_ADVICE"));
    }

    @Test
    public void transactionReceipt_isSafe() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Umepokea Tsh 50,000 kutoka JOHN MAKALA. Muamala namba QR12345. "
                        + "Salio lako ni Tsh 62,300.", "0712000111");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertTrue(result.matchedRules.contains("S02_TRANSACTION_RECEIPT"));
    }

    // ------------------------------------------------- the trusted-sender gate

    @Test
    public void registeredSenderId_isSafeWithoutScoringTheWording() {
        // The wording a real bank alert has to be allowed to use: a credential
        // word, a deadline and an amount. Scored, it reads as a phishing text.
        String body = "Ndugu mteja, salio lako ni Tsh 62,300. Usimpe mtu yeyote "
                + "PIN yako. Thibitisha muamala haraka leo hii.";

        for (String sender : new String[]{"NMB", "CRDB", "AIRTEL", "YAS", "M-PESA",
                "Airtel Money", "mixx by yas", "vodacom"}) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertEquals(sender + " should be trusted",
                    ScamDetector.SAFE, result.classification);
            assertEquals(sender + " should not be scored", 0, result.score);
            assertTrue(sender + " should say why it was trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
            assertFalse(sender + " should give the screen a reason",
                    result.indicators.isEmpty());
        }
    }

    @Test
    public void shortCode_isSafe() {
        ScamDetector.ScamResult result = ScamDetector.detect(
                "Thibitisha akaunti yako haraka, tuma PIN yako sasa hivi.", "15040");

        assertEquals(ScamDetector.SAFE, result.classification);
        assertTrue(result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
    }

    /**
     * Every sender ID in the project's "ScamShield Tanzania Incoming SMS
     * Whitelist" table, spelled the way the table spells it.
     *
     * <p>Listed one alias per line rather than folded into the registry's own
     * shape, so that this test still fails if {@code collapse} stops handling a
     * separator the table actually uses - the space in "HALO PESA", the hyphen
     * in "T-PESA", the ampersand in "I&amp;M". Reading the set back through the
     * same helper it was written with would prove nothing.
     */
    @Test
    public void everySenderIdInTheWhitelistTable_isTrusted() {
        String[] table = {
            "M-PESA", "MPESA",                      // 1  M-Pesa
            "VODACOM", "Vodacom",                   // 2  Vodacom Tanzania
            "MIXX", "MIXX BY YAS", "Mixx",          // 3  Mixx by Yas
            "YAS", "Yas",                           // 4  Yas Tanzania
            "AIRTEL MONEY", "AIRTELMONEY",          // 5  Airtel Money
            "AIRTEL", "Airtel",                     // 6  Airtel Tanzania
            "HALOPESA", "HALO PESA",                // 7  HaloPesa
            "HALOTEL", "Halotel",                   // 8  Halotel
            "TTCL",                                 // 9  TTCL
            "T-PESA", "TPESA",                      // 10 T-Pesa
            "NMB",                                  // 11 NMB Bank
            "CRDB",                                 // 12 CRDB Bank
            "NBC",                                  // 13 NBC Bank
            "ABSA",                                 // 14 Absa Bank Tanzania
            "STANBIC",                              // 15 Stanbic Bank Tanzania
            "STANCHART",                            // 16 Standard Chartered
            "EXIM",                                 // 17 Exim Bank Tanzania
            "DTB",                                  // 18 Diamond Trust Bank
            "EQUITY",                               // 19 Equity Bank Tanzania
            "KCB",                                  // 20 KCB Bank Tanzania
            "NCBA",                                 // 21 NCBA Bank Tanzania
            "IMBANK", "I&M",                        // 22 I&M Bank Tanzania
            "AMANA",                                // 23 Amana Bank
            "AZANIA",                               // 24 Azania Bank
            "UBA",                                  // 25 UBA Tanzania
            "ECOBANK",                              // 26 Ecobank Tanzania
            "BOA",                                  // 27 Bank of Africa Tanzania
            "PBZ",                                  // 28 People's Bank of Zanzibar
            "TCB",                                  // 29 Tanzania Commercial Bank
        };

        // Wording a real alert is entitled to use and the rules read worst.
        String body = "Ndugu mteja, salio lako ni Tsh 62,300. Thibitisha muamala "
                + "haraka leo hii, usimpe mtu yeyote PIN yako.";

        for (String sender : table) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertTrue(sender + " is in the whitelist table but was not trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
            assertEquals(sender + " should be safe", ScamDetector.SAFE,
                    result.classification);
        }
    }

    /**
     * The numeric codes the table names, which reach the gate through the
     * short-code rule rather than the registry. Pinned so that narrowing the
     * digit range cannot silently drop a bank's documented code.
     */
    @Test
    public void documentedBankShortCodes_areTrusted() {
        String body = "Thibitisha akaunti yako haraka, tuma PIN yako sasa hivi.";

        // 15166, 15566 - NMB; 15089 - CRDB.
        for (String sender : new String[]{"15166", "15566", "15089"}) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertTrue(sender + " is a documented bank short code but was not trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
        }
    }

    /**
     * The safety property the whole gate rests on. Borrowing a trusted name is
     * how the impersonation scams in the corpus address themselves, so a sender
     * is trusted only when it matches a registered ID exactly - never as a
     * prefix, a suffix or a substring.
     */
    @Test
    public void senderIdsBuiltOutOfATrustedName_areNotTrusted() {
        String body = "Laini yako itasimamishwa leo. Bofya bit.ly/nmb-update "
                + "kuthibitisha taarifa zako za benki.";

        // The short bank IDs the whitelist table adds - BOA, UBA, TCB, DTB - are
        // three letters, so the names built out of them are the easiest of all
        // to register. Exact matching is the only thing keeping them out.
        for (String sender : new String[]{"NMB-INFO", "AIRTEL-PR", "NMBB", "AIRTEL2",
                "YASPROMO", "CRDB SUPPORT", "MY-NMB", "NMB.SECURE",
                "BOA-TZ", "UBATZ", "TCB2", "DTB-ALERT", "EQUITY-INFO", "I&M2"}) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertFalse(sender + " must not be trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
            assertTrue(sender + " must still be scored and flagged",
                    result.shouldAlert());
        }
    }

    /**
     * An absent sender must never be trusted: the corpus tests score message
     * bodies with no originating address, and a gate that let those through
     * would silence the detector completely.
     */
    @Test
    public void absentSender_isNotTrusted() {
        String body = "Hongera umeshinda milioni 2. Lipa Tsh 5,000 kwa wakala "
                + "kabla ya kupokea zawadi yako.";

        // "-" is what AnalyzeFragment passes when the sender field is left
        // blank, and it collapses to nothing once separators are removed.
        for (String sender : new String[]{null, "", "   ", "-", ". . .", "_"}) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertFalse("sender [" + sender + "] must not be trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
            assertTrue("sender [" + sender + "] must still be flagged",
                    result.shouldAlert());
        }
    }

    /** A subscriber number is never a short code, however it is written. */
    @Test
    public void mobileNumbers_areNotMistakenForShortCodes() {
        String body = "Tuma PIN yako haraka sasa hivi kuthibitisha akaunti yako.";

        for (String sender : new String[]{"0712000111", "+255712000111", "255712000111",
                "+1555", "0712 000 111"}) {
            ScamDetector.ScamResult result = ScamDetector.detect(body, sender);

            assertFalse(sender + " must not be trusted",
                    result.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));
        }
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
    public void normalization_isStable() {
        assertEquals("tuma pesa sasa", ScamDetector.normalize("  TUMA\t  PESA  SASA  "));
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

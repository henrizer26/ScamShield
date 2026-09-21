package com.group2.scamshield;

import com.group2.scamshield.utils.ScamDetector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Measures the detector against a labelled corpus instead of asserting one
 * message at a time.
 *
 * <p>The corpus is a development set: the rules and these messages were written
 * together, so a perfect score here is a regression guard, not evidence of field
 * accuracy. Quoting it as field accuracy would be dishonest. What it does prove is
 * that a change to one rule cannot silently break the nineteen scam shapes and
 * twenty genuine shapes the app was built to separate - which is the property a
 * rule table needs, because every rule edit is global.
 *
 * <p>A twentieth scam shape sits in {@link #TRUSTED_SENDER_BLIND_SPOT} instead,
 * because the trusted-sender gate answers it before the rules see it. It is
 * listed rather than deleted so that what the gate costs is counted.
 *
 * <p>To report real accuracy, replace the corpus below with messages collected
 * from handsets and labelled by someone who did not write the rules.
 */
public class ScamDetectorCorpusTest {

    /** Messages the app must alert on: SUSPICIOUS or SCAM both count. */
    private static final String[][] SCAM_CORPUS = {
        {"0754321987", "Hongera! Umeshinda zawadi ya Tsh 500,000 kutoka promosheni ya Vodacom. Tuma Tsh 10,000 kwa namba 0754321987 kupokea zawadi yako."},
        {"+255713998877", "Ndugu mteja, akaunti yako ya M-Pesa imefungwa. Thibitisha PIN yako kupitia http://mpesa-verify.xyz haraka."},
        {"NMB-INFO", "Laini yako itasimamishwa leo. Bofya bit.ly/nmb-update kuthibitisha taarifa zako za benki."},
        {"0712345678", "Mkopo wa haraka bila dhamana! Piga 0712345678 upate hadi milioni 5 leo."},
        {"0765432198", "Mtaalam wa tiba ya asili. Kurudisha mke aliyekimbia, pete ya bahati, dawa ya mapenzi. Piga 0765432198."},
        {"0688111222", "Umechaguliwa kupata kazi ya mkataba bila usaili. Tuma ada ya usajili Tsh 20,000 kwa lipa namba 123456."},
        {"INFO", "Your account has been suspended. Verify your OTP at http://192.168.10.5/verify immediately."},
        {"0655443322", "Nitumie pesa haraka kwa mpesa namba 0655443322, nipo hospitali."},
        {"0699887766", "Tuma p.i.n yako ya M-Pesa ili tuweze kurejesha pesa zako zilizokwama."},
        {"PROMO", "W1nner! You have won a jackpot of USD 5,000. Send a processing fee to claim your prize now."},
        {"0744556677", "TCRA: Namba yako haijasajiliwa, itafungwa kesho. Sasisha taarifa kupitia www.tcra-update.top"},
        {"0622334455", "Salamu, hii ni benki yako. Tuma nenosiri lako la mtandaoni ili tuhakiki akaunti yako."},
        {"0788112233", "Kazi mpya nzuri bila usaili, tuma taarifa zako WhatsApp 0788112233"},
        {"AIRTEL-PR", "Hongera umeshinda droo ya Airtel. Lipa Tsh 5,000 kwa wakala kabla ya kupokea milioni 2."},
        {"0733221100", "URGENT! CLICK NOW! Visit www.free-money.tk to claim your prize before the deadline"},
        {"0614159000", "Habari ndugu, naomba tuongee kuhusu biashara."},
        {"0677889900", "Ingiza 0TP uliyotumiwa kwenye link hii http://voda-com.help ili kufungua akaunti yako."},
        {"0611223344", "Tuma tsh 30,000 kwa namba ya wakala 0611223344 sasa hivi, ni dharura."},
        {"0700111222", "Nina dawa ya kutajirika na hirizi ya kuvutia wateja. Wasiliana nami leo."},
    };

    /**
     * Scams the app deliberately does not flag, kept here rather than deleted so
     * the cost of the trusted-sender gate stays visible and counted.
     *
     * <p>This one addresses itself as "MIXX", a sender ID the networks reserve,
     * so the gate answers it safe before any rule runs. The trade is deliberate:
     * genuine operator and bank traffic is the wording the rules read worst -
     * an OTP carries a credential word, a deadline and an amount - and warning a
     * user about their own bank is the failure that makes people switch the app
     * off. Reaching this inbox at all requires sender-ID spoofing or an
     * aggregator that let the ID be registered, which is rarer than the false
     * alarms the gate removes, but it is not impossible.
     *
     * <p>Moving an entry into this list is therefore a product decision, not a
     * way to make a test pass. Anything added here must fail
     * {@link #trustedSenderGate_isTheOnlyThingSilencingThese()}, which checks
     * that the gate, and not weak wording, is what let it through.
     */
    private static final String[][] TRUSTED_SENDER_BLIND_SPOT = {
        {"MIXX", "Mteja wa YAS, akaunti yako itafungwa. Jibu na namba ya siri yako kuthibitisha."},
    };

    /** Everyday traffic the app must leave alone. */
    private static final String[][] GENUINE_CORPUS = {
        {"+255700000000", "Nimefika salama nyumbani. Tutaonana kesho."},
        {"M-PESA", "Umepokea Tsh 50,000 kutoka JOHN MAKALA. Muamala namba QR12345. Salio lako ni Tsh 62,300."},
        {"NMB", "Hatutakuomba PIN yako. Usimpe mtu yeyote namba ya siri ya akaunti yako."},
        {"+255715000111", "Mkutano wa kesho utaanza saa tatu asubuhi ukumbi wa Diamond."},
        {"+255754111222", "Habari mama, nimetuma pesa ya kodi ya nyumba jana usiku."},
        {"VODACOM", "Salio lako la data ni 2.5GB, litaisha tarehe 12/09. Piga *149*07# kuangalia."},
        {"TICKETS", "Asante kwa kununua tiketi. Namba ya muamala ni ABC123. Karibu tena."},
        {"+255762333444", "Naomba unipigie ukifika ofisini, tutaongea kuhusu ripoti ya mwezi."},
        {"CRDB", "Your OTP is 483920. Do not share it with anyone."},
        {"LUKU", "Ahsante kwa malipo yako ya LUKU. Namba ya muamala 998877. Uniti 45.6."},
        {"VODACOM", "Karibu kwenye huduma zetu. Kwa msaada piga 100 bure."},
        {"+255786222333", "Tafadhali lipa ada ya mtoto shule kabla ya tarehe kumi mwezi huu."},
        {"+255719444555", "Mwalimu amesema wanafunzi walete Tsh 5,000 kwa ajili ya mtihani."},
        {"TIGO", "Umepokea bonasi ya dakika 30 na SMS 100. Furahia huduma zetu."},
        {"+255688555666", "Nipigie ukiwa huru, nina habari njema kuhusu kazi tuliyoomba."},
        {"+255745666777", "Nitakutumia pesa kesho asubuhi nikifika benki, usiwe na wasiwasi."},
        {"NHIF", "Kadi yako ya bima itaisha muda tarehe 30. Tembelea ofisi zetu kwa maelezo."},
        {"+255693777888", "Nashukuru kwa msaada wako wa jana. Mungu akubariki sana."},
        {"CRDB", "Umelipa Tsh 120,000 kwa DAWASA. Kumbukumbu namba TX998877. Salio lako ni Tsh 340,000."},
        {"+255658888999", "Tutaonana kanisani jumapili saa nne kamili."},
    };

    @Test
    public void detectorSeparatesTheCorpus() {
        List<String> missed = new ArrayList<>();
        List<String> falseAlarms = new ArrayList<>();

        for (String[] row : SCAM_CORPUS) {
            ScamDetector.ScamResult result = ScamDetector.detect(row[1], row[0]);
            if (!result.shouldAlert()) {
                missed.add(result.score + " | " + row[1]);
            }
        }
        for (String[] row : GENUINE_CORPUS) {
            ScamDetector.ScamResult result = ScamDetector.detect(row[1], row[0]);
            if (result.shouldAlert()) {
                falseAlarms.add(result.score + " | " + row[1]);
            }
        }

        int truePositives = SCAM_CORPUS.length - missed.size();
        int falsePositives = falseAlarms.size();
        int falseNegatives = missed.size();
        double precision = truePositives / (double) (truePositives + falsePositives);
        double recall = truePositives / (double) (truePositives + falseNegatives);
        double f1 = 2 * precision * recall / (precision + recall);

        String report = String.format(
                "corpus=%d scam + %d genuine | precision=%.3f recall=%.3f f1=%.3f%n"
                        + "missed: %s%nfalse alarms: %s",
                SCAM_CORPUS.length, GENUINE_CORPUS.length, precision, recall, f1,
                missed, falseAlarms);
        System.out.println(report);

        assertTrue("recall dropped below 0.90\n" + report, recall >= 0.90);
        assertTrue("precision dropped below 0.90\n" + report, precision >= 0.90);
    }

    /**
     * Pins the exact shape of the blind spot the trusted-sender gate opens.
     *
     * <p>For every entry: the app stays silent, the gate is what silenced it,
     * and the identical message from an ordinary handset is still caught. The
     * last part is what keeps this list honest - without it, a scam the rules
     * simply miss could be filed here and look intentional.
     */
    @Test
    public void trustedSenderGate_isTheOnlyThingSilencingThese() {
        for (String[] row : TRUSTED_SENDER_BLIND_SPOT) {
            ScamDetector.ScamResult trusted = ScamDetector.detect(row[1], row[0]);

            assertTrue(row[0] + " is no longer silenced - move it back into "
                            + "SCAM_CORPUS rather than leaving it here",
                    !trusted.shouldAlert());
            assertTrue(row[0] + " was silenced by something other than the gate",
                    trusted.matchedRules.contains(ScamDetector.TRUSTED_SENDER_RULE));

            ScamDetector.ScamResult fromHandset = ScamDetector.detect(row[1], "0712000111");
            assertTrue("the rules miss \"" + row[1] + "\" on its wording alone, so "
                            + "the gate is not what is costing this detection",
                    fromHandset.shouldAlert());
        }
    }

    /**
     * The two thresholds are the only tuning knobs, so their effect has to stay
     * visible: every scam in the corpus must clear the warning threshold, and the
     * clearest ones must clear the scam threshold.
     */
    @Test
    public void thresholdsSeparateWarningsFromVerdicts() {
        int confident = 0;
        for (String[] row : SCAM_CORPUS) {
            ScamDetector.ScamResult result = ScamDetector.detect(row[1], row[0]);
            assertTrue(row[1] + " scored " + result.score,
                    result.score >= ScamDetector.SUSPICIOUS_THRESHOLD);
            if (result.score >= ScamDetector.SCAM_THRESHOLD) {
                confident++;
            }
        }
        assertTrue("only " + confident + " of " + SCAM_CORPUS.length
                + " scams reached the scam threshold", confident >= 15);
    }
}

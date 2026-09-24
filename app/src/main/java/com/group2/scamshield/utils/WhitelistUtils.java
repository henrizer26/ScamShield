package com.group2.scamshield.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Decides which incoming SMS are operator or bank traffic and skip the detector. */
public final class WhitelistUtils {

    /**
     * Sender IDs the mobile networks reserve for licensed operators, banks and
     * authorities. A subscriber's handset cannot originate traffic from one:
     * they are registered with TCRA through an aggregator, they cannot be dialled
     * back, and they carry no subscriber number. Traffic that arrives on one is
     * therefore the operator or the bank speaking, which is the one sender an
     * on-device rule table has no business second-guessing.
     *
     * <p>Written in collapsed form - lower case with separators removed - because
     * the same registered ID reaches a handset spelled several ways ("M-PESA",
     * "MPESA", "MIXX BY YAS"). {@link #collapse(String)} puts the incoming sender
     * into that same shape before it is looked up here.
     *
     * <p>The operator, mobile-money and bank entries follow the project's
     * "ScamShield Tanzania Incoming SMS Whitelist" table. That table calls its
     * sender IDs candidates to be confirmed against the originating address
     * Android actually reports, so an entry here is a claim about who may use
     * the name, not a confirmed observation. Removing one that turns out to be
     * wrong only costs a false alarm; the exact-match rule below is what stops a
     * wrong entry from costing anything worse.
     */
    private static final Set<String> REGISTERED_SENDER_IDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    // Mobile network operators.
                    "vodacom", "airtel", "yas", "halotel", "ttcl",
                    // Mobile money.
                    "mpesa", "airtelmoney", "mixx", "mixxbyyas", "halopesa",
                    "tpesa", "azampesa", "ezypesa", "selcom",
                    // Banks.
                    "nmb", "nmbbank", "crdb", "crdbbank", "nbc", "absa",
                    "stanbic", "stanchart", "exim", "dtb", "equity", "kcb",
                    "ncba", "imbank", "i&m", "amana", "azania", "uba",
                    "ecobank", "boa", "pbz", "tcb",
                    // Authorities.
                    "tcra", "nida",
                    // Not in the table, kept from the rule this gate replaced:
                    // Tigo is the name Yas traded under until the 2023 rebrand,
                    // and handsets still hold old messages under it.
                    "tigo", "tigopesa")));

    /**
     * Separators that registered sender IDs are spelled with. Only these are
     * removed before a lookup: letters and digits are left exactly as they
     * arrived, so a name built out of a trusted one - "AIRTEL-PR", "NMB-INFO",
     * "AIRTEL2" - collapses to something the set does not hold and goes on to be
     * scored like any other message. That exact-match requirement is the whole
     * safety property here, and the reason the set is never matched as a
     * substring: "borrow the brand, add a word" is precisely how the
     * impersonation scams in the test corpus address themselves.
     */
    private static final Pattern SEPARATORS = Pattern.compile("[\\s._-]");

    private static final Pattern SHORT_CODE = Pattern.compile("^[0-9]{3,5}$");

    private static final String SHORT_CODE_REASON =
            "Arrived on a short code, which only a licensed service can send from";

    private static final String SENDER_ID_REASON =
            "Arrived on a registered operator, bank or authority sender ID "
                    + "that subscribers cannot send from";

    // Wording typical of operator/bank service traffic.
    private static final String[] SERVICE_KEYWORDS = {
            "salio", "mpaka", "muda", "minute", "sms", "data",
            "mb", "gb", "bonus", "package", "bundle",
            "subsidy", "shukran", "asante", "karibu",
            "mmt", "maboresho", "server", "network"
    };

    private WhitelistUtils() {
    }

    /**
     * Why this sender is trusted, in the words shown on screen, or {@code null}
     * when it is not and the message has to be scored on its contents.
     *
     * <p>One method rather than a boolean plus a separate lookup, so that the
     * decision and the explanation given for it can never disagree.
     */
    public static String trustReason(String sender) {
        if (sender == null || sender.trim().isEmpty()) {
            return null;
        }
        if (isShortCode(sender)) {
            return SHORT_CODE_REASON;
        }
        // A sender made only of separators collapses to nothing, and the scan
        // screen supplies exactly that - "-" - when the user leaves the sender
        // blank. Guarded explicitly rather than relying on the set not holding
        // an empty string, because that would be a quiet way to trust everything.
        String collapsed = collapse(sender);
        if (!collapsed.isEmpty() && REGISTERED_SENDER_IDS.contains(collapsed)) {
            return SENDER_ID_REASON;
        }
        return null;
    }

    /**
     * True for a short code or a registered operator, bank or authority sender
     * ID. An unknown, empty or absent sender is never trusted: the corpus tests
     * score message bodies carrying no originating address at all, and trusting
     * those would silence the detector entirely.
     */
    public static boolean isTrustedSender(String sender) {
        return trustReason(sender) != null;
    }

    public static boolean isServiceMessage(String sender, String message) {
        if (isTrustedSender(sender)) {
            return true;
        }

        // Wording alone must never silence an alert: scam texts routinely borrow
        // service vocabulary ("asante", "karibu", "bonus", "sms"), and treating
        // that as proof of a service message hid real scams. Only sender IDs
        // that cannot belong to a person are eligible for the keyword check.
        if (!isServiceSenderId(sender)) {
            return false;
        }

        String msg = message == null ? "" : message.toLowerCase(Locale.ROOT);
        for (String keyword : SERVICE_KEYWORDS) {
            if (msg.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * True for a 3-5 digit short code such as TCRA's 15040. Operators, banks and
     * authorities send from these; a subscriber's phone cannot, because no
     * Tanzanian mobile number is that short.
     */
    private static boolean isShortCode(String sender) {
        if (sender == null || sender.isEmpty()) {
            return false;
        }
        // The "+" survives the strip so that a short international number cannot
        // pass itself off as a short code.
        String cleaned = sender.replaceAll("[^0-9+]", "");
        return SHORT_CODE.matcher(cleaned).matches();
    }

    /** Lower case with separators removed: "M-PESA" and "MIXX BY YAS" as stored. */
    private static String collapse(String sender) {
        return SEPARATORS.matcher(sender.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    /**
     * True for alphanumeric sender IDs such as "VODACOM" or "NMB". They cannot
     * be dialled back, so they identify a service rather than a subscriber.
     */
    private static boolean isServiceSenderId(String sender) {
        if (sender == null) {
            return false;
        }
        for (int i = 0; i < sender.length(); i++) {
            if (Character.isLetter(sender.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

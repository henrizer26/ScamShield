package com.group2.scamshield.utils;

import java.util.Locale;

/** Decides which incoming SMS are operator or bank traffic and skip the detector. */
public final class WhitelistUtils {

    // Wording typical of operator/bank service traffic.
    private static final String[] SERVICE_KEYWORDS = {
            "salio", "mpaka", "muda", "minute", "sms", "data",
            "mb", "gb", "bonus", "package", "bundle",
            "subsidy", "shukran", "asante", "karibu",
            "mmt", "maboresho", "server", "network"
    };

    private WhitelistUtils() {
    }

    public static boolean isServiceMessage(String sender, String message) {
        if (isShortCode(sender)) {
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
     * authorities send from these; a subscriber's phone cannot.
     */
    private static boolean isShortCode(String sender) {
        if (sender == null || sender.isEmpty()) {
            return false;
        }
        // Remove spaces and special characters
        String cleaned = sender.replaceAll("[^0-9+]", "");
        return cleaned.matches("^[0-9]{3,5}$");
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

package com.example.scamshield.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WhitelistUtils {

    // Known service provider numbers (NOT scams)
    private static final Set<String> WHITELIST_NUMBERS = new HashSet<>(Arrays.asList(
            // Vodacom
            "15001", "15002", "15003", "15004", "15005",
            "15010", "15011", "15012", "15013", "15014",
            "15100", "15101", "15102", "15103", "15104",

            // Tigo
            "15030", "15031", "15032", "15033", "15034",
            "15035", "15036", "15037", "15038", "15039",

            // Airtel
            "15060", "15061", "15062", "15063", "15064",
            "15065", "15066", "15067", "15068", "15069",

            // Halotel
            "15090", "15091", "15092", "15093", "15094",

            // TCRA
            "15040",

            // Other known service numbers
            "100", "101", "102", "103", "104", "105", "106",
            "111", "112", "113", "114", "115", "116", "117",
            "118", "119", "120", "121", "122", "123", "124",
            "125", "126", "127", "128", "129", "130",

            // Bank shortcodes
            "15300", "15301", "15302", "15303", "15304",
            "15305", "15306", "15307", "15308", "15309",
            "15310", "15311", "15312", "15313", "15314"
    ));

    // Known prefixes (e.g., Vodacom, Tigo, Airtel)
    private static final Set<String> WHITELIST_PREFIXES = new HashSet<>(Arrays.asList(
            "255", // Tanzania country code
            "+255"
    ));

    public static boolean isWhitelisted(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }

        // Remove spaces and special characters
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");

        // Check exact match
        if (WHITELIST_NUMBERS.contains(cleaned)) {
            return true;
        }

        // Check if it's a short code (3-5 digits)
        if (cleaned.matches("^[0-9]{3,5}$")) {
            return true; // Most short codes are service numbers
        }

        return false;
    }

    // Wording typical of operator/bank service traffic.
    private static final String[] SERVICE_KEYWORDS = {
            "salio", "mpaka", "muda", "minute", "sms", "data",
            "mb", "gb", "bonus", "package", "bundle",
            "subsidy", "shukran", "asante", "karibu",
            "mmt", "maboresho", "server", "network"
    };

    public static boolean isServiceMessage(String sender, String message) {
        // Check if sender is whitelisted
        if (isWhitelisted(sender)) {
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
package com.example.scamshield.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScamDetector {

    // All scam keywords
    private static final String[] SCAM_KEYWORDS = {
            // Original keywords
            "tuma", "shilingi", "droo", "zawadi", "pesa",
            "maganaga", "mwenye nyumba", "kodi", "mkopo",
            "benki", "akaunti", "namba", "tuzo",

            // New keywords
            "0614159000", "0623638092", "0784062546",
            "ABEL MASIMA", "abel masima",
            "tiba", "uzazi", "masomo", "pete", "mali",
            "kuludisha mke", "kuludisha mume",
            "asili", "kafala", "mtaalam",
            "AirtelMoney", "ela", "Nitumie",
            "malipo", "baada ya tiba"
    };

    // Phone numbers to detect
    private static final String[] SCAM_PHONES = {
            "0614159000", "0623638092", "0784062546"
    };

    public static ScamResult detect(String message, String sender) {
        boolean isScam = false;
        boolean knownScamPhone = false;
        List<String> indicators = new ArrayList<>();

        // Check for scam phone numbers
        for (String phone : SCAM_PHONES) {
            if (message.contains(phone) || (sender != null && sender.contains(phone))) {
                isScam = true;
                knownScamPhone = true;
                indicators.add("Suspicious phone number: " + phone);
                break;
            }
        }

        // Check for scam keywords
        if (!isScam) {
            for (String keyword : SCAM_KEYWORDS) {
                if (message.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                    isScam = true;
                    indicators.add("Contains keyword: " + keyword);
                }
            }
        }

        if (!isScam) {
            return new ScamResult(false, "Unknown", "Low", indicators);
        }

        // Determine scam type and danger level
        ScamResult classified = determineScamType(message, sender);
        String scamType = classified.scamType;
        String dangerLevel = classified.dangerLevel;
        indicators.addAll(classified.indicators);

        // A message from a known scam number is high risk regardless of its wording.
        if (knownScamPhone) {
            if (scamType.equals("General Scam")) {
                scamType = "Fake Phone Number Scam";
            }
            dangerLevel = "High Risk";
        }

        return new ScamResult(true, scamType, dangerLevel, indicators);
    }

    private static ScamResult determineScamType(String message, String sender) {
        String msg = message.toLowerCase(Locale.ROOT);
        String from = sender == null ? "" : sender.toLowerCase(Locale.ROOT);
        List<String> indicators = new ArrayList<>();
        String scamType = "General Scam";
        String dangerLevel = "Medium Risk";

        // Traditional Healer Scam
        if (msg.contains("tiba") || msg.contains("uzazi") || msg.contains("pete") ||
                msg.contains("mali") || msg.contains("asili") || msg.contains("kafala") ||
                msg.contains("mtaalam") || msg.contains("kuludisha")) {
            scamType = "Fake Traditional Healer Scam";
            dangerLevel = "High Risk";
            indicators.add("Fake healer/traditional promises");
            indicators.add("Requests for money");
        }

        // Money Request Scam
        if (msg.contains("ela") || msg.contains("nitumie") || msg.contains("airtelmoney") ||
                msg.contains("malipo") || msg.contains("tuma")) {
            scamType = "Money Request Scam";
            dangerLevel = "High Risk";
            indicators.add("Direct money request");
            indicators.add("Suspicious payment method (AirtelMoney)");
        }

        // Phone Number Scam
        if (containsScamPhone(msg) || containsScamPhone(from) || msg.contains("abel masima")) {
            scamType = "Fake Phone Number Scam";
            dangerLevel = "High Risk";
            indicators.add("Fake contact number");
            indicators.add("Known scam phone number");
        }

        // Prize Scam
        if (msg.contains("zawadi") || msg.contains("droo") || msg.contains("tuzo") ||
                msg.contains("shilingi") || msg.contains("pesa")) {
            if (scamType.equals("General Scam")) {
                scamType = "Prize Scam / Fake Promotion";
                dangerLevel = "High Risk";
                indicators.add("Prize/Money claim");
                indicators.add("Urgent request");
            }
        }

        // Bank/Loan Scam
        if (msg.contains("benki") || msg.contains("akaunti") || msg.contains("mkopo")) {
            if (scamType.equals("General Scam")) {
                scamType = "Bank/Loan Scam";
                dangerLevel = "High Risk";
                indicators.add("Fake bank/loan offer");
                indicators.add("Request for personal information");
            }
        }

        return new ScamResult(scamType, dangerLevel, indicators);
    }

    private static boolean containsScamPhone(String text) {
        for (String phone : SCAM_PHONES) {
            if (text.contains(phone)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Risk bucket ("High" / "Medium" / "Low") for a stored scam type. Only the
     * type is persisted, so the dashboard and the history rows both derive the
     * risk from it here rather than each guessing with their own keywords.
     */
    public static String riskLevelFor(String scamType) {
        if (scamType == null) {
            return "Low";
        }
        String type = scamType.toLowerCase(Locale.ROOT);
        if (type.contains("healer") || type.contains("money") || type.contains("phone number") ||
                type.contains("prize") || type.contains("bank") || type.contains("loan")) {
            return "High";
        }
        if (type.contains("general") || type.contains("scam")) {
            return "Medium";
        }
        return "Low";
    }

    // Result class
    public static class ScamResult {
        public boolean isScam;
        public String scamType;
        public String dangerLevel;
        public List<String> indicators;

        public ScamResult(boolean isScam, String scamType, String dangerLevel, List<String> indicators) {
            this.isScam = isScam;
            this.scamType = scamType;
            this.dangerLevel = dangerLevel;
            this.indicators = indicators;
        }

        public ScamResult(String scamType, String dangerLevel, List<String> indicators) {
            this.isScam = true;
            this.scamType = scamType;
            this.dangerLevel = dangerLevel;
            this.indicators = indicators;
        }
    }
}
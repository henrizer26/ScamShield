package com.group2.scamshield.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One explainable regular-expression rule used by the local detector.
 *
 * <p>A rule owns a compiled {@link Pattern}, the signed weight it contributes to
 * the risk score, the sentence shown to the user when it fires, and the prepared
 * view of the SMS the pattern is applied to. Patterns are compiled once while the
 * rule table is built, so scanning a message never compiles a regular expression.
 *
 * <p>Weights may be negative. A negative rule describes wording that only genuine
 * traffic uses, so it pulls the score back down instead of pushing it up.
 */
public final class DetectionRule {

    /** Which prepared view of the message a pattern is applied to. */
    public enum Field {
        /** Case-folded body with collapsed spacing: keeps links, digits and punctuation. */
        MESSAGE,
        /** Body with inserted separators removed and look-alike characters folded back. */
        CANONICAL,
        /** Originating address only: a phone number or an alphanumeric sender ID. */
        SENDER,
        /** The sender first, then the case-folded body. */
        SENDER_OR_MESSAGE,
        /** The untouched body, for patterns that read the original capitalisation. */
        RAW
    }

    /** The four prepared views of a single SMS that rules are matched against. */
    public static final class Input {
        public final String raw;
        public final String message;
        public final String canonical;
        public final String sender;

        public Input(String raw, String message, String canonical, String sender) {
            this.raw = raw == null ? "" : raw;
            this.message = message == null ? "" : message;
            this.canonical = canonical == null ? "" : canonical;
            this.sender = sender == null ? "" : sender;
        }
    }

    private static final int EVIDENCE_LIMIT = 44;

    private final String id;
    private final Pattern pattern;
    private final int weight;
    private final String explanation;
    private final String category;
    private final Field field;

    public DetectionRule(String id, String regex, int weight, String explanation,
                         String category, Field field) {
        this.id = id;
        // RAW rules read capitalisation, so they must not be folded to one case.
        this.pattern = Pattern.compile(regex, field == Field.RAW
                ? Pattern.UNICODE_CASE
                : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        this.weight = weight;
        this.explanation = explanation;
        this.category = category;
        this.field = field;
    }

    /**
     * Runs the pattern over the view this rule reads.
     *
     * @return the text that fired the rule, trimmed for display, or {@code null}
     *         when the rule does not fire.
     */
    public String match(Input input) {
        switch (field) {
            case RAW:
                return firstMatch(input.raw);
            case SENDER:
                return firstMatch(input.sender);
            case MESSAGE:
                return firstMatch(input.message);
            case SENDER_OR_MESSAGE:
                String senderHit = firstMatch(input.sender);
                return senderHit != null ? senderHit : firstMatch(input.message);
            case CANONICAL:
            default:
                return firstMatch(input.canonical);
        }
    }

    private String firstMatch(String value) {
        if (value.isEmpty()) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        String hit = matcher.group().trim();
        if (hit.length() > EVIDENCE_LIMIT) {
            hit = hit.substring(0, EVIDENCE_LIMIT - 1).trim() + "...";
        }
        return hit;
    }

    /** True for supporting signals that must not stack into a verdict on their own. */
    public boolean isWeak() {
        return weight > 0 && weight <= ScamDetector.WEAK_RULE_WEIGHT;
    }

    /** True for wording that only genuine traffic uses. */
    public boolean isSuppressor() {
        return weight < 0;
    }

    public String getId() {
        return id;
    }

    public int getWeight() {
        return weight;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getCategory() {
        return category;
    }

    public Field getField() {
        return field;
    }
}

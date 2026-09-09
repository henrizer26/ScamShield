package com.group2.scamshield.utils;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import com.group2.scamshield.R;
import java.util.Locale;

/**
 * Single mapping from a risk bucket to the colours and wording the screens use.
 *
 * <p>{@link ScamDetector} is the only thing that decides risk; everything that
 * paints it - dashboard, history rows, verdict screen, report flow - reads the
 * bucket back through here so the three screens can never drift apart.
 */
public final class RiskUi {

    public static final String HIGH = "High";
    public static final String MEDIUM = "Medium";
    public static final String LOW = "Low";

    private RiskUi() {
    }

    /**
     * Normalises anything risk-shaped to a bucket: the stored scam type, a
     * detector danger level ("High Risk") or a bucket that is already correct.
     */
    public static String bucket(String value) {
        if (value == null) {
            return LOW;
        }
        String v = value.toLowerCase(Locale.ROOT);
        if (v.startsWith("high")) {
            return HIGH;
        }
        if (v.startsWith("medium")) {
            return MEDIUM;
        }
        if (v.startsWith("low")) {
            return LOW;
        }
        // Not a level - treat it as a scam type and let the detector classify it.
        return ScamDetector.riskLevelFor(value);
    }

    @ColorRes
    public static int color(String risk) {
        switch (bucket(risk)) {
            case HIGH:
                return R.color.risk_high;
            case MEDIUM:
                return R.color.risk_medium;
            default:
                return R.color.risk_low;
        }
    }

    @ColorRes
    public static int background(String risk) {
        switch (bucket(risk)) {
            case HIGH:
                return R.color.risk_high_bg;
            case MEDIUM:
                return R.color.risk_medium_bg;
            default:
                return R.color.risk_low_bg;
        }
    }

    /** Sentence-case label, e.g. "High Risk". */
    @StringRes
    public static int label(String risk) {
        switch (bucket(risk)) {
            case HIGH:
                return R.string.risk_high;
            case MEDIUM:
                return R.string.risk_medium;
            default:
                return R.string.risk_low;
        }
    }

    /** Upper-case headline for the verdict screen, e.g. "HIGH RISK". */
    @StringRes
    public static int headline(String risk) {
        switch (bucket(risk)) {
            case HIGH:
                return R.string.result_high_risk;
            case MEDIUM:
                return R.string.result_medium_risk;
            default:
                return R.string.result_low_risk;
        }
    }
}

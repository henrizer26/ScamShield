package com.group2.scamshield.utils;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import com.group2.scamshield.R;
import java.util.Locale;

/**
 * Single mapping from a verdict to the colours and wording the screens use.
 *
 * <p>{@link ScamDetector} is the only thing that decides a verdict; everything
 * that paints it - dashboard, history rows, verdict screen, report flow - reads
 * the bucket back through here so the three screens can never drift apart.
 */
public final class RiskUi {

    public static final String SCAM = "Scam";
    public static final String SUSPICIOUS = "Suspicious";
    public static final String SAFE = "Safe";

    private RiskUi() {
    }

    /**
     * Normalises anything verdict-shaped to a bucket: the stored scam type, a
     * detector verdict ("Scam") or a bucket that is already correct. The old
     * "High/Medium/Low Risk" wording is still understood so that rows written
     * before the rename keep landing in the right bucket.
     */
    public static String bucket(String value) {
        if (value == null) {
            return SAFE;
        }
        String v = value.toLowerCase(Locale.ROOT);
        if (v.startsWith("scam") || v.startsWith("high")) {
            return SCAM;
        }
        if (v.startsWith("suspicious") || v.startsWith("medium")) {
            return SUSPICIOUS;
        }
        if (v.startsWith("safe") || v.startsWith("low")) {
            return SAFE;
        }
        // Not a verdict - treat it as a scam type and let the detector classify it.
        return ScamDetector.riskLevelFor(value);
    }

    @ColorRes
    public static int color(String risk) {
        switch (bucket(risk)) {
            case SCAM:
                return R.color.risk_high;
            case SUSPICIOUS:
                return R.color.risk_medium;
            default:
                return R.color.risk_low;
        }
    }

    @ColorRes
    public static int background(String risk) {
        switch (bucket(risk)) {
            case SCAM:
                return R.color.risk_high_bg;
            case SUSPICIOUS:
                return R.color.risk_medium_bg;
            default:
                return R.color.risk_low_bg;
        }
    }

    /** Sentence-case label, e.g. "Scam". */
    @StringRes
    public static int label(String risk) {
        switch (bucket(risk)) {
            case SCAM:
                return R.string.verdict_scam;
            case SUSPICIOUS:
                return R.string.verdict_suspicious;
            default:
                return R.string.verdict_safe;
        }
    }

    /** Upper-case headline for the verdict screen, e.g. "SCAM". */
    @StringRes
    public static int headline(String risk) {
        switch (bucket(risk)) {
            case SCAM:
                return R.string.result_class_scam;
            case SUSPICIOUS:
                return R.string.result_class_suspicious;
            default:
                return R.string.result_class_safe;
        }
    }
}

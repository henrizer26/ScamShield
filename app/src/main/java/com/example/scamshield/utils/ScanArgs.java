package com.example.scamshield.utils;

import android.os.Bundle;
import com.example.scamshield.models.ScamModel;
import java.util.ArrayList;

/**
 * Bundle keys shared by the scan and report screens.
 *
 * <p>The flow is verdict -> details -> report -> review -> sent, and every step
 * needs the same scam plus what the previous step chose, so the arguments are
 * built and read here instead of in five separate fragments.
 */
public final class ScanArgs {

    public static final String SCAM = "scam";
    public static final String INDICATORS = "indicators";
    public static final String DANGER = "danger";
    public static final String IS_SCAM = "isScam";
    public static final String CLASSIFICATION = "classification";
    public static final String SCORE = "score";
    public static final String CHANNEL = "channel";
    public static final String NOTES = "notes";

    private ScanArgs() {
    }

    public static Bundle of(ScamModel scam, String danger, ArrayList<String> indicators, boolean isScam) {
        return of(scam, danger, indicators,
                isScam ? ScamDetector.SCAM : ScamDetector.SAFE,
                isScam ? ScamDetector.SCAM_THRESHOLD : 0);
    }

    public static Bundle of(ScamModel scam, String danger, ArrayList<String> indicators,
                            String classification, int score) {
        Bundle args = new Bundle();
        args.putSerializable(SCAM, scam);
        args.putString(DANGER, danger);
        args.putStringArrayList(INDICATORS, indicators);
        args.putBoolean(IS_SCAM, !ScamDetector.SAFE.equals(classification));
        args.putString(CLASSIFICATION, classification);
        args.putInt(SCORE, score);
        return args;
    }

    /** Copies the scan arguments forward and adds the reporting choice. */
    public static Bundle withReport(Bundle source, String channel, String notes) {
        Bundle args = source == null ? new Bundle() : new Bundle(source);
        args.putString(CHANNEL, channel);
        args.putString(NOTES, notes);
        return args;
    }

    public static ScamModel scam(Bundle args) {
        if (args == null) {
            return null;
        }
        Object value = args.getSerializable(SCAM);
        return value instanceof ScamModel ? (ScamModel) value : null;
    }

    public static ArrayList<String> indicators(Bundle args) {
        ArrayList<String> list = args == null ? null : args.getStringArrayList(INDICATORS);
        return list == null ? new ArrayList<String>() : list;
    }
}

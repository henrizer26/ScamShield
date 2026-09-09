package com.group2.scamshield.utils;

import android.os.Bundle;
import com.group2.scamshield.models.ScamModel;
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

    /**
     * Arguments for a scam read back from the database.
     *
     * <p>Indicators are not stored: the table keeps what a message was, not why.
     * The detector has no learned state and no I/O, so running it again over the
     * saved message reproduces exactly the reasons the message was flagged for,
     * which keeps the reasons and the rule table from drifting apart the way a
     * stored column would the first time a rule is edited.
     *
     * <p>Re-running also supplies the real score and classification. Building
     * these arguments without the detector meant guessing both, and the guess
     * was the scam threshold for every stored row.
     */
    public static Bundle ofStored(ScamModel scam) {
        if (scam == null) {
            return of(null, null, new ArrayList<String>(), ScamDetector.SAFE, 0);
        }
        ScamDetector.ScamResult result =
                ScamDetector.detect(scam.getMessage(), scam.getPhoneNumber());
        return of(scam, result.dangerLevel, new ArrayList<>(result.indicators),
                result.classification, result.score);
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

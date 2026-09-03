package com.example.scamshield.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Small key-value state that is not worth a database row: the dark/light
 * choice, whether onboarding has run, and how many messages have been checked.
 *
 * <p>The locale choice is not kept here: AppCompat persists that itself via the
 * {@code autoStoreLocales} service declared in the manifest.
 */
public final class AppPrefs {

    public static final String PREFS_NAME = "ScamShieldPrefs";
    public static final String KEY_FIRST_LAUNCH = "firstLaunch";

    private static final String KEY_DARK_THEME = "darkTheme";
    private static final String KEY_SCANNED = "messagesScanned";

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The reference design is the light one, so that is the default. */
    public static boolean isDarkTheme(Context context) {
        return prefs(context).getBoolean(KEY_DARK_THEME, false);
    }

    public static void setDarkTheme(Context context, boolean dark) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, dark).apply();
        applyNightMode(dark);
    }

    /** Re-applies the stored choice. Call once on process start. */
    public static void applyStoredTheme(Context context) {
        applyNightMode(isDarkTheme(context));
    }

    /**
     * Messages checked so far. Scams live in the database, but the dashboard
     * also counts the ones that came back clean, and those are not stored.
     */
    public static int getScannedCount(Context context) {
        return prefs(context).getInt(KEY_SCANNED, 0);
    }

    public static void incrementScannedCount(Context context) {
        prefs(context).edit().putInt(KEY_SCANNED, getScannedCount(context) + 1).apply();
    }

    /** Clears the local counters and sends the user back through onboarding. */
    public static void reset(Context context) {
        prefs(context).edit()
                .putInt(KEY_SCANNED, 0)
                .putBoolean(KEY_FIRST_LAUNCH, true)
                .apply();
    }

    private static void applyNightMode(boolean dark) {
        // Recreates any started activity, so the whole UI reskins immediately.
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}

package com.example.scamshield.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Stores the user's dark/light choice and applies it.
 *
 * <p>The locale choice is not kept here: AppCompat persists that itself via the
 * {@code autoStoreLocales} service declared in the manifest.
 */
public final class AppPrefs {

    private static final String PREFS_NAME = "ScamShieldPrefs";
    private static final String KEY_DARK_THEME = "darkTheme";

    private AppPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** The app was designed dark, so that is the default until the user picks. */
    public static boolean isDarkTheme(Context context) {
        return prefs(context).getBoolean(KEY_DARK_THEME, true);
    }

    public static void setDarkTheme(Context context, boolean dark) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, dark).apply();
        applyNightMode(dark);
    }

    /** Re-applies the stored choice. Call once on process start. */
    public static void applyStoredTheme(Context context) {
        applyNightMode(isDarkTheme(context));
    }

    private static void applyNightMode(boolean dark) {
        // Recreates any started activity, so the whole UI reskins immediately.
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }
}

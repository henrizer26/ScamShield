package com.example.scamshield;

import android.app.Application;
import com.example.scamshield.utils.AppPrefs;

public class ScamShieldApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply the saved dark/light choice before the first activity inflates,
        // otherwise the app would flash the default theme on every cold start.
        AppPrefs.applyStoredTheme(this);
    }
}

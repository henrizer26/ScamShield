package com.example.scamshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;
    private static final String PREFS_NAME = "ScamShieldPrefs";
    private static final String KEY_FIRST_LAUNCH = "firstLaunch";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if first launch
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

                if (isFirstLaunch) {
                    // First time → Show Onboarding
                    Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                } else {
                    // Already launched → Go to Main
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        }, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        // Drop the pending navigation so it cannot run against a dead activity.
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
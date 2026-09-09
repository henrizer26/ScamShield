package com.group2.scamshield;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.group2.scamshield.utils.AppPrefs;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObjectAnimator progressAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        // The bar fills over the same span the splash is shown for.
        ProgressBar progress = findViewById(R.id.progressSplash);
        progressAnimator = ObjectAnimator.ofInt(progress, "progress", 8, 100);
        progressAnimator.setDuration(SPLASH_DELAY);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.start();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs =
                        getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
                boolean isFirstLaunch = prefs.getBoolean(AppPrefs.KEY_FIRST_LAUNCH, true);

                Intent intent = isFirstLaunch
                        ? new Intent(SplashActivity.this, OnboardingActivity.class)
                        : new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        // Drop the pending navigation so it cannot run against a dead activity.
        handler.removeCallbacksAndMessages(null);
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        super.onDestroy();
    }
}

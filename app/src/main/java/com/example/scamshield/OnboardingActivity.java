package com.example.scamshield;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ScamShieldPrefs";
    private static final String KEY_FIRST_LAUNCH = "firstLaunch";

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button btnSkip, btnNext;

    private String[] titles = {
            "Stay Safe from SMS Scams",
            "Real-Time Alerts",
            "Easy Reporting",
            "", // Analyze Message screen
            ""  // Dashboard screen
    };

    private String[] descriptions = {
            "ScamShield detects suspicious messages, alerts you about the danger and helps you report scams easily.",
            "Get instant alerts with danger level so you can act before it's too late.",
            "Report scams to TCRA (15040) and telecom operators with guided steps.",
            "",
            ""
    };

    private int[] images = {
            R.drawable.onboard1,  // Screen 1 artwork
            R.drawable.onboard2,  // Screen 2 artwork
            R.drawable.onboard3,  // Screen 3 artwork
            R.drawable.logo,      // Analyze screen (falls back to the logo)
            R.drawable.logo       // Dashboard screen (falls back to the logo)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_activity);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        OnboardingAdapter adapter = new OnboardingAdapter(titles, descriptions, images);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {}
        ).attach();

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                int totalItems = titles.length - 1;

                if (currentItem < totalItems) {
                    viewPager.setCurrentItem(currentItem + 1);
                } else {
                    finishOnboarding();
                }
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == titles.length - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_FIRST_LAUNCH, false);
        editor.apply();

        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
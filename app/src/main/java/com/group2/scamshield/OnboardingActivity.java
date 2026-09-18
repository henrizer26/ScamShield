package com.group2.scamshield;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.group2.scamshield.utils.AppPrefs;
import com.group2.scamshield.utils.ProtectionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/** The three intro pages from the reference: artwork, headline, description. */
public class OnboardingActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 101;

    private ViewPager2 viewPager;
    private MaterialButton btnNext;

    private int[] titles;
    private int[] accents;
    private int[] descriptions;
    private int[] images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_activity);

        // Ask while the intro explains why, so protection can start as soon as
        // the user finishes it. Not again after a rotation.
        if (savedInstanceState == null && !ProtectionManager.hasRequiredPermissions(this)) {
            ActivityCompat.requestPermissions(this,
                    ProtectionManager.requiredPermissions(), PERMISSION_REQUEST);
        }

        titles = new int[]{R.string.onb1_title, R.string.onb2_title, R.string.onb3_title};
        // Only the first page splits its headline onto a coloured second line.
        accents = new int[]{R.string.onb1_title_accent, 0, 0};
        descriptions = new int[]{R.string.onb1_desc, R.string.onb2_desc, R.string.onb3_desc};
        images = new int[]{R.drawable.onboard1, R.drawable.onboard2, R.drawable.onboard3};

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        MaterialButton btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);

        viewPager.setAdapter(new OnboardingAdapter(titles, accents, descriptions, images));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
        }).attach();

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = viewPager.getCurrentItem();
                if (current < titles.length - 1) {
                    viewPager.setCurrentItem(current + 1);
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
                btnNext.setText(position == titles.length - 1
                        ? R.string.action_get_started
                        : R.string.action_next);
            }
        });
    }

    private void finishOnboarding() {
        AppPrefs.setOnboardingDone(this);

        // Granted during the intro: start real-time protection straight away.
        if (ProtectionManager.hasRequiredPermissions(this)) {
            ProtectionManager.enable(this);
        }

        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
        finish();
    }
}

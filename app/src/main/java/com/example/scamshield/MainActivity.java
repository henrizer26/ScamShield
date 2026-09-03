package com.example.scamshield;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.scamshield.utils.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Hosts the four tabs and the floating scan button.
 *
 * <p>The bar is only part of Home, History and Profile: the scan and report
 * steps are full-screen pages with their own back arrow, as in the reference.
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private BottomNavigationView bottomNav;
    private FloatingActionButton fabScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);

        // Request permissions
        requestPermissions();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        final NavController navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_nav);
        fabScan = findViewById(R.id.fabScan);
        NavigationUI.setupWithNavController(bottomNav, navController);

        fabScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.analyzeFragment, null, tabNavOptions());
            }
        });

        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NonNull NavController controller,
                                                     @NonNull NavDestination destination,
                                                     Bundle arguments) {
                        setChromeVisible(isTabDestination(destination.getId()));
                    }
                });

        routeSharedText(navController, getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            routeSharedText(navHostFragment.getNavController(), intent);
        }
    }

    /**
     * "Share Message" on the Analyze screen is the system share sheet: an SMS
     * shared from Messages lands here and is carried to the scan screen.
     */
    private void routeSharedText(NavController navController, Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            return;
        }

        String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(shared)) {
            return;
        }

        Bundle args = new Bundle();
        args.putString(AnalyzeFragment.ARG_SHARED_TEXT, shared);
        navController.navigate(R.id.analyzeFragment, args, tabNavOptions());

        // Consumed: a configuration change must not re-open the same message.
        intent.removeExtra(Intent.EXTRA_TEXT);
    }

    /** True for the three destinations the reference draws the bar on. */
    private boolean isTabDestination(int destinationId) {
        return destinationId == R.id.homeFragment
                || destinationId == R.id.historyFragment
                || destinationId == R.id.profileFragment;
    }

    private void setChromeVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        bottomNav.setVisibility(visibility);
        fabScan.setVisibility(visibility);
    }

    /** Tab-style navigation: one entry per tab, never a growing back stack. */
    private NavOptions tabNavOptions() {
        return new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();
    }

    private void requestPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
            };
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                break;
            }
        }
    }
}

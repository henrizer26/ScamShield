package com.group2.scamshield;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.group2.scamshield.utils.Nav;
import com.group2.scamshield.utils.NotificationHelper;
import com.group2.scamshield.utils.ProtectionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Hosts the four tabs.
 *
 * <p>The bar is only part of Home, History and Profile: the scan and report
 * steps are full-screen pages with their own back arrow, as in the reference.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create notification channel
        NotificationHelper.createNotificationChannel(this);

        // Restore protection only when the user previously enabled it and the
        // required Android permissions are still granted.
        ProtectionManager.restore(this);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        final NavController navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                bottomNav.setVisibility(isTabDestination(destination.getId())
                        ? View.VISIBLE
                        : View.GONE));

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
     * An SMS shared from Messages through the system share sheet lands here and
     * is carried to the scan screen, already filled in.
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
        navController.navigate(R.id.analyzeFragment, args, Nav.tabs());

        // Consumed: a configuration change must not re-open the same message.
        intent.removeExtra(Intent.EXTRA_TEXT);
    }

    /** True for the three destinations the reference draws the bar on. */
    private boolean isTabDestination(int destinationId) {
        return destinationId == R.id.homeFragment
                || destinationId == R.id.historyFragment
                || destinationId == R.id.profileFragment;
    }
}

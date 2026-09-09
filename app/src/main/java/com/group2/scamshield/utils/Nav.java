package com.group2.scamshield.utils;

import androidx.navigation.NavOptions;
import com.group2.scamshield.R;

/** Shared navigation options so switching tabs never stacks duplicates. */
public final class Nav {

    private Nav() {
    }

    public static NavOptions tabs() {
        return new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build();
    }

    /**
     * Clears the report flow once the report has been sent.
     *
     * <p>The confirmation screen offers Done and View History rather than a back
     * arrow, because the report has already left the phone. Back has to agree
     * with that: without this, the system back button walks into the review
     * screen the report was just sent from, where Send would send it a second
     * time. Popping the flow inclusively leaves the confirmation sitting on the
     * screen the report was started from - the scan result or the scam details.
     */
    public static NavOptions afterReportSent() {
        return new NavOptions.Builder()
                .setPopUpTo(R.id.reportScamFragment, true)
                .build();
    }
}

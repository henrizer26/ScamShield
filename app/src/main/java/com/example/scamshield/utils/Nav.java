package com.example.scamshield.utils;

import androidx.navigation.NavOptions;
import com.example.scamshield.R;

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
}

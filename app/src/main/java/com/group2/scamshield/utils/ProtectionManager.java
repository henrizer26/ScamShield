package com.group2.scamshield.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.group2.scamshield.services.ProtectionService;

/** Single source of truth for permission-gated real-time protection. */
public final class ProtectionManager {

    private ProtectionManager() {
    }

    /** What to ask Android for before protection can be switched on. */
    public static String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        return new String[]{Manifest.permission.RECEIVE_SMS};
    }

    private static boolean hasSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasNotificationPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasRequiredPermissions(Context context) {
        return hasSmsPermission(context) && hasNotificationPermission(context);
    }

    public static boolean isEnabled(Context context) {
        return AppPrefs.isProtectionEnabled(context);
    }

    /** Active means the user enabled protection and Android still grants access. */
    public static boolean isActive(Context context) {
        return isEnabled(context) && hasRequiredPermissions(context);
    }

    public static void enable(Context context) {
        if (!hasRequiredPermissions(context)) {
            disable(context);
            return;
        }
        AppPrefs.setProtectionEnabled(context, true);
        ContextCompat.startForegroundService(context.getApplicationContext(),
                new Intent(context, ProtectionService.class));
    }

    public static void disable(Context context) {
        AppPrefs.setProtectionEnabled(context, false);
        context.getApplicationContext().stopService(
                new Intent(context, ProtectionService.class));
    }

    /** Restores a previously enabled service when the application is opened. */
    public static void restore(Context context) {
        if (isActive(context)) {
            ContextCompat.startForegroundService(context.getApplicationContext(),
                    new Intent(context, ProtectionService.class));
        } else if (isEnabled(context)) {
            disable(context);
        }
    }
}

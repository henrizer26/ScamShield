package com.group2.scamshield.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.app.NotificationCompat;
import com.group2.scamshield.MainActivity;
import com.group2.scamshield.R;

public final class NotificationHelper {

    private static final String ALERT_CHANNEL_ID = "scamshield_alerts";
    private static final String PROTECTION_CHANNEL_ID = "scamshield_protection";

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.notif_channel_desc));
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 200, 500});

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    public static void createProtectionChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                PROTECTION_CHANNEL_ID,
                context.getString(R.string.protection_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.protection_channel_desc));
        channel.setShowBadge(false);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    public static Notification buildProtectionNotification(Context context) {
        return new NotificationCompat.Builder(context, PROTECTION_CHANNEL_ID)
                .setSmallIcon(R.drawable.shield_24)
                .setContentTitle(context.getString(R.string.protection_notification_title))
                .setContentText(context.getString(R.string.protection_notification_text))
                .setContentIntent(openAppIntent(context))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    public static void showRiskAlert(Context context, String phoneNumber,
                                     ScamDetector.ScamResult result) {
        createNotificationChannel(context);
        String title = ScamDetector.SCAM.equals(result.classification)
                ? context.getString(R.string.notif_scam_title)
                : context.getString(R.string.notif_suspicious_title);
        String text = context.getString(R.string.notif_risk_text,
                phoneNumber, result.scamType, result.score);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(openAppIntent(context))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Opens this app's notification settings: the bell on the dashboard and the
     * Notifications row in Settings. Before Android 8 there is no per-app
     * notification screen, so the app's info page stands in for it.
     */
    public static void openSettings(Context context) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + context.getPackageName()));
        }
        context.startActivity(intent);
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

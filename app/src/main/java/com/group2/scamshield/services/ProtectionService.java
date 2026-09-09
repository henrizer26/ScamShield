package com.group2.scamshield.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import com.group2.scamshield.utils.NotificationHelper;
import com.group2.scamshield.utils.ProtectionManager;

/** Visible foreground service representing user-enabled SMS protection. */
public class ProtectionService extends Service {

    public static final int NOTIFICATION_ID = 4100;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createProtectionChannel(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID,
                    NotificationHelper.buildProtectionNotification(this),
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                            ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            : 0);
        } else {
            startForeground(NOTIFICATION_ID,
                    NotificationHelper.buildProtectionNotification(this));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ProtectionManager.isActive(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

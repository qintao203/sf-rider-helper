package com.sf.riderhelper;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class GrabForegroundService extends Service {
    private static final int NOTIF_ID = 9001;
    private NotificationHelper notifHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        notifHelper = new NotificationHelper(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = notifHelper.buildServiceNotification(
                "顺丰抢单助手", "后台服务运行中...");
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NOTIF_ID, n);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

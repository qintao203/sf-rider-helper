package com.sf.riderhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationHelper {
    private static final String CHAN_GRAB = "grab_alerts";
    private static final String CHAN_SERVICE = "foreground_service";
    private final Context ctx;
    private final NotificationManager nm;

    public NotificationHelper(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        nm = (NotificationManager) this.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) return;
        nm.createNotificationChannel(new NotificationChannel(CHAN_GRAB,
                "抢单提醒", NotificationManager.IMPORTANCE_HIGH));
        nm.createNotificationChannel(new NotificationChannel(CHAN_SERVICE,
                "后台服务", NotificationManager.IMPORTANCE_LOW));
    }

    public Notification buildGrabNotification(String title, String text) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(ctx, CHAN_GRAB)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
    }

    public Notification buildServiceNotification(String title, String text) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 1, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(ctx, CHAN_SERVICE)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    public void notifyGrab(String title, String text) {
        nm.notify(1001, buildGrabNotification(title, text));
    }
}

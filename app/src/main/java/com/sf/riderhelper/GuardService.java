package com.sf.riderhelper;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * 守护进程：与GrabForegroundService互相监控
 * 对方被杀时自动拉起
 */
public class GuardService extends Service {
    private static final String TAG = "GuardService";
    private static GuardService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "GuardService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 定时检查主服务是否存活
        monitorThread.start();
        return START_STICKY;
    }

    private Thread monitorThread = new Thread(() -> {
        int failCount = 0;
        while (true) {
            try { Thread.sleep(10000); } catch (InterruptedException e) { break; }

            GrabAccessibilityService grabService = GrabAccessibilityService.getInstance();
            if (grabService == null || !grabService.isActive()) {
                failCount++;
                if (failCount >= 3) {
                    // 尝试重启服务
                    Log.w(TAG, "Main service seems dead, restarting...");
                    try {
                        startService(new Intent(this, GrabForegroundService.class));
                    } catch (Exception ignored) {}
                    failCount = 0;
                }
            } else {
                failCount = 0;
            }
        }
    });

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        instance = null;
        // 如果被系统杀死，尝试重启
        try {
            startService(new Intent(this, GuardService.class));
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    public static GuardService getInstance() { return instance; }
}

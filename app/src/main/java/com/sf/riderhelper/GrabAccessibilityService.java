package com.sf.riderhelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.Random;

public class GrabAccessibilityService extends AccessibilityService {
    private static final String TAG = "GrabService";
    private static GrabAccessibilityService instance;
    private boolean active = false;
    private boolean paused = false;
    private Handler handler = new Handler();
    private ScreenInteractor interactor;
    private ConfigManager config;
    private NotificationHelper notifHelper;
    private Runnable grabLoop;
    private long lastGrabTime = 0;
    private int consecutiveFails = 0;
    private Random rng = new Random();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        interactor = new ScreenInteractor(this);
        config = ConfigManager.getInstance(this);
        notifHelper = new NotificationHelper(this);
        Log.d(TAG, "GrabAccessibilityService created");
    }

    @Override
    public void onDestroy() {
        instance = null;
        active = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public static GrabAccessibilityService getInstance() { return instance; }
    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; }

    public void startGrabLoop() {
        if (active) return;
        active = true;
        paused = false;
        consecutiveFails = 0;
        Log.d(TAG, "Grab loop started");

        grabLoop = new Runnable() {
            @Override
            public void run() {
                if (!active || paused) {
                    if (active) handler.postDelayed(this, 1000);
                    return;
                }
                try {
                    executeGrabCycle();
                } catch (Exception e) {
                    Log.e(TAG, "Grab cycle error", e);
                }
                int interval = Math.max(500, config.getScanInterval() + rng.nextInt(500));
                handler.postDelayed(this, interval);
            }
        };
        handler.post(grabLoop);

        // 发送通知
        try {
            startForegroundService(new Intent(this, GrabForegroundService.class));
        } catch (Exception e) {
            Log.e(TAG, "Foreground service start failed", e);
        }
    }

    public void stopGrabLoop() {
        active = false;
        paused = false;
        handler.removeCallbacks(grabLoop);
        try { stopForegroundService(new Intent(this, GrabForegroundService.class)); } catch (Exception ignored) {}
        Log.d(TAG, "Grab loop stopped");
    }

    private void executeGrabCycle() {
        String pageText = interactor.getPageText();
        if (pageText.isEmpty()) {
            consecutiveFails++;
            return;
        }

        // 抢单逻辑：查找可点击的"抢单"按钮
        AccessibilityNodeInfo grabBtn = interactor.findNodeContaining("抢单");
        if (grabBtn == null) grabBtn = interactor.findNodeContaining("接单");
        if (grabBtn == null) grabBtn = interactor.findNodeContaining("领取");

        if (grabBtn != null) {
            // 提取订单信息
            String orderInfo = extractOrderInfo(pageText);

            // 检查是否符合过滤条件
            if (shouldGrab(orderInfo)) {
                // 随机延迟模拟人类操作
                int delay = config.getGrabDelay() + rng.nextInt(200);
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}

                boolean ok = interactor.clickNode(grabBtn);
                if (ok) {
                    long now = System.currentTimeMillis();
                    lastGrabTime = now;
                    int grabbed = config.getStatGrabbed() + 1;
                    config.setStatGrabbed(grabbed);
                    consecutiveFails = 0;

                    String msg = "已抢单: " + orderInfo;
                    Log.d(TAG, msg);
                    if (config.isVibrateOnGrab()) {
                        try { vibrate(); } catch (Exception ignored) {}
                    }
                    if (config.isNotifyOnGrab()) {
                        notifHelper.notifyGrab("抢单成功", orderInfo);
                    }

                    // 冷却
                    int cool = config.getCooldownSeconds() * 1000;
                    paused = true;
                    handler.postDelayed(() -> { paused = false; }, cool);
                } else {
                    config.setStatFailed(config.getStatFailed() + 1);
                    consecutiveFails++;
                }
            }
        } else {
            // 没有可抢的订单
            consecutiveFails++;
            if (consecutiveFails > 20) {
                // 连续失败20次，短暂停
                paused = true;
                handler.postDelayed(() -> { paused = false; }, 10000);
            }
        }

        if (grabBtn != null) grabBtn.recycle();
    }

    private boolean shouldGrab(String orderInfo) {
        // 简化版：先不做复杂过滤，直接抢
        return true;
    }

    private String extractOrderInfo(String pageText) {
        if (pageText.length() > 50) return pageText.substring(0, 50) + "...";
        return pageText;
    }

    private void vibrate() {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(200);
        }
    }
}

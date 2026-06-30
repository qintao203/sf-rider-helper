package com.sf.riderhelper;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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
    private GrabFilterEngine filterEngine;
    private GrabStrategy strategy;
    private TextSpeaker textSpeaker;
    private OrderDatabase orderDb;
    private FloatingWindowManager floatWin;
    private Runnable grabLoop;
    private long lastGrabTime = 0;
    private int consecutiveFails = 0;
    private Random rng = new Random();

    // 最近一次过滤结果（供UI读取）
    private volatile GrabFilterEngine.FilterResult lastFilterResult;
    private volatile GrabStrategy.Tier lastTier = GrabStrategy.Tier.REJECT;
    private volatile OrderParser.ParsedOrder lastParsedOrder;

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
        filterEngine = new GrabFilterEngine(config);
        strategy = new GrabStrategy(config);
        textSpeaker = new TextSpeaker(this);
        textSpeaker.init();
        orderDb = OrderDatabase.getInstance(this);
        floatWin = new FloatingWindowManager(this);
        Log.d(TAG, "GrabAccessibilityService created with smart engine + DB + TTS + float");
    }

    @Override
    public void onDestroy() {
        instance = null;
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (textSpeaker != null) textSpeaker.shutdown();
        super.onDestroy();
    }

    public static GrabAccessibilityService getInstance() { return instance; }
    public boolean isActive() { return active; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean p) { paused = p; }

    public GrabFilterEngine.FilterResult getLastFilterResult() { return lastFilterResult; }
    public GrabStrategy.Tier getLastTier() { return lastTier; }
    public OrderParser.ParsedOrder getLastParsedOrder() { return lastParsedOrder; }
    public GrabStrategy getStrategy() { return strategy; }

    public void startGrabLoop() {
        if (active) return;
        active = true;
        paused = false;
        consecutiveFails = 0;
        Log.d(TAG, "Grab loop started with smart engine");

        grabLoop = new Runnable() {
            @Override
            public void run() {
                if (!active) return;
                if (paused) {
                    handler.postDelayed(this, 1000);
                    return;
                }
                try {
                    executeSmartGrabCycle();
                } catch (Exception e) {
                    Log.e(TAG, "Grab cycle error", e);
                }
                int interval = Math.max(800, config.getScanInterval() + rng.nextInt(800));

                // 省电模式：低电量时降低扫描频率
                if (config.isPowerSaving()) {
                    interval = Math.max(3000, interval * 2);
                }

                // 勿扰时段检查
                if (config.isDndEnabled() && isInDndPeriod()) {
                    paused = true;
                    handler.postDelayed(() -> { paused = false; }, 60000);
                    return;
                }

                handler.postDelayed(this, interval);
            }
        };
        handler.post(grabLoop);

        try {
            startForegroundService(new Intent(this, GrabForegroundService.class));
        } catch (Exception e) {
            Log.e(TAG, "Foreground start failed", e);
        }

        // 启动守护进程
        try {
            startService(new Intent(this, GuardService.class));
        } catch (Exception ignored) {}

        // 启动悬浮球（闭环联动悬浮窗）
        if (config.isFloatingBall()) {
            try {
                floatWin.show();
            } catch (Exception ignored) {}
        }
    }

    public void stopGrabLoop() {
        active = false;
        paused = false;
        handler.removeCallbacks(grabLoop);
        try { stopService(new Intent(this, GrabForegroundService.class)); } catch (Exception ignored) {}
        Log.d(TAG, "Grab loop stopped");
    }

    private void executeSmartGrabCycle() {
        // 1. 获取页面文本
        String pageText = interactor.getPageText();
        if (pageText.isEmpty()) {
            consecutiveFails++;
            return;
        }

        // 2. 智能解析订单
        OrderParser.ParsedOrder order = OrderParser.parse(pageText);
        lastParsedOrder = order;

        if (!order.isValid()) {
            consecutiveFails++;
            return;
        }

        // 3. 多维过滤评分
        GrabFilterEngine.FilterResult result = filterEngine.evaluate(order);
        lastFilterResult = result;

        // 4. 三级策略决策
        GrabStrategy.Tier tier = strategy.decideTier(result.score);
        lastTier = tier;

        if (tier == GrabStrategy.Tier.REJECT) {
            config.setStatSkipped(config.getStatSkipped() + 1);
            consecutiveFails = 0; // 主动过滤不算失败
            Log.d(TAG, "过滤跳过: " + result);
            return;
        }

        // 5. 查找抢单按钮
        AccessibilityNodeInfo grabBtn = findGrabButton();

        if (grabBtn == null) {
            consecutiveFails++;
            return;
        }

        // 6. 获取策略延迟后，通过Handler延迟执行点击（避免Thread.sleep阻塞主线程）
        final int delayMs = strategy.getDelay(tier);
        final AccessibilityNodeInfo btnToClick = grabBtn;
        final GrabStrategy.Tier finalTier = tier;
        final GrabFilterEngine.FilterResult finalResult = result;
        final OrderParser.ParsedOrder finalOrder = order;

        handler.postDelayed(() -> {
            // 7. 执行抢单
            boolean ok = interactor.clickNode(btnToClick);
            if (btnToClick != null) btnToClick.recycle();

            if (ok) {
                onGrabSuccess(finalTier, finalResult, finalOrder);
            } else {
                config.setStatFailed(config.getStatFailed() + 1);
                consecutiveFails++;
            }
        }, delayMs);
    }

    private void onGrabSuccess(GrabStrategy.Tier tier,
                                GrabFilterEngine.FilterResult result,
                                OrderParser.ParsedOrder order) {
        strategy.onGrab(tier);
        lastGrabTime = System.currentTimeMillis();
        int grabbed = config.getStatGrabbed() + 1;
        config.setStatGrabbed(grabbed);
        consecutiveFails = 0;

        String msg = result.getStrategyName() + "抢单: " + order;
        Log.d(TAG, msg);

        // 震动
        if (config.isVibrateOnGrab()) {
            try { vibrate(); } catch (Exception ignored) {}
        }

        // 通知
        if (config.isNotifyOnGrab()) {
            notifHelper.notifyGrab(
                "抢单成功 " + result.getStrategyName(),
                order.toString() + " | " + result.reason);
        }

        // 语音播报
        textSpeaker.setEnabled(config.isTtsEnabled());
        textSpeaker.speakGrab(order.price, order.direction, order.distance, result.getStrategyName());

        // 保存到数据库
        orderDb.insertOrder(lastGrabTime, order.price, order.distance,
                order.direction, order.storeName, "success",
                result.getStrategyName(), result.score, result.reason);

        // 按策略冷却
        int coolMs = strategy.getCooldown(tier);
        paused = true;
        handler.postDelayed(() -> { paused = false; }, coolMs);

        // 更新悬浮窗
        if (floatWin != null && floatWin.isShowing()) {
            int g = config.getStatGrabbed();
            int f = config.getStatFailed();
            float income = g * 12;
            floatWin.updateStatus(tier.label + " 已抢", 0xFF4CAF50);
            floatWin.updateOrderInfo(order.toString() + " " + result.reason);
            floatWin.updateStats(g, f, income);
        }
    }

    private AccessibilityNodeInfo findGrabButton() {
        AccessibilityNodeInfo btn = interactor.findNodeContaining("抢单");
        if (btn == null) btn = interactor.findNodeContaining("接单");
        if (btn == null) btn = interactor.findNodeContaining("领取");
        if (btn == null) btn = interactor.findNodeContaining("确认");
        return btn;
    }

    private void vibrate() {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            long[] pattern = {0, 100, 50, 100}; // 双震
            v.vibrate(pattern, -1);
        }
    }

    /** 检查当前是否在勿扰时段内 */
    private boolean isInDndPeriod() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int now = c.get(java.util.Calendar.HOUR_OF_DAY);
        int start = config.getDndStartHour();
        int end = config.getDndEndHour();
        if (start <= end) return now >= start && now < end;
        return now >= start || now < end; // 跨天（如 23:00-06:00）
    }
}

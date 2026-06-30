1|package com.sf.riderhelper;
2|
3|import android.accessibilityservice.AccessibilityService;
4|import android.content.Intent;
5|import android.os.Handler;
6|import android.util.Log;
7|import android.view.accessibility.AccessibilityEvent;
8|import android.view.accessibility.AccessibilityNodeInfo;
9|
10|import java.util.Random;
11|
12|public class GrabAccessibilityService extends AccessibilityService {
13|    private static final String TAG = "GrabService";
14|    private static GrabAccessibilityService instance;
15|    private boolean active = false;
16|    private boolean paused = false;
17|    private Handler handler = new Handler();
18|    private ScreenInteractor interactor;
19|    private ConfigManager config;
20|    private NotificationHelper notifHelper;
21|    private GrabFilterEngine filterEngine;
22|    private GrabStrategy strategy;
23|    private TextSpeaker textSpeaker;
24|    private OrderDatabase orderDb;
25|    private FloatingWindowManager floatWin;
26|    private Runnable grabLoop;
27|    private long lastGrabTime = 0;
28|    private int consecutiveFails = 0;
29|    private Random rng = new Random();
30|
31|    // 最近一次过滤结果（供UI读取）
32|    private volatile GrabFilterEngine.FilterResult lastFilterResult;
33|    private volatile GrabStrategy.Tier lastTier = GrabStrategy.Tier.REJECT;
34|    private volatile OrderParser.ParsedOrder lastParsedOrder;
35|
36|    @Override
37|    public void onAccessibilityEvent(AccessibilityEvent event) {}
38|
39|    @Override
40|    public void onInterrupt() {}
41|
42|    @Override
43|    public void onCreate() {
44|        super.onCreate();
45|        instance = this;
46|        interactor = new ScreenInteractor(this);
47|        config = ConfigManager.getInstance(this);
48|        notifHelper = new NotificationHelper(this);
49|        filterEngine = new GrabFilterEngine(config);
50|        strategy = new GrabStrategy(config);
51|        textSpeaker = new TextSpeaker(this);
52|        textSpeaker.init();
53|        orderDb = OrderDatabase.getInstance(this);
54|        floatWin = new FloatingWindowManager(this);
55|        Log.d(TAG, "GrabAccessibilityService created with smart engine + DB + TTS + float");
56|    }
57|
58|    @Override
59|    public void onDestroy() {
60|        instance = null;
61|        active = false;
62|        handler.removeCallbacksAndMessages(null);
63|        if (textSpeaker != null) textSpeaker.shutdown();
64|        super.onDestroy();
65|    }
66|
67|    public static GrabAccessibilityService getInstance() { return instance; }
68|    public boolean isActive() { return active; }
69|    public boolean isPaused() { return paused; }
70|    public void setPaused(boolean p) { paused = p; }
71|
72|    public GrabFilterEngine.FilterResult getLastFilterResult() { return lastFilterResult; }
73|    public GrabStrategy.Tier getLastTier() { return lastTier; }
74|    public OrderParser.ParsedOrder getLastParsedOrder() { return lastParsedOrder; }
75|    public GrabStrategy getStrategy() { return strategy; }
76|
77|    public void startGrabLoop() {
78|        if (active) return;
79|        active = true;
80|        paused = false;
81|        consecutiveFails = 0;
82|        Log.d(TAG, "Grab loop started with smart engine");
83|
84|        grabLoop = new Runnable() {
85|            @Override
86|            public void run() {
87|                if (!active) return;
88|                if (paused) {
89|                    handler.postDelayed(this, 1000);
90|                    return;
91|                }
92|                try {
93|                    executeSmartGrabCycle();
94|                } catch (Exception e) {
95|                    Log.e(TAG, "Grab cycle error", e);
96|                }
97|                int interval = Math.max(800, config.getScanInterval() + rng.nextInt(800));
98|
99|                // 省电模式：低电量时降低扫描频率
100|                if (config.isPowerSaving()) {
101|                    interval = Math.max(3000, interval * 2);
102|                }
103|
104|                // 勿扰时段检查
105|                if (config.isDndEnabled() && isInDndPeriod()) {
106|                    paused = true;
107|                    handler.postDelayed(() -> { paused = false; }, 60000);
108|                    return;
109|                }
110|
111|                handler.postDelayed(this, interval);
112|            }
113|        };
114|        handler.post(grabLoop);
115|        // 启动前台服务（捕获Android 14+限制异常）
116|        try {
117|            Intent fi = new Intent(this, GrabForegroundService.class);
118|            if (Build.VERSION.SDK_INT >= 26) {
119|                startForegroundService(fi);
120|            } else {
121|                startService(fi);
122|            }
123|        } catch (Exception e) {
124|            Log.e(TAG, "Foreground service start failed: " + e.getMessage());
125|            // Android 14+可能限制前台服务，降级为普通服务
126|            try { startService(new Intent(this, GrabForegroundService.class)); } catch (Exception ignored) {}
127|        }
128|
129|        // 启动守护进程（捕获IllegalStateException）
130|        try {
131|            if (Build.VERSION.SDK_INT < 31) {
132|                startService(new Intent(this, GuardService.class));
133|            } else {
134|                // Android 12+ 需要foreground service type
135|                try { startForegroundService(new Intent(this, GuardService.class)); } catch (Exception ignored) {}
136|            }
137|        } catch (Exception ignored) {}
138|
139|        // 启动悬浮球（如果启用）- 带权限检测
140|        if (config.isFloatingBall()) {
141|            try {
142|                if (Build.VERSION.SDK_INT >= 23) {
143|                    boolean canDraw = android.provider.Settings.canDrawOverlays(this);
144|                    if (canDraw) {
145|                        floatWin.show();
146|                    }
147|                } else {
148|                    floatWin.show();
149|                }
150|            } catch (Exception ignored) {}
151|        }
152|    }
153|
154|    public void stopGrabLoop() {
155|        active = false;
156|        paused = false;
157|        handler.removeCallbacks(grabLoop);
158|        try { stopService(new Intent(this, GrabForegroundService.class)); } catch (Exception ignored) {}
159|        Log.d(TAG, "Grab loop stopped");
160|    }
161|
162|    private void executeSmartGrabCycle() {
163|        // 1. 获取页面文本
164|        String pageText = interactor.getPageText();
165|        if (pageText.isEmpty()) {
166|            consecutiveFails++;
167|            return;
168|        }
169|
170|        // 2. 智能解析订单
171|        OrderParser.ParsedOrder order = OrderParser.parse(pageText);
172|        lastParsedOrder = order;
173|
174|        if (!order.isValid()) {
175|            consecutiveFails++;
176|            return;
177|        }
178|
179|        // 3. 多维过滤评分
180|        GrabFilterEngine.FilterResult result = filterEngine.evaluate(order);
181|        lastFilterResult = result;
182|
183|        // 4. 三级策略决策
184|        GrabStrategy.Tier tier = strategy.decideTier(result.score);
185|        lastTier = tier;
186|
187|        if (tier == GrabStrategy.Tier.REJECT) {
188|            config.setStatSkipped(config.getStatSkipped() + 1);
189|            consecutiveFails = 0; // 主动过滤不算失败
190|            Log.d(TAG, "过滤跳过: " + result);
191|            return;
192|        }
193|
194|        // 5. 查找抢单按钮
195|        AccessibilityNodeInfo grabBtn = findGrabButton();
196|
197|        if (grabBtn == null) {
198|            consecutiveFails++;
199|            return;
200|        }
201|
202|        // 6. 获取策略延迟后，通过Handler延迟执行点击（避免Thread.sleep阻塞主线程）
203|        final int delayMs = strategy.getDelay(tier);
204|        final AccessibilityNodeInfo btnToClick = grabBtn;
205|        final GrabStrategy.Tier finalTier = tier;
206|        final GrabFilterEngine.FilterResult finalResult = result;
207|        final OrderParser.ParsedOrder finalOrder = order;
208|
209|        handler.postDelayed(() -> {
210|            // 7. 执行抢单
211|            boolean ok = interactor.clickNode(btnToClick);
212|            if (btnToClick != null) btnToClick.recycle();
213|
214|            if (ok) {
215|                onGrabSuccess(finalTier, finalResult, finalOrder);
216|            } else {
217|                config.setStatFailed(config.getStatFailed() + 1);
218|                consecutiveFails++;
219|            }
220|        }, delayMs);
221|    }
222|
223|    private void onGrabSuccess(GrabStrategy.Tier tier,
224|                                GrabFilterEngine.FilterResult result,
225|                                OrderParser.ParsedOrder order) {
226|        strategy.onGrab(tier);
227|        lastGrabTime = System.currentTimeMillis();
228|        int grabbed = config.getStatGrabbed() + 1;
229|        config.setStatGrabbed(grabbed);
230|        consecutiveFails = 0;
231|
232|        String msg = result.getStrategyName() + "抢单: " + order;
233|        Log.d(TAG, msg);
234|
235|        // 震动
236|        if (config.isVibrateOnGrab()) {
237|            try { vibrate(); } catch (Exception ignored) {}
238|        }
239|
240|        // 通知
241|        if (config.isNotifyOnGrab()) {
242|            notifHelper.notifyGrab(
243|                "抢单成功 " + result.getStrategyName(),
244|                order.toString() + " | " + result.reason);
245|        }
246|
247|        // 语音播报
248|        textSpeaker.setEnabled(config.isTtsEnabled());
249|        textSpeaker.speakGrab(order.price, order.direction, order.distance, result.getStrategyName());
250|
251|        // 保存到数据库
252|        orderDb.insertOrder(lastGrabTime, order.price, order.distance,
253|                order.direction, order.storeName, "success",
254|                result.getStrategyName(), result.score, result.reason);
255|
256|        // 按策略冷却
257|        int coolMs = strategy.getCooldown(tier);
258|        paused = true;
259|        handler.postDelayed(() -> { paused = false; }, coolMs);
260|
261|        // 更新悬浮窗
262|        if (floatWin != null && floatWin.isShowing()) {
263|            int g = config.getStatGrabbed();
264|            int f = config.getStatFailed();
265|            float income = g * 12;
266|            floatWin.updateStatus(tier.label + " 已抢", 0xFF4CAF50);
267|            floatWin.updateOrderInfo(order.toString() + " " + result.reason);
268|            floatWin.updateStats(g, f, income);
269|        }
270|    }
271|
272|    private AccessibilityNodeInfo findGrabButton() {
273|        AccessibilityNodeInfo btn = interactor.findNodeContaining("抢单");
274|        if (btn == null) btn = interactor.findNodeContaining("接单");
275|        if (btn == null) btn = interactor.findNodeContaining("领取");
276|        if (btn == null) btn = interactor.findNodeContaining("确认");
277|        return btn;
278|    }
279|
280|    private void vibrate() {
281|        android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
282|        if (v != null && v.hasVibrator()) {
283|            long[] pattern = {0, 100, 50, 100}; // 双震
284|            v.vibrate(pattern, -1);
285|        }
286|    }
287|
288|    /** 检查当前是否在勿扰时段内 */
289|    private boolean isInDndPeriod() {
290|        java.util.Calendar c = java.util.Calendar.getInstance();
291|        int now = c.get(java.util.Calendar.HOUR_OF_DAY);
292|        int start = config.getDndStartHour();
293|        int end = config.getDndEndHour();
294|        if (start <= end) return now >= start && now < end;
295|        return now >= start || now < end; // 跨天（如 23:00-06:00）
296|    }
297|}
298|
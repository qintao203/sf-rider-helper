1|package com.sf.riderhelper;
2|
3|import android.app.Activity;
4|import android.content.Intent;
5|import android.graphics.drawable.GradientDrawable;
6|import android.os.Build;
7|import android.os.Bundle;
8|import android.os.Handler;
9|import android.os.SystemClock;
10|import android.provider.Settings;
11|import android.view.Gravity;
12|import android.view.MotionEvent;
13|import android.view.View;
14|import android.view.Window;
15|import android.view.WindowManager;
16|import android.widget.Button;
17|import android.widget.LinearLayout;
18|import android.widget.ScrollView;
19|import android.widget.TextView;
20|
21|import java.text.SimpleDateFormat;
22|import java.util.Date;
23|import java.util.List;
24|import java.util.Locale;
25|
26|/**
27| * 骑手工作台 v13 — 全自动刷新 + 真实抢单数据
28| */
29|public class MainActivity2 extends Activity {
30|
31|    private ConfigManager config;
32|    private OrderDatabase orderDb;
33|    private Handler handler = new Handler();
34|    private boolean isOnline = false;
35|    private long onlineStartTime = 0;
36|    private int currentPage = 2;
37|
38|    private LinearLayout contentArea;
39|    private View topBar;
40|    private Button onlineBtn;
41|    private TextView tvTodayIncome, tvOrderCount;
42|
43|    private static final String[] NAV_ICONS = {"📊", "📋", "⚡", "💰", "👤"};
44|    private static final String[] NAV_TEXTS = {"看板", "订单", "抢单", "收入", "我的"};
45|
46|    @Override
47|    protected void onCreate(Bundle b) {
48|        super.onCreate(b);
49|        makeFullScreen();
50|        config = ConfigManager.getInstance(this);
51|        orderDb = OrderDatabase.getInstance(this);
52|
53|        LinearLayout root = new LinearLayout(this);
54|        root.setOrientation(LinearLayout.VERTICAL);
55|        root.setBackgroundColor(ThemeEngine.BG_DARK);
56|        root.addView(topBar = createTopBar());
57|
58|        contentArea = new LinearLayout(this);
59|        contentArea.setOrientation(LinearLayout.VERTICAL);
60|        contentArea.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
61|        root.addView(contentArea);
62|
63|        root.addView(navBar = createNavBar());
64|        setContentView(root);
65|        switchPage(2);
66|
67|        // ========== 自动刷新：每2秒刷新所有页面数据 ==========
68|        handler.postDelayed(new Runnable() {
69|            @Override
70|            public void run() {
71|                if (!isFinishing()) {
72|                    refreshAllData();
73|                    handler.postDelayed(this, 2000);
74|                }
75|            }
76|        }, 2000);
77|    }
78|
79|    /** 自动刷新所有实时数据 */
80|    private void refreshAllData() {
81|        // 更新顶部栏
82|        int grabbed = config.getStatGrabbed();
83|        float income = grabbed * 12;
84|        tvTodayIncome.setText("¥" + String.format("%.0f", income));
85|        tvOrderCount.setText(grabbed + "单");
86|
87|        // 刷新当前页面
88|        switchPage(currentPage);
89|    }
90|
91|    // ==================== 顶部栏 ====================
92|
93|    private View createTopBar() {
94|        LinearLayout bar = new LinearLayout(this);
95|        bar.setOrientation(LinearLayout.HORIZONTAL);
96|        bar.setGravity(Gravity.CENTER_VERTICAL);
97|        bar.setPadding(dp(16), dp(10), dp(16), dp(8));
98|        bar.setBackgroundColor(0xFF0D0D1A);
99|
100|        onlineBtn = new Button(this);
101|        onlineBtn.setText("⚪ 离线");
102|        onlineBtn.setTextColor(ThemeEngine.TEXT_DISABLED);
103|        onlineBtn.setTextSize(11);
104|        onlineBtn.setTypeface(null, 1);
105|        onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));
106|        onlineBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
107|        onlineBtn.setOnClickListener(v -> toggleOnline());
108|        bar.addView(onlineBtn);
109|
110|        View sp1 = new View(this);
111|        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
112|        bar.addView(sp1);
113|
114|        LinearLayout iw = new LinearLayout(this);
115|        iw.setOrientation(LinearLayout.VERTICAL);
116|        iw.setGravity(Gravity.CENTER);
117|        tvTodayIncome = new TextView(this);
118|        tvTodayIncome.setText("¥0");
119|        tvTodayIncome.setTextColor(ThemeEngine.NEON_GOLD);
120|        tvTodayIncome.setTextSize(16);
121|        tvTodayIncome.setTypeface(null, 1);
122|        iw.addView(tvTodayIncome);
123|        TextView il = new TextView(this);
124|        il.setText("今日收入");
125|        il.setTextColor(ThemeEngine.TEXT_MUTED);
126|        il.setTextSize(9);
127|        iw.addView(il);
128|        bar.addView(iw);
129|
130|        LinearLayout ow = new LinearLayout(this);
131|        ow.setOrientation(LinearLayout.VERTICAL);
132|        ow.setGravity(Gravity.CENTER);
133|        ow.setPadding(dp(16), 0, 0, 0);
134|        tvOrderCount = new TextView(this);
135|        tvOrderCount.setText("0单");
136|        tvOrderCount.setTextColor(ThemeEngine.NEON_CYAN);
137|        tvOrderCount.setTextSize(16);
138|        tvOrderCount.setTypeface(null, 1);
139|        ow.addView(tvOrderCount);
140|        TextView ol = new TextView(this);
141|        ol.setText("完成单");
142|        ol.setTextColor(ThemeEngine.TEXT_MUTED);
143|        ol.setTextSize(9);
144|        ow.addView(ol);
145|        bar.addView(ow);
146|
147|        View sp2 = new View(this);
148|        sp2.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
149|        bar.addView(sp2);
150|
151|        Button nb = new Button(this);
152|        nb.setText("🔔");
153|        nb.setTextSize(16);
154|        nb.setBackground(null);
155|        nb.setOnClickListener(v -> switchPage(1));
156|        bar.addView(nb);
157|
158|        return bar;
159|    }
160|
161|    private void toggleOnline() {
162|        isOnline = !isOnline;
163|        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
164|        if (isOnline) {
165|            onlineBtn.setText("🟢 在线");
166|            onlineBtn.setTextColor(ThemeEngine.NEON_GREEN);
167|            onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF1B5E20, dp(14)));
168|            if (s != null && !s.isActive()) {
169|                if (isAccEnabled()) {
170|                    try {
171|                        s.startGrabLoop();
172|                    } catch (Exception e) {
173|                        toast("启动失败: " + e.getMessage());
174|                    }
175|                } else {
176|                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
177|                }
178|            }
179|        } else {
180|            onlineBtn.setText("⚪ 离线");
181|            onlineBtn.setTextColor(ThemeEngine.TEXT_DISABLED);
182|            onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));
183|            if (s != null && s.isActive()) {
184|                try { s.stopGrabLoop(); } catch (Exception ignored) {}
185|            }
186|        }
187|    }
188|
189|    private boolean isAccEnabled() {
190|        try {
191|            String a = getPackageName() + "/" + GrabAccessibilityService.class.getCanonicalName();
192|            String e = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
193|            return e != null && e.contains(a);
194|        } catch (Exception ex) { return false; }
195|    }
196|
197|    // ==================== 底部导航 ====================
198|
199|    private LinearLayout createNavBar() {
200|        LinearLayout bar = new LinearLayout(this);
201|        bar.setOrientation(LinearLayout.HORIZONTAL);
202|        bar.setBackgroundColor(0xFF0D0D1A);
203|        bar.setPadding(0, dp(4), 0, dp(8));
204|        if (Build.VERSION.SDK_INT >= 28) bar.setPadding(0, dp(4), 0, dp(12));
205|        for (int i = 0; i < 5; i++) {
206|            final int idx = i;
207|            LinearLayout tab = new LinearLayout(this);
208|            tab.setOrientation(LinearLayout.VERTICAL);
209|            tab.setGravity(Gravity.CENTER);
210|            tab.setLayoutParams(new LinearLayout.LayoutParams(0, dp(50), 1));
211|            tab.setClickable(true);
212|            TextView ic = new TextView(this);
213|            ic.setText(NAV_ICONS[i]);
214|            ic.setTextSize(18);
215|            tab.addView(ic);
216|            TextView tx = new TextView(this);
217|            tx.setText(NAV_TEXTS[i]);
218|            tx.setTextSize(9);
219|            tab.addView(tx);
220|            tab.setOnClickListener(v -> switchPage(idx));
221|            bar.addView(tab);
222|        }
223|        return bar;
224|    }
225|
226|    private void switchPage(int idx) {
227|        currentPage = idx;
228|        contentArea.removeAllViews();
229|        // 高亮
230|        for (int i = 0; i < 5; i++) {
231|            boolean a = i == idx;
232|            LinearLayout tab = (LinearLayout) navBar.getChildAt(i);
233|            tab.setAlpha(a ? 1.0f : 0.35f);
234|            if (tab.getChildAt(0) instanceof TextView)
235|                ((TextView) tab.getChildAt(0)).setTextColor(a ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
236|            if (tab.getChildAt(1) instanceof TextView)
237|                ((TextView) tab.getChildAt(1)).setTextColor(a ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
238|        }
239|        View page = null;
240|        switch (idx) {
241|            case 0: page = buildDashboard(); break;
242|            case 1: page = buildOrderList(); break;
243|            case 2: page = buildGrabPanel(); break;
244|            case 3: page = buildEarnings(); break;
245|            case 4: page = buildProfile(); break;
246|        }
247|        if (page != null) contentArea.addView(page);
248|    }
249|
250|    private LinearLayout navBar;
251|
252|    // ==================== 页面0: 看板（实时数据） ====================
253|
254|    private View buildDashboard() {
255|        ScrollView sv = new ScrollView(this);
256|        LinearLayout p = new LinearLayout(this);
257|        p.setOrientation(LinearLayout.VERTICAL);
258|        p.setPadding(dp(16), dp(12), dp(16), dp(24));
259|
260|        int grabbed = config.getStatGrabbed();
261|        int failed = config.getStatFailed();
262|        int skipped = config.getStatSkipped();
263|        float income = grabbed * 12;
264|        int total = grabbed + failed;
265|        float rate = total > 0 ? (grabbed * 100f / total) : 0;
266|
267|        p.addView(stitle("📊 今日实时"));
268|
269|        LinearLayout mr = new LinearLayout(this);
270|        mr.setOrientation(LinearLayout.HORIZONTAL);
271|        mr.addView(metric("成功", String.valueOf(grabbed), ThemeEngine.NEON_GREEN));
272|        mr.addView(metric("失败", String.valueOf(failed), ThemeEngine.NEON_ROSE));
273|        mr.addView(metric("过滤", String.valueOf(skipped), ThemeEngine.TEXT_DISABLED));
274|        mr.addView(metric("收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));
275|        p.addView(mr);
276|
277|        // 最新抢单信息（来自GrabAccessibilityService实时数据）
278|        GrabAccessibilityService gs = GrabAccessibilityService.getInstance();
279|        if (gs != null && gs.getLastFilterResult() != null) {
280|            p.addView(stitle("⚡ 最新抢单"));
281|            GrabFilterEngine.FilterResult fr = gs.getLastFilterResult();
282|            OrderParser.ParsedOrder lastOrder = gs.getLastParsedOrder();
283|            if (lastOrder != null) {
284|                LinearLayout card = new LinearLayout(this);
285|                card.setOrientation(LinearLayout.VERTICAL);
286|                card.setBackground(gcard());
287|                card.setPadding(dp(14), dp(10), dp(14), dp(10));
288|                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
289|                clp.setMargins(0, dp(4), 0, dp(4));
290|                card.setLayoutParams(clp);
291|
292|                TextView line1 = new TextView(this);
293|                line1.setText("¥" + String.format("%.1f", lastOrder.price)
294|                        + "  " + lastOrder.direction
295|                        + "  " + (lastOrder.distance > 0 ? lastOrder.distance + "km" : ""));
296|                line1.setTextColor(ThemeEngine.TEXT_PRIMARY);
297|                line1.setTextSize(15);
298|                line1.setTypeface(null, 1);
299|                card.addView(line1);
300|
301|                TextView line2 = new TextView(this);
302|                line2.setText(fr.getStrategyName() + " · " + fr.score + "分 · " + fr.reason);
303|                line2.setTextColor(strategyColor(fr.getStrategyName()));
304|                line2.setTextSize(11);
305|                card.addView(line2);
306|                p.addView(card);
307|            }
308|        } else {
309|            p.addView(stitle("⚡ 抢单状态"));
310|            p.addView(statusCard(isOnline ? "服务运行中，等待订单..." : "点击顶部[🟢在线]启动服务",
311|                    isOnline ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED));
312|        }
313|
314|        // 效率数据
315|        p.addView(stitle("📈 效率"));
316|        p.addView(drow("成功率", String.format("%.0f%%", rate), rate >= 80 ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ORANGE));
317|        p.addView(drow("策略模式", config.getStrategyMode(), ThemeEngine.NEON_PURPLE));
318|        p.addView(drow("扫描间隔", config.getScanInterval() + "ms", ThemeEngine.TEXT_SECONDARY));
319|        p.addView(drow("最低评分", config.getMinScore() + "分", ThemeEngine.TEXT_SECONDARY));
320|
321|        p.addView(stitle("⚡ 快捷"));
322|        LinearLayout qr = new LinearLayout(this);
323|        qr.setOrientation(LinearLayout.HORIZONTAL);
324|        qr.addView(qbtn("📋 订单", () -> switchPage(1)));
325|        qr.addView(qbtn("⚙️ 设置", () -> startActivity(new Intent(this, SettingsActivity.class))));
326|        qr.addView(qbtn("📊 历史", () -> startActivity(new Intent(this, StatsActivity.class))));
327|        p.addView(qr);
328|
329|        sv.addView(p);
330|        return sv;
331|    }
332|
333|    // ==================== 页面1: 订单列表（来自OrderDatabase真实数据） ====================
334|
335|    private View buildOrderList() {
336|        ScrollView sv = new ScrollView(this);
337|        LinearLayout p = new LinearLayout(this);
338|        p.setOrientation(LinearLayout.VERTICAL);
339|        p.setPadding(dp(12), dp(8), dp(12), dp(24));
340|
341|        List<OrderDatabase.OrderRecord> records = orderDb.getRecent(50);
342|        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
343|
344|        if (records.isEmpty()) {
345|            LinearLayout e = new LinearLayout(this);
346|            e.setOrientation(LinearLayout.VERTICAL);
347|            e.setGravity(Gravity.CENTER);
348|            e.setPadding(0, dp(60), 0, 0);
349|            e.addView(emptyT("📋"));
350|            e.addView(emptyT("暂无抢单记录"));
351|            e.addView(emptySub("上线后自动抢单并记录"));
352|            p.addView(e);
353|        } else {
354|            p.addView(stitle("📋 抢单记录 (共" + records.size() + "条)"));
355|            for (OrderDatabase.OrderRecord rec : records) {
356|                LinearLayout card = new LinearLayout(this);
357|                card.setOrientation(LinearLayout.HORIZONTAL);
358|                card.setGravity(Gravity.CENTER_VERTICAL);
359|                card.setBackground(gcard());
360|                card.setPadding(dp(12), dp(8), dp(12), dp(8));
361|                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
362|                clp.setMargins(0, dp(2), 0, dp(2));
363|                card.setLayoutParams(clp);
364|
365|                // 结果图标
366|                TextView res = new TextView(this);
367|                res.setText(rec.isSuccess() ? "✓" : "✗");
368|                res.setTextColor(rec.isSuccess() ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ROSE);
369|                res.setTextSize(16);
370|                res.setTypeface(null, 1);
371|                res.setLayoutParams(new LinearLayout.LayoutParams(dp(24), -2));
372|                card.addView(res);
373|
374|                // 信息
375|                LinearLayout info = new LinearLayout(this);
376|                info.setOrientation(LinearLayout.VERTICAL);
377|
378|                TextView line1 = new TextView(this);
379|                String dir = rec.direction != null && !rec.direction.isEmpty() ? rec.direction : "未知方向";
380|                line1.setText("¥" + String.format("%.1f", rec.price) + "  " + dir
381|                        + (rec.distance > 0 ? "  " + rec.distance + "km" : ""));
382|                line1.setTextColor(ThemeEngine.TEXT_PRIMARY);
383|                line1.setTextSize(14);
384|                line1.setTypeface(null, 1);
385|                info.addView(line1);
386|
387|                TextView line2 = new TextView(this);
388|                line2.setText(rec.strategy + " · " + rec.score + "分 · " + sdf.format(new Date(rec.timestamp))
389|                        + (rec.reason != null && !rec.reason.isEmpty() ? " · " + rec.reason : ""));
390|                line2.setTextColor(ThemeEngine.TEXT_MUTED);
391|                line2.setTextSize(10);
392|                info.addView(line2);
393|
394|                card.addView(info);
395|                p.addView(card);
396|            }
397|
398|            // 清除按钮
399|            if (!records.isEmpty()) {
400|                Button clear = new Button(this);
401|                clear.setText("🗑 清除全部");
402|                clear.setTextColor(ThemeEngine.NEON_ROSE);
403|                clear.setTextSize(12);
404|                clear.setBackground(gcard());
405|                clear.setPadding(dp(16), dp(6), dp(16), dp(6));
406|                clear.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
407|                ((LinearLayout.LayoutParams)clear.getLayoutParams()).setMargins(0, dp(8), 0, 0);
408|                clear.setOnClickListener(v -> { orderDb.clearAll(); config.resetStats(); refreshAllData(); });
409|                p.addView(clear);
410|            }
411|        }
412|
413|        sv.addView(p);
414|        return sv;
415|    }
416|
417|    // ==================== 页面2: 抢单面板（实时抢单数据） ====================
418|
419|    private View buildGrabPanel() {
420|        ScrollView sv = new ScrollView(this);
421|        LinearLayout p = new LinearLayout(this);
422|        p.setOrientation(LinearLayout.VERTICAL);
423|        p.setPadding(dp(16), dp(12), dp(16), dp(24));
424|
425|        int grabbed = config.getStatGrabbed();
426|        int failed = config.getStatFailed();
427|        int skipped = config.getStatSkipped();
428|        float income = grabbed * 12;
429|        GrabAccessibilityService gs = GrabAccessibilityService.getInstance();
430|        boolean running = gs != null && gs.isActive();
431|
432|        // 服务状态
433|        p.addView(stitle("⚡ 抢单服务"));
434|        p.addView(statusCard(
435|                running ? "● 运行中 · 策略: " + config.getStrategyMode() : "⚪ 已停止 · 点击顶部[🟢在线]启动",
436|                running ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED));
437|
438|        // 实时统计
439|        p.addView(stitle("📊 实时统计"));
440|        LinearLayout mr = new LinearLayout(this);
441|        mr.setOrientation(LinearLayout.HORIZONTAL);
442|        mr.addView(metric("成功", String.valueOf(grabbed), ThemeEngine.NEON_GREEN));
443|        mr.addView(metric("失败", String.valueOf(failed), ThemeEngine.NEON_ROSE));
444|        mr.addView(metric("过滤", String.valueOf(skipped), ThemeEngine.TEXT_DISABLED));
445|        mr.addView(metric("收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));
446|        p.addView(mr);
447|
448|        // 最新识别到的订单信息（自动识别距离金额）
449|        if (gs != null && gs.getLastParsedOrder() != null) {
450|            p.addView(stitle("🔍 最新识别订单"));
451|            OrderParser.ParsedOrder last = gs.getLastParsedOrder();
452|            GrabFilterEngine.FilterResult fr = gs.getLastFilterResult();
453|
454|            LinearLayout card = new LinearLayout(this);
455|            card.setOrientation(LinearLayout.VERTICAL);
456|            card.setBackground(gcard());
457|            card.setPadding(dp(14), dp(12), dp(14), dp(12));
458|            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
459|            clp.setMargins(0, dp(4), 0, dp(4));
460|            card.setLayoutParams(clp);
461|
462|            card.addView(detailRow("💰 金额", "¥" + String.format("%.1f", last.price), ThemeEngine.NEON_GOLD));
463|            if (last.distance > 0)
464|                card.addView(detailRow("📏 距离", last.distance + " km", ThemeEngine.NEON_CYAN));
465|            if (!last.direction.isEmpty())
466|                card.addView(detailRow("🧭 方向", last.direction, ThemeEngine.NEON_PURPLE));
467|            if (last.hasOverweightFee)
468|                card.addView(detailRow("⚖ 超重费", "含超重费", ThemeEngine.NEON_ORANGE));
469|            if (fr != null) {
470|                card.addView(detailRow("🎯 评分", fr.score + "分 · " + fr.getStrategyName(), strategyColor(fr.getStrategyName())));
471|                card.addView(detailRow("📝 原因", fr.reason, ThemeEngine.TEXT_SECONDARY));
472|            }
473|
474|            p.addView(card);
475|        } else if (!running) {
476|            p.addView(stitle("🔍 订单识别"));
477|            p.addView(statusCard("启动服务后自动识别金额/距离/方向", ThemeEngine.TEXT_DISABLED));
478|        }
479|
480|        // 启动/停止按钮
481|        Button btn = new Button(this);
482|        btn.setText(running ? "⏻ 停止服务" : "⚡ 启动服务");
483|        btn.setTextColor(running ? ThemeEngine.TEXT_SECONDARY : 0xFF0A0A14);
484|        btn.setTextSize(16);
485|        btn.setTypeface(null, 1);
486|        btn.setBackground(ThemeEngine.diagonalGradient(
487|                running ? new int[]{0xFF33334D, 0xFF252540} : new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35},
488|                ThemeEngine.RADIUS_XLARGE));
489|        btn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
490|        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(0, dp(12), 0, 0);
491|        btn.setOnClickListener(v -> {
492|            onlineBtn.performClick();
493|            refreshAllData();
494|        });
495|        p.addView(btn);
496|
497|        // 配置入口
498|        LinearLayout cr = new LinearLayout(this);
499|        cr.setOrientation(LinearLayout.HORIZONTAL);
500|        cr.setPadding(0, dp(8), 0, 0);
501|
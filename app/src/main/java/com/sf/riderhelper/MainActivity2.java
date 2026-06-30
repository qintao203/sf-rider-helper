package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 骑手工作台 v13 — 全自动刷新 + 真实抢单数据
 */
public class MainActivity2 extends Activity {

    private ConfigManager config;
    private OrderDatabase orderDb;
    private Handler handler = new Handler();
    private boolean isOnline = false;
    private long onlineStartTime = 0;
    private int currentPage = 2;

    private LinearLayout contentArea;
    private View topBar;
    private Button onlineBtn;
    private TextView tvTodayIncome, tvOrderCount;

    private static final String[] NAV_ICONS = {"📊", "📋", "⚡", "💰", "👤"};
    private static final String[] NAV_TEXTS = {"看板", "订单", "抢单", "收入", "我的"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);
        orderDb = OrderDatabase.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);
        root.addView(topBar = createTopBar());

        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(contentArea);

        root.addView(navBar = createNavBar());
        setContentView(root);
        switchPage(2);

        // ========== 自动刷新：每2秒刷新所有页面数据 ==========
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    refreshAllData();
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    /** 自动刷新所有实时数据 */
    private void refreshAllData() {
        // 更新顶部栏
        int grabbed = config.getStatGrabbed();
        float income = grabbed * 12;
        tvTodayIncome.setText("¥" + String.format("%.0f", income));
        tvOrderCount.setText(grabbed + "单");

        // 刷新当前页面
        switchPage(currentPage);
    }

    // ==================== 顶部栏 ====================

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(10), dp(16), dp(8));
        bar.setBackgroundColor(0xFF0D0D1A);

        onlineBtn = new Button(this);
        onlineBtn.setText("⚪ 离线");
        onlineBtn.setTextColor(ThemeEngine.TEXT_DISABLED);
        onlineBtn.setTextSize(11);
        onlineBtn.setTypeface(null, 1);
        onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));
        onlineBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        onlineBtn.setOnClickListener(v -> toggleOnline());
        bar.addView(onlineBtn);

        View sp1 = new View(this);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        bar.addView(sp1);

        LinearLayout iw = new LinearLayout(this);
        iw.setOrientation(LinearLayout.VERTICAL);
        iw.setGravity(Gravity.CENTER);
        tvTodayIncome = new TextView(this);
        tvTodayIncome.setText("¥0");
        tvTodayIncome.setTextColor(ThemeEngine.NEON_GOLD);
        tvTodayIncome.setTextSize(16);
        tvTodayIncome.setTypeface(null, 1);
        iw.addView(tvTodayIncome);
        TextView il = new TextView(this);
        il.setText("今日收入");
        il.setTextColor(ThemeEngine.TEXT_MUTED);
        il.setTextSize(9);
        iw.addView(il);
        bar.addView(iw);

        LinearLayout ow = new LinearLayout(this);
        ow.setOrientation(LinearLayout.VERTICAL);
        ow.setGravity(Gravity.CENTER);
        ow.setPadding(dp(16), 0, 0, 0);
        tvOrderCount = new TextView(this);
        tvOrderCount.setText("0单");
        tvOrderCount.setTextColor(ThemeEngine.NEON_CYAN);
        tvOrderCount.setTextSize(16);
        tvOrderCount.setTypeface(null, 1);
        ow.addView(tvOrderCount);
        TextView ol = new TextView(this);
        ol.setText("完成单");
        ol.setTextColor(ThemeEngine.TEXT_MUTED);
        ol.setTextSize(9);
        ow.addView(ol);
        bar.addView(ow);

        View sp2 = new View(this);
        sp2.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        bar.addView(sp2);

        Button nb = new Button(this);
        nb.setText("🔔");
        nb.setTextSize(16);
        nb.setBackground(null);
        nb.setOnClickListener(v -> switchPage(1));
        bar.addView(nb);

        return bar;
    }

    private void toggleOnline() {
        isOnline = !isOnline;
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (isOnline) {
            onlineBtn.setText("🟢 在线");
            onlineBtn.setTextColor(ThemeEngine.NEON_GREEN);
            onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF1B5E20, dp(14)));
            if (s != null && !s.isActive()) {
                if (isAccEnabled()) {
                    try {
                        s.startGrabLoop();
                    } catch (Exception e) {
                        toast("启动失败: " + e.getMessage());
                    }
                } else {
                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
                }
            }
        } else {
            onlineBtn.setText("⚪ 离线");
            onlineBtn.setTextColor(ThemeEngine.TEXT_DISABLED);
            onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));
            if (s != null && s.isActive()) {
                try { s.stopGrabLoop(); } catch (Exception ignored) {}
            }
        }
    }

    private boolean isAccEnabled() {
        try {
            String a = getPackageName() + "/" + GrabAccessibilityService.class.getCanonicalName();
            String e = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return e != null && e.contains(a);
        } catch (Exception ex) { return false; }
    }

    // ==================== 底部导航 ====================

    private LinearLayout createNavBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(0xFF0D0D1A);
        bar.setPadding(0, dp(4), 0, dp(8));
        if (Build.VERSION.SDK_INT >= 28) bar.setPadding(0, dp(4), 0, dp(12));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, dp(50), 1));
            tab.setClickable(true);
            TextView ic = new TextView(this);
            ic.setText(NAV_ICONS[i]);
            ic.setTextSize(18);
            tab.addView(ic);
            TextView tx = new TextView(this);
            tx.setText(NAV_TEXTS[i]);
            tx.setTextSize(9);
            tab.addView(tx);
            tab.setOnClickListener(v -> switchPage(idx));
            bar.addView(tab);
        }
        return bar;
    }

    private void switchPage(int idx) {
        currentPage = idx;
        contentArea.removeAllViews();
        // 高亮
        for (int i = 0; i < 5; i++) {
            boolean a = i == idx;
            LinearLayout tab = (LinearLayout) navBar.getChildAt(i);
            tab.setAlpha(a ? 1.0f : 0.35f);
            if (tab.getChildAt(0) instanceof TextView)
                ((TextView) tab.getChildAt(0)).setTextColor(a ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
            if (tab.getChildAt(1) instanceof TextView)
                ((TextView) tab.getChildAt(1)).setTextColor(a ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
        }
        View page = null;
        switch (idx) {
            case 0: page = buildDashboard(); break;
            case 1: page = buildOrderList(); break;
            case 2: page = buildGrabPanel(); break;
            case 3: page = buildEarnings(); break;
            case 4: page = buildProfile(); break;
        }
        if (page != null) contentArea.addView(page);
    }

    private LinearLayout navBar;

    // ==================== 页面0: 看板（实时数据） ====================

    private View buildDashboard() {
        ScrollView sv = new ScrollView(this);
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(12), dp(16), dp(24));

        int grabbed = config.getStatGrabbed();
        int failed = config.getStatFailed();
        int skipped = config.getStatSkipped();
        float income = grabbed * 12;
        int total = grabbed + failed;
        float rate = total > 0 ? (grabbed * 100f / total) : 0;

        p.addView(stitle("📊 今日实时"));

        LinearLayout mr = new LinearLayout(this);
        mr.setOrientation(LinearLayout.HORIZONTAL);
        mr.addView(metric("成功", String.valueOf(grabbed), ThemeEngine.NEON_GREEN));
        mr.addView(metric("失败", String.valueOf(failed), ThemeEngine.NEON_ROSE));
        mr.addView(metric("过滤", String.valueOf(skipped), ThemeEngine.TEXT_DISABLED));
        mr.addView(metric("收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));
        p.addView(mr);

        // 最新抢单信息（来自GrabAccessibilityService实时数据）
        GrabAccessibilityService gs = GrabAccessibilityService.getInstance();
        if (gs != null && gs.getLastFilterResult() != null) {
            p.addView(stitle("⚡ 最新抢单"));
            GrabFilterEngine.FilterResult fr = gs.getLastFilterResult();
            OrderParser.ParsedOrder lastOrder = gs.getLastParsedOrder();
            if (lastOrder != null) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(gcard());
                card.setPadding(dp(14), dp(10), dp(14), dp(10));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
                clp.setMargins(0, dp(4), 0, dp(4));
                card.setLayoutParams(clp);

                TextView line1 = new TextView(this);
                line1.setText("¥" + String.format("%.1f", lastOrder.price)
                        + "  " + lastOrder.direction
                        + "  " + (lastOrder.distance > 0 ? lastOrder.distance + "km" : ""));
                line1.setTextColor(ThemeEngine.TEXT_PRIMARY);
                line1.setTextSize(15);
                line1.setTypeface(null, 1);
                card.addView(line1);

                TextView line2 = new TextView(this);
                line2.setText(fr.getStrategyName() + " · " + fr.score + "分 · " + fr.reason);
                line2.setTextColor(strategyColor(fr.getStrategyName()));
                line2.setTextSize(11);
                card.addView(line2);
                p.addView(card);
            }
        } else {
            p.addView(stitle("⚡ 抢单状态"));
            p.addView(statusCard(isOnline ? "服务运行中，等待订单..." : "点击顶部[🟢在线]启动服务",
                    isOnline ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED));
        }

        // 效率数据
        p.addView(stitle("📈 效率"));
        p.addView(drow("成功率", String.format("%.0f%%", rate), rate >= 80 ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ORANGE));
        p.addView(drow("策略模式", config.getStrategyMode(), ThemeEngine.NEON_PURPLE));
        p.addView(drow("扫描间隔", config.getScanInterval() + "ms", ThemeEngine.TEXT_SECONDARY));
        p.addView(drow("最低评分", config.getMinScore() + "分", ThemeEngine.TEXT_SECONDARY));

        p.addView(stitle("⚡ 快捷"));
        LinearLayout qr = new LinearLayout(this);
        qr.setOrientation(LinearLayout.HORIZONTAL);
        qr.addView(qbtn("📋 订单", () -> switchPage(1)));
        qr.addView(qbtn("⚙️ 设置", () -> startActivity(new Intent(this, SettingsActivity.class))));
        qr.addView(qbtn("📊 历史", () -> startActivity(new Intent(this, StatsActivity.class))));
        p.addView(qr);

        sv.addView(p);
        return sv;
    }

    // ==================== 页面1: 订单列表（来自OrderDatabase真实数据） ====================

    private View buildOrderList() {
        ScrollView sv = new ScrollView(this);
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(12), dp(8), dp(12), dp(24));

        List<OrderDatabase.OrderRecord> records = orderDb.getRecent(50);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        if (records.isEmpty()) {
            LinearLayout e = new LinearLayout(this);
            e.setOrientation(LinearLayout.VERTICAL);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(60), 0, 0);
            e.addView(emptyT("📋"));
            e.addView(emptyT("暂无抢单记录"));
            e.addView(emptySub("上线后自动抢单并记录"));
            p.addView(e);
        } else {
            p.addView(stitle("📋 抢单记录 (共" + records.size() + "条)"));
            for (OrderDatabase.OrderRecord rec : records) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setBackground(gcard());
                card.setPadding(dp(12), dp(8), dp(12), dp(8));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
                clp.setMargins(0, dp(2), 0, dp(2));
                card.setLayoutParams(clp);

                // 结果图标
                TextView res = new TextView(this);
                res.setText(rec.isSuccess() ? "✓" : "✗");
                res.setTextColor(rec.isSuccess() ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ROSE);
                res.setTextSize(16);
                res.setTypeface(null, 1);
                res.setLayoutParams(new LinearLayout.LayoutParams(dp(24), -2));
                card.addView(res);

                // 信息
                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView line1 = new TextView(this);
                String dir = rec.direction != null && !rec.direction.isEmpty() ? rec.direction : "未知方向";
                line1.setText("¥" + String.format("%.1f", rec.price) + "  " + dir
                        + (rec.distance > 0 ? "  " + rec.distance + "km" : ""));
                line1.setTextColor(ThemeEngine.TEXT_PRIMARY);
                line1.setTextSize(14);
                line1.setTypeface(null, 1);
                info.addView(line1);

                TextView line2 = new TextView(this);
                line2.setText(rec.strategy + " · " + rec.score + "分 · " + sdf.format(new Date(rec.timestamp))
                        + (rec.reason != null && !rec.reason.isEmpty() ? " · " + rec.reason : ""));
                line2.setTextColor(ThemeEngine.TEXT_MUTED);
                line2.setTextSize(10);
                info.addView(line2);

                card.addView(info);
                p.addView(card);
            }

            // 清除按钮
            if (!records.isEmpty()) {
                Button clear = new Button(this);
                clear.setText("🗑 清除全部");
                clear.setTextColor(ThemeEngine.NEON_ROSE);
                clear.setTextSize(12);
                clear.setBackground(gcard());
                clear.setPadding(dp(16), dp(6), dp(16), dp(6));
                clear.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                ((LinearLayout.LayoutParams)clear.getLayoutParams()).setMargins(0, dp(8), 0, 0);
                clear.setOnClickListener(v -> { orderDb.clearAll(); config.resetStats(); refreshAllData(); });
                p.addView(clear);
            }
        }

        sv.addView(p);
        return sv;
    }

    // ==================== 页面2: 抢单面板（实时抢单数据） ====================

    private View buildGrabPanel() {
        ScrollView sv = new ScrollView(this);
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(12), dp(16), dp(24));

        int grabbed = config.getStatGrabbed();
        int failed = config.getStatFailed();
        int skipped = config.getStatSkipped();
        float income = grabbed * 12;
        GrabAccessibilityService gs = GrabAccessibilityService.getInstance();
        boolean running = gs != null && gs.isActive();

        // 服务状态
        p.addView(stitle("⚡ 抢单服务"));
        p.addView(statusCard(
                running ? "● 运行中 · 策略: " + config.getStrategyMode() : "⚪ 已停止 · 点击顶部[🟢在线]启动",
                running ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED));

        // 实时统计
        p.addView(stitle("📊 实时统计"));
        LinearLayout mr = new LinearLayout(this);
        mr.setOrientation(LinearLayout.HORIZONTAL);
        mr.addView(metric("成功", String.valueOf(grabbed), ThemeEngine.NEON_GREEN));
        mr.addView(metric("失败", String.valueOf(failed), ThemeEngine.NEON_ROSE));
        mr.addView(metric("过滤", String.valueOf(skipped), ThemeEngine.TEXT_DISABLED));
        mr.addView(metric("收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));
        p.addView(mr);

        // 最新识别到的订单信息（自动识别距离金额）
        if (gs != null && gs.getLastParsedOrder() != null) {
            p.addView(stitle("🔍 最新识别订单"));
            OrderParser.ParsedOrder last = gs.getLastParsedOrder();
            GrabFilterEngine.FilterResult fr = gs.getLastFilterResult();

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(gcard());
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
            clp.setMargins(0, dp(4), 0, dp(4));
            card.setLayoutParams(clp);

            card.addView(detailRow("💰 金额", "¥" + String.format("%.1f", last.price), ThemeEngine.NEON_GOLD));
            if (last.distance > 0)
                card.addView(detailRow("📏 距离", last.distance + " km", ThemeEngine.NEON_CYAN));
            if (!last.direction.isEmpty())
                card.addView(detailRow("🧭 方向", last.direction, ThemeEngine.NEON_PURPLE));
            if (last.hasOverweightFee)
                card.addView(detailRow("⚖ 超重费", "含超重费", ThemeEngine.NEON_ORANGE));
            if (fr != null) {
                card.addView(detailRow("🎯 评分", fr.score + "分 · " + fr.getStrategyName(), strategyColor(fr.getStrategyName())));
                card.addView(detailRow("📝 原因", fr.reason, ThemeEngine.TEXT_SECONDARY));
            }

            p.addView(card);
        } else if (!running) {
            p.addView(stitle("🔍 订单识别"));
            p.addView(statusCard("启动服务后自动识别金额/距离/方向", ThemeEngine.TEXT_DISABLED));
        }

        // 启动/停止按钮
        Button btn = new Button(this);
        btn.setText(running ? "⏻ 停止服务" : "⚡ 启动服务");
        btn.setTextColor(running ? ThemeEngine.TEXT_SECONDARY : 0xFF0A0A14);
        btn.setTextSize(16);
        btn.setTypeface(null, 1);
        btn.setBackground(ThemeEngine.diagonalGradient(
                running ? new int[]{0xFF33334D, 0xFF252540} : new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35},
                ThemeEngine.RADIUS_XLARGE));
        btn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(0, dp(12), 0, 0);
        btn.setOnClickListener(v -> {
            onlineBtn.performClick();
            refreshAllData();
        });
        p.addView(btn);

        // 配置入口
        LinearLayout cr = new LinearLayout(this);
        cr.setOrientation(LinearLayout.HORIZONTAL);
        cr.setPadding(0, dp(8), 0, 0);
        cr.addView(qbtn("⚙️ 抢单设置", () -> startActivity(new Intent(this, SettingsActivity.class))));
        cr.addView(qbtn("📊 订单历史", () -> startActivity(new Intent(this, StatsActivity.class))));
        p.addView(cr);

        sv.addView(p);
        return sv;
    }

    // ==================== 页面3: 收入（来自真实抢单数据） ====================

    private View buildEarnings() {
        ScrollView sv = new ScrollView(this);
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(12), dp(16), dp(24));

        int grabbed = config.getStatGrabbed();
        float todayIncome = grabbed * 12;
        OrderDatabase.Stats dbStats = orderDb.getStats();

        // 今日大数字
        LinearLayout bn = new LinearLayout(this);
        bn.setOrientation(LinearLayout.VERTICAL);
        bn.setGravity(Gravity.CENTER);
        bn.setPadding(0, dp(16), 0, dp(16));
        TextView vv = new TextView(this);
        vv.setText("¥" + String.format("%.0f", todayIncome));
        vv.setTextColor(ThemeEngine.NEON_GOLD);
        vv.setTextSize(44);
        vv.setTypeface(null, 1);
        bn.addView(vv);
        TextView ll = new TextView(this);
        ll.setText("今日抢单收入");
        ll.setTextColor(ThemeEngine.TEXT_DISABLED);
        ll.setTextSize(13);
        bn.addView(ll);
        p.addView(bn);

        // 周期
        p.addView(stitle("📆 收入详情"));
        p.addView(drow("已抢单", grabbed + "单", ThemeEngine.NEON_GREEN));
        p.addView(drow("总收入", "¥" + String.format("%.0f", todayIncome), ThemeEngine.NEON_GOLD));
        p.addView(drow("每单均价", "¥" + (grabbed > 0 ? String.format("%.1f", todayIncome / grabbed) : "0"), ThemeEngine.NEON_CYAN));

        // 数据库累加
        if (dbStats != null) {
            p.addView(stitle("🗄 历史累计"));
            p.addView(drow("累计成功", dbStats.totalGrabbed + "单", ThemeEngine.NEON_GREEN));
            p.addView(drow("累计收入", "¥" + String.format("%.0f", dbStats.totalIncome), ThemeEngine.NEON_GOLD));
        }

        // 提现
        Button wb = new Button(this);
        wb.setText("💳 提现 · 可提现¥" + String.format("%.0f", todayIncome));
        wb.setTextColor(ThemeEngine.NEON_CYAN);
        wb.setTextSize(14);
        wb.setTypeface(null, 1);
        wb.setBackground(gcard());
        wb.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(46)));
        ((LinearLayout.LayoutParams)wb.getLayoutParams()).setMargins(0, dp(12), 0, 0);
        wb.setOnClickListener(v ->
                toast("提现功能开发中\n当前可提现: ¥" + String.format("%.0f", todayIncome)));
        p.addView(wb);

        sv.addView(p);
        return sv;
    }

    // ==================== 页面4: 我的 ====================

    private View buildProfile() {
        ScrollView sv = new ScrollView(this);
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(12), dp(16), dp(24));

        // 资料卡
        LinearLayout pc = new LinearLayout(this);
        pc.setOrientation(LinearLayout.HORIZONTAL);
        pc.setGravity(Gravity.CENTER_VERTICAL);
        pc.setBackground(gcard());
        pc.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.setMargins(0, dp(4), 0, dp(8));
        pc.setLayoutParams(plp);

        TextView av = new TextView(this);
        av.setText("👤");
        av.setTextSize(36);
        av.setBackground(ThemeEngine.roundedBg(0x2200E5FF, dp(24)));
        av.setPadding(dp(10), dp(10), dp(10), dp(10));
        pc.addView(av);

        LinearLayout inf = new LinearLayout(this);
        inf.setOrientation(LinearLayout.VERTICAL);
        inf.setPadding(dp(12), 0, 0, 0);
        TextView nm = new TextView(this);
        nm.setText("骑手 · 顺丰同城");
        nm.setTextColor(ThemeEngine.TEXT_PRIMARY);
        nm.setTextSize(17);
        nm.setTypeface(null, 1);
        inf.addView(nm);
        TextView dt = new TextView(this);
        dt.setText("⚡ 已抢 " + config.getStatGrabbed() + "单 · 策略: " + config.getStrategyMode());
        dt.setTextColor(ThemeEngine.TEXT_SECONDARY);
        dt.setTextSize(12);
        inf.addView(dt);
        pc.addView(inf);
        p.addView(pc);

        // 设置
        p.addView(stitle("⚙️ 设置"));
        p.addView(menu("🎯 抢单配置", "金额/距离/方向/策略", () -> startActivity(new Intent(this, SettingsActivity.class))));
        p.addView(menu("🔔 提醒设置", "震动/通知/语音/悬浮球", () -> startActivity(new Intent(this, SettingsActivity.class))));
        p.addView(menu("📊 抢单历史", "查看所有记录", () -> startActivity(new Intent(this, StatsActivity.class))));

        // 顺丰同城
        p.addView(stitle("📦 顺丰同城"));
        boolean installed = SFRiderBridge.isInstalled(this);
        p.addView(menu(
                installed ? "📱 打开顺丰同城" : "📥 安装顺丰同城",
                installed ? "已安装 · 一键启动" : "前往应用商店",
                () -> { if (!SFRiderBridge.launchSFApp(this)) SFRiderBridge.openMarket(this); }));

        // 关于
        p.addView(stitle("ℹ️ 关于"));
        p.addView(menu("版本", "v13.0 · 自动刷新", null));
        p.addView(menu("帮助", "使用问题/建议", () -> toast("联系开发者: GitHub提交Issue")));

        sv.addView(p);
        return sv;
    }

    // ==================== UI工具 ====================

    private View metric(String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(gcard());
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(64), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(2), 0, dp(2), 0);
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(18);
        v.setTypeface(null, 1);
        card.addView(v);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(ThemeEngine.TEXT_DISABLED);
        l.setTextSize(10);
        card.addView(l);
        return card;
    }

    private View drow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(gcard());
        row.setPadding(dp(14), dp(8), dp(14), dp(8));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(rlp);
        row.addView(lbl(label));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(15);
        v.setTypeface(null, 1);
        row.addView(v);
        return row;
    }

    private View detailRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));
        row.addView(lbl(label));
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(14);
        v.setTypeface(null, 1);
        row.addView(v);
        return row;
    }

    private View menu(String label, String sub, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(gcard());
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(rlp);
        row.setClickable(onClick != null);
        if (onClick != null) row.setOnClickListener(v -> onClick.run());

        LinearLayout tw = new LinearLayout(this);
        tw.setOrientation(LinearLayout.VERTICAL);
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(ThemeEngine.TEXT_PRIMARY);
        l.setTextSize(14);
        tw.addView(l);
        if (sub != null && !sub.isEmpty()) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextColor(ThemeEngine.TEXT_MUTED);
            s.setTextSize(10);
            tw.addView(s);
        }
        row.addView(tw);
        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row.addView(sp);
        if (onClick != null) {
            TextView ar = new TextView(this);
            ar.setText("›");
            ar.setTextColor(ThemeEngine.TEXT_DISABLED);
            ar.setTextSize(20);
            row.addView(ar);
        }
        return row;
    }

    private View qbtn(String text, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(ThemeEngine.TEXT_SECONDARY);
        b.setTextSize(11);
        b.setBackground(gcard());
        b.setLayoutParams(new LinearLayout.LayoutParams(0, dp(36), 1));
        ((LinearLayout.LayoutParams)b.getLayoutParams()).setMargins(dp(2), 0, dp(2), 0);
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    private View statusCard(String text, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(gcard());
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(clp);
        View dot = new View(this);
        dot.setBackground(ThemeEngine.dot(color, 8, dot));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
        card.addView(dot);
        TextView tx = new TextView(this);
        tx.setText(text);
        tx.setTextColor(color);
        tx.setTextSize(13);
        tx.setTypeface(null, 1);
        tx.setPadding(dp(10), 0, 0, 0);
        card.addView(tx);
        return card;
    }

    private TextView stitle(String text) {
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.HORIZONTAL);
        w.setGravity(Gravity.CENTER_VERTICAL);
        w.setPadding(dp(4), dp(10), dp(4), dp(3));
        View d = new View(this);
        d.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_CYAN, dp(3)));
        d.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(10)));
        w.addView(d);
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(ThemeEngine.NEON_CYAN);
        t.setTextSize(12);
        t.setTypeface(null, 1);
        t.setPadding(dp(8), 0, 0, 0);
        w.addView(t);
        return t;
    }

    private GradientDrawable gcard() {
        return ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD);
    }

    private TextView lbl(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(ThemeEngine.TEXT_SECONDARY);
        t.setTextSize(13);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return t;
    }

    private int strategyColor(String s) {
        if (s == null) return ThemeEngine.TEXT_DISABLED;
        switch (s) {
            case "高优": return ThemeEngine.NEON_GREEN;
            case "中优": return ThemeEngine.NEON_CYAN;
            case "保底": return ThemeEngine.NEON_ORANGE;
            default: return ThemeEngine.TEXT_DISABLED;
        }
    }

    private TextView emptyT(String t) { TextView v = new TextView(this); v.setText(t); v.setTextSize(48); return v; }
    private TextView emptySub(String t) { TextView v = new TextView(this); v.setText(t); v.setTextColor(ThemeEngine.TEXT_MUTED); v.setTextSize(12); return v; }
    private void toast(String msg) { android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show(); }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            if (Build.VERSION.SDK_INT >= 28)
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } catch (Exception ignored) {}
        try { getWindow().setNavigationBarColor(0xFF0D0D1A); getWindow().setStatusBarColor(ThemeEngine.BG_DARK); } catch (Exception ignored) {}
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override
    protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}

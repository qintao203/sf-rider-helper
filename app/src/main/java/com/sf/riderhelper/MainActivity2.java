package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
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
 * 骑手工作台 - 主入口
 * 顶栏(在线/离线+今日收入) + 底部导航(今日看板/订单/抢单/收入/我的)
 * 抢单助手作为内置功能
 */
public class MainActivity2 extends Activity {

    private ConfigManager config;
    private RiderDataManager dataManager;
    private Handler handler = new Handler();
    private boolean isOnline = false;
    private long onlineStartTime = 0;

    // 底部导航
    private static final String[] NAV_LABELS = {"📊", "📋", "⚡", "💰", "👤"};
    private static final String[] NAV_TEXTS = {"看板", "订单", "抢单", "收入", "我的"};
    private LinearLayout contentArea;
    private LinearLayout navBar;
    private int currentPage = 2; // 默认抢单页
    private View topBar;
    private TextView tvTodayIncome, tvOrderCount, tvOnlineStatus;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);
        dataManager = RiderDataManager.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // ========== 顶部状态栏 ==========
        topBar = createTopBar();
        root.addView(topBar);

        // ========== 内容区 ==========
        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(contentArea);

        // ========== 底部导航 ==========
        navBar = createNavBar();
        root.addView(navBar);

        setContentView(root);
        switchPage(currentPage);

        // 定时刷新顶部数据
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    updateTopBar();
                    handler.postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    // ========== 顶部栏 ==========

    private View createTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(10), dp(16), dp(8));
        bar.setBackgroundColor(0xFF0D0D1A);

        // 在线/离线按钮
        final Button onlineBtn = new Button(this);
        onlineBtn.setText("⚪ 离线");
        onlineBtn.setTextColor(ThemeEngine.TEXT_DISABLED);
        onlineBtn.setTextSize(11);
        onlineBtn.setTypeface(null, 1);
        onlineBtn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));
        onlineBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
        onlineBtn.setTag("online_btn");
        onlineBtn.setOnClickListener(v -> toggleOnline(onlineBtn));
        bar.addView(onlineBtn);

        // 今日收入
        View sp1 = new View(this);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        bar.addView(sp1);

        LinearLayout incomeWrap = new LinearLayout(this);
        incomeWrap.setOrientation(LinearLayout.VERTICAL);
        incomeWrap.setGravity(Gravity.CENTER);

        tvTodayIncome = new TextView(this);
        tvTodayIncome.setText("¥0");
        tvTodayIncome.setTextColor(ThemeEngine.NEON_GOLD);
        tvTodayIncome.setTextSize(16);
        tvTodayIncome.setTypeface(null, 1);
        tvTodayIncome.setGravity(Gravity.CENTER);
        incomeWrap.addView(tvTodayIncome);

        TextView ilbl = new TextView(this);
        ilbl.setText("今日收入");
        ilbl.setTextColor(ThemeEngine.TEXT_MUTED);
        ilbl.setTextSize(9);
        ilbl.setGravity(Gravity.CENTER);
        incomeWrap.addView(ilbl);

        bar.addView(incomeWrap);

        // 订单数
        LinearLayout orderWrap = new LinearLayout(this);
        orderWrap.setOrientation(LinearLayout.VERTICAL);
        orderWrap.setGravity(Gravity.CENTER);
        orderWrap.setPadding(dp(16), 0, 0, 0);

        tvOrderCount = new TextView(this);
        tvOrderCount.setText("0单");
        tvOrderCount.setTextColor(ThemeEngine.NEON_CYAN);
        tvOrderCount.setTextSize(16);
        tvOrderCount.setTypeface(null, 1);
        tvOrderCount.setGravity(Gravity.CENTER);
        orderWrap.addView(tvOrderCount);

        TextView olbl = new TextView(this);
        olbl.setText("完成单");
        olbl.setTextColor(ThemeEngine.TEXT_MUTED);
        olbl.setTextSize(9);
        olbl.setGravity(Gravity.CENTER);
        orderWrap.addView(olbl);

        bar.addView(orderWrap);

        // 消息铃铛
        View sp2 = new View(this);
        sp2.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        bar.addView(sp2);

        Button notifBtn = new Button(this);
        notifBtn.setText("🔔");
        notifBtn.setTextSize(16);
        notifBtn.setBackground(null);
        notifBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        notifBtn.setOnClickListener(v -> {
            // 跳转到订单页并显示最新订单
            switchPage(1);
        });
        bar.addView(notifBtn);

        return bar;
    }

    private void toggleOnline(Button btn) {
        isOnline = !isOnline;
        if (isOnline) {
            onlineStartTime = SystemClock.elapsedRealtime();
            btn.setText("🟢 在线");
            btn.setTextColor(ThemeEngine.NEON_GREEN);
            btn.setBackground(ThemeEngine.roundedBg(0xFF1B5E20, dp(14)));

            // 启动抢单服务
            GrabAccessibilityService s = GrabAccessibilityService.getInstance();
            if (s != null && !s.isActive()) {
                if (isAccEnabled()) {
                    s.startGrabLoop();
                } else {
                    try {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } catch (Exception ignored) {}
                }
            }
        } else {
            btn.setText("⚪ 离线");
            btn.setTextColor(ThemeEngine.TEXT_DISABLED);
            btn.setBackground(ThemeEngine.roundedBg(0xFF33334D, dp(14)));

            GrabAccessibilityService s = GrabAccessibilityService.getInstance();
            if (s != null && s.isActive()) {
                s.stopGrabLoop();
            }
        }
        updateTopBar();
    }

    private boolean isAccEnabled() {
        try {
            String a = getPackageName() + "/" + GrabAccessibilityService.class.getCanonicalName();
            String e = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return e != null && e.contains(a);
        } catch (Exception ex) { return false; }
    }

    private void updateTopBar() {
        Object[] stats = dataManager.getStats();
        int completed = (int) stats[1];
        float income = (float) stats[3];
        tvTodayIncome.setText("¥" + String.format("%.0f", income));
        tvOrderCount.setText(completed + "单");
    }

    // ========== 底部导航 ==========

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

            TextView icon = new TextView(this);
            icon.setText(NAV_LABELS[i]);
            icon.setTextSize(18);
            icon.setTag("nav_icon_" + i);
            tab.addView(icon);

            TextView lbl = new TextView(this);
            lbl.setText(NAV_TEXTS[i]);
            lbl.setTextSize(9);
            lbl.setGravity(Gravity.CENTER);
            lbl.setTag("nav_text_" + i);
            tab.addView(lbl);

            tab.setOnClickListener(v -> switchPage(idx));
            bar.addView(tab);
        }
        return bar;
    }

    private void switchPage(int idx) {
        currentPage = idx;
        contentArea.removeAllViews();

        // 高亮当前页
        for (int i = 0; i < 5; i++) {
            boolean active = i == idx;
            LinearLayout tab = (LinearLayout) navBar.getChildAt(i);
            tab.setAlpha(active ? 1.0f : 0.35f);
            View iconV = tab.getChildAt(0);
            View textV = tab.getChildAt(1);
            if (iconV instanceof TextView) ((TextView) iconV).setTextColor(
                    active ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
            if (textV instanceof TextView) ((TextView) textV).setTextColor(
                    active ? ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
        }

        // 加载页面
        View page = null;
        switch (idx) {
            case 0: page = createDashboardPage(); break;
            case 1: page = createOrderListPage(); break;
            case 2: page = createGrabPage(); break;
            case 3: page = createEarningsPage(); break;
            case 4: page = createProfilePage(); break;
        }
        if (page != null) {
            contentArea.addView(page);
            contentArea.setAlpha(0.7f);
            contentArea.animate().alpha(1f).setDuration(200).start();
        }
    }

    // ========== 页面1: 今日看板 ==========

    private View createDashboardPage() {
        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(24));

        Object[] stats = dataManager.getStats();
        int total = (int) stats[0];
        int completed = (int) stats[1];
        int cancelled = (int) stats[2];
        float income = (float) stats[3];
        int active = dataManager.getOrdersByStatus(1).size()
                   + dataManager.getOrdersByStatus(2).size()
                   + dataManager.getOrdersByStatus(3).size();
        int available = dataManager.getOrdersByStatus(0).size();
        float rate = total > 0 ? (completed * 100f / total) : 0;

        page.addView(sectionTitle("📊 今日概览"));

        // 核心指标行
        LinearLayout metricRow = new LinearLayout(this);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        metricRow.addView(metricCard("待抢", String.valueOf(available), ThemeEngine.NEON_ROSE));
        metricRow.addView(metricCard("进行", String.valueOf(active), ThemeEngine.NEON_ORANGE));
        metricRow.addView(metricCard("完成", String.valueOf(completed), ThemeEngine.NEON_GREEN));
        metricRow.addView(metricCard("收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));
        page.addView(metricRow);

        page.addView(sectionTitle("📈 效率数据"));

        page.addView(dataRow("接单成功率", String.format("%.0f%%", rate), rate >= 80 ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ORANGE));
        page.addView(dataRow("在线时长", isOnline ? "在线中" : "已离线", isOnline ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED));
        page.addView(dataRow("取消单", String.valueOf(cancelled), cancelled > 0 ? ThemeEngine.NEON_ROSE : ThemeEngine.TEXT_DISABLED));
        page.addView(dataRow("总收入", "¥" + String.format("%.0f", income), ThemeEngine.NEON_GOLD));

        // 快捷操作
        page.addView(sectionTitle("⚡ 快捷操作"));
        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.addView(quickBtn("📋 查看订单", () -> switchPage(1)));
        quickRow.addView(quickBtn("⚡ 自动抢单", () -> switchPage(2)));
        quickRow.addView(quickBtn("⚙️ 设置", () -> startActivity(new Intent(this, SettingsActivity.class))));
        page.addView(quickRow);

        sv.addView(page);
        return sv;
    }

    // ========== 页面2: 订单列表 ==========

    private View createOrderListPage() {
        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(12), dp(8), dp(12), dp(24));

        List<RiderDataManager.OrderData> orders = dataManager.getOrders();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (orders.isEmpty()) {
            // 空状态
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(60), 0, 0);
            empty.addView(emptyIcon("📋"));
            empty.addView(emptyText("暂无订单"));
            empty.addView(emptySub("上线后自动接收订单"));
            page.addView(empty);
        } else {
            for (RiderDataManager.OrderData order : orders) {
                page.addView(orderCard(order, sdf));
            }
        }

        sv.addView(page);
        return sv;
    }

    // ========== 页面3: 抢单面板（真正集成） ==========

    private View createGrabPage() {
        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(24));

        // 抢单服务状态
        page.addView(sectionTitle("⚡ 抢单服务"));

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.setBackground(glassCard());
        statusCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.setMargins(0, dp(4), 0, dp(4));
        statusCard.setLayoutParams(slp);

        View dot = new View(this);
        dot.setBackground(ThemeEngine.dot(ThemeEngine.NEON_GREEN, 8, dot));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
        statusCard.addView(dot);

        TextView statusText = new TextView(this);
        statusText.setText("服务状态：" + (isOnline ? "运行中" : "已离线"));
        statusText.setTextColor(isOnline ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED);
        statusText.setTextSize(14);
        statusText.setTypeface(null, 1);
        statusText.setPadding(dp(10), 0, 0, 0);
        statusCard.addView(statusText);

        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        statusCard.addView(sp);

        // 策略标签
        TextView strategyTag = new TextView(this);
        strategyTag.setText(config.getStrategyMode());
        strategyTag.setTextColor(ThemeEngine.NEON_PURPLE);
        strategyTag.setTextSize(12);
        strategyTag.setTypeface(null, 1);
        strategyTag.setBackground(ThemeEngine.roundedBg(0x22BB86FC, dp(6)));
        strategyTag.setPadding(dp(8), dp(3), dp(8), dp(3));
        statusCard.addView(strategyTag);

        page.addView(statusCard);

        // 统计四列
        Object[] stats = dataManager.getStats();
        int g = config.getStatGrabbed();
        int f = config.getStatFailed();
        int sk = config.getStatSkipped();

        page.addView(sectionTitle("📊 抢单统计"));
        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.addView(statCard("成功", String.valueOf(g), ThemeEngine.NEON_GREEN));
        statRow.addView(statCard("失败", String.valueOf(f), ThemeEngine.NEON_ROSE));
        statRow.addView(statCard("过滤", String.valueOf(sk), ThemeEngine.TEXT_DISABLED));
        statRow.addView(statCard("收入", "¥" + (g * 12), ThemeEngine.NEON_GOLD));
        page.addView(statRow);

        // 操作按钮
        Button btnGrab = new Button(this);
        btnGrab.setText(isOnline ? "⏻ 停止服务" : "⚡ 启动服务");
        btnGrab.setTextColor(isOnline ? ThemeEngine.TEXT_SECONDARY : 0xFF0A0A14);
        btnGrab.setTextSize(16);
        btnGrab.setTypeface(null, 1);
        btnGrab.setBackground(ThemeEngine.diagonalGradient(
                isOnline ? new int[]{0xFF33334D, 0xFF252540} : new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35},
                ThemeEngine.RADIUS_XLARGE));
        btnGrab.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        ((LinearLayout.LayoutParams)btnGrab.getLayoutParams()).setMargins(0, dp(8), 0, 0);
        btnGrab.setElevation(dp(4));
        btnGrab.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN)
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL)
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            return false;
        });
        btnGrab.setOnClickListener(v -> {
            // 模拟顶部在线切换
            Button onlineBtn = topBar.findViewWithTag("online_btn");
            if (onlineBtn != null) onlineBtn.performClick();
        });
        page.addView(btnGrab);

        // 配置入口
        LinearLayout configRow = new LinearLayout(this);
        configRow.setOrientation(LinearLayout.HORIZONTAL);
        configRow.setPadding(0, dp(8), 0, 0);

        Button btnSettings = new Button(this);
        btnSettings.setText("⚙️ 抢单设置");
        btnSettings.setTextColor(ThemeEngine.TEXT_SECONDARY);
        btnSettings.setTextSize(13);
        btnSettings.setBackground(glassCard());
        btnSettings.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), 1));
        ((LinearLayout.LayoutParams)btnSettings.getLayoutParams()).setMargins(0, 0, dp(4), 0);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        configRow.addView(btnSettings);

        Button btnStats = new Button(this);
        btnStats.setText("📊 订单历史");
        btnStats.setTextColor(ThemeEngine.TEXT_SECONDARY);
        btnStats.setTextSize(13);
        btnStats.setBackground(glassCard());
        btnStats.setLayoutParams(new LinearLayout.LayoutParams(0, dp(40), 1));
        ((LinearLayout.LayoutParams)btnStats.getLayoutParams()).setMargins(dp(4), 0, 0, 0);
        btnStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        configRow.addView(btnStats);

        page.addView(configRow);

        sv.addView(page);
        return sv;
    }

    // ========== 页面4: 收入 ==========

    private View createEarningsPage() {
        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(24));

        RiderDataManager.RiderProfile profile = dataManager.getProfile();

        // 今日大数字
        LinearLayout bigNum = new LinearLayout(this);
        bigNum.setOrientation(LinearLayout.VERTICAL);
        bigNum.setGravity(Gravity.CENTER);
        bigNum.setPadding(0, dp(16), 0, dp(16));

        TextView val = new TextView(this);
        val.setText("¥" + String.format("%.0f", profile.todayIncome));
        val.setTextColor(ThemeEngine.NEON_GOLD);
        val.setTextSize(44);
        val.setTypeface(null, 1);
        bigNum.addView(val);

        TextView lbl = new TextView(this);
        lbl.setText("今日收入");
        lbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        lbl.setTextSize(13);
        lbl.setPadding(0, dp(4), 0, 0);
        bigNum.addView(lbl);

        page.addView(bigNum);

        // 周期收入
        page.addView(sectionTitle("📆 周期收入"));
        page.addView(dataRow("今日", "¥" + String.format("%.0f", profile.todayIncome), ThemeEngine.NEON_GOLD));
        page.addView(dataRow("本周", "¥" + String.format("%.0f", profile.weekIncome), ThemeEngine.NEON_CYAN));
        page.addView(dataRow("本月", "¥" + String.format("%.0f", profile.monthIncome), ThemeEngine.NEON_GREEN));

        // 钱包操作
        page.addView(sectionTitle("💰 钱包"));
        Button withdrawBtn = new Button(this);
        withdrawBtn.setText("💳 提现 · 可提现¥" + String.format("%.0f", profile.monthIncome));
        withdrawBtn.setTextColor(ThemeEngine.NEON_CYAN);
        withdrawBtn.setTextSize(14);
        withdrawBtn.setTypeface(null, 1);
        withdrawBtn.setBackground(glassCard());
        withdrawBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(46)));
        ((LinearLayout.LayoutParams)withdrawBtn.getLayoutParams()).setMargins(0, dp(4), 0, dp(4));
        withdrawBtn.setOnClickListener(v -> {
            android.widget.Toast.makeText(this,
                    "提现功能开发中\n当前可提现: ¥" + String.format("%.0f", profile.monthIncome),
                    android.widget.Toast.LENGTH_LONG).show();
        });
        page.addView(withdrawBtn);

        // 数据统计
        page.addView(sectionTitle("📊 数据统计"));
        Object[] stats = dataManager.getStats();
        int total = (int) stats[0];
        int completed = (int) stats[1];
        float rate = total > 0 ? (completed * 100f / total) : 0;
        page.addView(dataRow("总订单", String.valueOf(total), ThemeEngine.TEXT_PRIMARY));
        page.addView(dataRow("已完成", String.valueOf(completed), ThemeEngine.NEON_GREEN));
        page.addView(dataRow("成功率", String.format("%.0f%%", rate), rate >= 80 ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ORANGE));
        page.addView(dataRow("总里程", profile.totalDistance + " km", ThemeEngine.NEON_PURPLE));
        page.addView(dataRow("好评率", "4.9 ⭐", ThemeEngine.NEON_GOLD));

        sv.addView(page);
        return sv;
    }

    // ========== 页面5: 我的 ==========

    private View createProfilePage() {
        ScrollView sv = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(24));

        RiderDataManager.RiderProfile profile = dataManager.getProfile();

        // 个人资料卡
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.HORIZONTAL);
        profileCard.setGravity(Gravity.CENTER_VERTICAL);
        profileCard.setBackground(glassCard());
        profileCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.setMargins(0, dp(4), 0, dp(8));
        profileCard.setLayoutParams(plp);

        TextView avatar = new TextView(this);
        avatar.setText("👤");
        avatar.setTextSize(36);
        avatar.setBackground(ThemeEngine.roundedBg(0x2200E5FF, dp(24)));
        avatar.setPadding(dp(10), dp(10), dp(10), dp(10));
        profileCard.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);

        TextView name = new TextView(this);
        name.setText(profile.name + "  Lv." + profile.level);
        name.setTextColor(ThemeEngine.TEXT_PRIMARY);
        name.setTextSize(17);
        name.setTypeface(null, 1);
        info.addView(name);

        TextView detail = new TextView(this);
        detail.setText("⭐ " + profile.rating + " · " + profile.completedOrders + "单");
        detail.setTextColor(ThemeEngine.TEXT_SECONDARY);
        detail.setTextSize(12);
        info.addView(detail);

        profileCard.addView(info);
        page.addView(profileCard);

        // 设置列表
        page.addView(sectionTitle("⚙️ 设置"));
        page.addView(menuRow("🎯 抢单配置", "金额/距离/方向/策略", () ->
                startActivity(new Intent(this, SettingsActivity.class))));
        page.addView(menuRow("🔔 提醒设置", "震动/通知/语音/悬浮球", () ->
                startActivity(new Intent(this, SettingsActivity.class))));
        page.addView(menuRow("📊 数据统计", "历史订单/收入报表", () ->
                startActivity(new Intent(this, StatsActivity.class))));

        page.addView(sectionTitle("📦 顺丰同城"));
        boolean sfInstalled = SFRiderBridge.isInstalled(this);
        page.addView(menuRow(
                sfInstalled ? "📱 打开顺丰同城" : "📥 安装顺丰同城",
                sfInstalled ? "已安装 · 一键启动" : "前往应用商店安装",
                () -> {
                    if (!SFRiderBridge.launchSFApp(this)) {
                        SFRiderBridge.openMarket(this);
                    }
                }));

        page.addView(sectionTitle("ℹ️ 关于"));
        page.addView(menuRow("版本", "v11.0 · 29模块", null));
        page.addView(menuRow("帮助反馈", "使用问题/建议", () ->
                android.widget.Toast.makeText(this,
                        "联系开发者: 请在GitHub提交Issue", android.widget.Toast.LENGTH_SHORT).show()));

        sv.addView(page);
        return sv;
    }

    // ========== UI工具 ==========

    private View metricCard(String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(glassCard());
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(64), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);

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

    private View statCard(String label, String value, int color) {
        return metricCard(label, value, color);
    }

    private View dataRow(String label, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(glassCard());
        row.setPadding(dp(14), dp(9), dp(14), dp(9));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_SECONDARY);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(lbl);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(color);
        val.setTextSize(16);
        val.setTypeface(null, 1);
        row.addView(val);
        return row;
    }

    private View menuRow(String label, String sub, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(glassCard());
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(2), 0, dp(2));
        row.setLayoutParams(rlp);
        row.setClickable(onClick != null);
        if (onClick != null) row.setOnClickListener(v -> onClick.run());

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_PRIMARY);
        lbl.setTextSize(14);
        textWrap.addView(lbl);

        if (sub != null && !sub.isEmpty()) {
            TextView subLbl = new TextView(this);
            subLbl.setText(sub);
            subLbl.setTextColor(ThemeEngine.TEXT_MUTED);
            subLbl.setTextSize(10);
            textWrap.addView(subLbl);
        }

        row.addView(textWrap);

        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row.addView(sp);

        if (onClick != null) {
            TextView arrow = new TextView(this);
            arrow.setText("›");
            arrow.setTextColor(ThemeEngine.TEXT_DISABLED);
            arrow.setTextSize(20);
            row.addView(arrow);
        }

        return row;
    }

    private View quickBtn(String text, Runnable onClick) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(ThemeEngine.TEXT_SECONDARY);
        btn.setTextSize(11);
        btn.setBackground(glassCard());
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(36), 1));
        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private View orderCard(RiderDataManager.OrderData order, SimpleDateFormat sdf) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(glassCard());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(3), 0, dp(3));
        card.setLayoutParams(clp);

        // 商家+金额
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.setPadding(dp(14), dp(10), dp(14), dp(3));

        TextView store = new TextView(this);
        store.setText(order.storeName);
        store.setTextColor(ThemeEngine.TEXT_PRIMARY);
        store.setTextSize(15);
        store.setTypeface(null, 1);
        row1.addView(store);

        View sp1 = new View(this);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row1.addView(sp1);

        TextView price = new TextView(this);
        price.setText("¥" + String.format("%.1f", order.price));
        price.setTextColor(ThemeEngine.NEON_GOLD);
        price.setTextSize(16);
        price.setTypeface(null, 1);
        row1.addView(price);

        card.addView(row1);

        // 地址
        TextView addr = new TextView(this);
        addr.setText(order.storeAddress + " → " + order.customerAddress);
        addr.setTextColor(ThemeEngine.TEXT_DISABLED);
        addr.setTextSize(11);
        addr.setPadding(dp(14), dp(2), dp(14), dp(3));
        addr.setMaxLines(1);
        card.addView(addr);

        // 底部操作
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setPadding(dp(14), dp(3), dp(14), dp(10));

        TextView time = new TextView(this);
        time.setText("🕐 " + sdf.format(new Date(order.createTime)));
        time.setTextColor(ThemeEngine.TEXT_MUTED);
        time.setTextSize(10);
        row3.addView(time);

        TextView dist = new TextView(this);
        dist.setText(" 📏 " + order.distance + "km");
        dist.setTextColor(ThemeEngine.TEXT_MUTED);
        dist.setTextSize(10);
        row3.addView(dist);

        // 状态按钮
        View sp3 = new View(this);
        sp3.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row3.addView(sp3);

        String statusAction = "";
        int statusColor = 0;
        switch (order.status) {
            case 0: statusAction = "⚡抢单"; statusColor = ThemeEngine.NEON_ROSE; break;
            case 1: statusAction = "📞联系取货"; statusColor = 0xFF2196F3; break;
            case 2: statusAction = "🚚配送中"; statusColor = ThemeEngine.NEON_CYAN; break;
            case 3: statusAction = "✅确认送达"; statusColor = ThemeEngine.NEON_GREEN; break;
            case 4: statusAction = "已完成"; statusColor = ThemeEngine.NEON_GREEN; break;
        }
        TextView statusBtn = new TextView(this);
        statusBtn.setText(statusAction);
        statusBtn.setTextColor(statusColor);
        statusBtn.setTextSize(12);
        statusBtn.setTypeface(null, 1);
        statusBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
        statusBtn.setBackground(ThemeEngine.roundedBg(statusColor & 0x22FFFFFF, dp(6)));
        row3.addView(statusBtn);

        card.addView(row3);
        return card;
    }

    private View sectionTitle(String text) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.CENTER_VERTICAL);
        wrap.setPadding(dp(4), dp(12), dp(4), dp(4));
        View dot = new View(this);
        dot.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_CYAN, dp(3)));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(12)));
        dot.setPadding(dp(4), 0, dp(8), 0);
        wrap.addView(dot);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ThemeEngine.NEON_CYAN);
        tv.setTextSize(12);
        tv.setTypeface(null, 1);
        tv.setPadding(dp(8), 0, 0, 0);
        wrap.addView(tv);
        return wrap;
    }

    private LinearLayout glassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
        return card;
    }

    private TextView emptyIcon(String text) { TextView t = new TextView(this); t.setText(text); t.setTextSize(48); return t; }
    private TextView emptyText(String text) { TextView t = new TextView(this); t.setText(text); t.setTextColor(ThemeEngine.TEXT_DISABLED); t.setTextSize(16); t.setPadding(0, dp(12), 0, dp(4)); return t; }
    private TextView emptySub(String text) { TextView t = new TextView(this); t.setText(text); t.setTextColor(ThemeEngine.TEXT_MUTED); t.setTextSize(12); return t; }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            if (Build.VERSION.SDK_INT >= 28)
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        } catch (Exception ignored) {}
        try {
            getWindow().setNavigationBarColor(0xFF0D0D1A);
            getWindow().setStatusBarColor(ThemeEngine.BG_DARK);
        } catch (Exception ignored) {}
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

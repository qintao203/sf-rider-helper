package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

/**
 * 主导航容器
 * 底部5页导航：订单 | 收入 | 我的 | 抢单 | 顺丰
 * 完整骑手平台框架
 */
public class MainActivity2 extends Activity {

    private static final String[][] NAV_ITEMS = {
        {"📋", "订单"},
        {"💰", "收入"},
        {"👤", "我的"},
        {"⚡", "抢单"},
        {"📦", "顺丰"},
    };

    private LinearLayout contentArea;
    private Button[] navButtons;
    private int currentPage = 3; // 默认进入"抢单"页面
    private RiderDataManager dataManager;
    private LinearLayout navBar;
    private View contentRoot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        dataManager = RiderDataManager.getInstance();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // 内容区
        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        root.addView(contentArea);

        // 底部导航栏
        navBar = new LinearLayout(this);
        navBar.setOrientation(LinearLayout.HORIZONTAL);
        navBar.setBackgroundColor(0xFF0D0D1A);
        navBar.setPadding(0, dp(4), 0, dp(8));

        // 底部安全区适配
        if (Build.VERSION.SDK_INT >= 28) {
            navBar.setPadding(0, dp(4), 0, dp(12));
        }

        navButtons = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, dp(52, this), 1));
            tab.setClickable(true);
            tab.setTag(i);

            Button btn = new Button(this);
            btn.setText(NAV_ITEMS[i][0]);
            btn.setTextSize(18);
            btn.setBackground(null);
            btn.setPadding(0, 0, 0, 0);
            btn.setTag(i);
            btn.setIncludeFontPadding(false);
            tab.addView(btn);
            navButtons[i] = btn;

            TextView lbl = new TextView(this);
            lbl.setText(NAV_ITEMS[i][1]);
            lbl.setTextSize(9);
            lbl.setGravity(Gravity.CENTER);
            tab.addView(lbl);

            tab.setOnClickListener(v -> switchPage(idx));
            navBar.addView(tab);
        }

        root.addView(navBar);
        setContentView(root);

        // 默认显示抢单页
        switchPage(currentPage);
    }

    private void switchPage(int idx) {
        currentPage = idx;
        contentArea.removeAllViews();

        // 更新导航高亮
        for (int i = 0; i < 5; i++) {
            boolean active = i == idx;
            LinearLayout tab = (LinearLayout) navBar.getChildAt(i);
            tab.setAlpha(active ? 1.0f : 0.4f);
            TextView lbl = (TextView) tab.getChildAt(1);
            if (lbl != null) lbl.setTextColor(active ?
                    ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
            Button btn = (Button) tab.getChildAt(0);
            if (btn != null) btn.setTextColor(active ?
                    ThemeEngine.NEON_CYAN : ThemeEngine.TEXT_DISABLED);
        }

        // 加载对应页面
        View pageView = null;
        switch (idx) {
            case 0: // 订单
                pageView = OrdersFragment.create(this, dataManager);
                break;
            case 1: // 收入
                pageView = EarningsFragment.create(this, dataManager);
                break;
            case 2: // 我的
                pageView = ProfileFragment.create(this, dataManager);
                break;
            case 3: // 抢单助手（原有功能入口）
                pageView = createGrabPage();
                break;
            case 4: // 顺丰同城
                pageView = createSFPage();
                break;
        }

        if (pageView != null) {
            contentArea.addView(pageView);
        }

        // 页面切换动画
        contentArea.setAlpha(0.6f);
        contentArea.animate().alpha(1f).setDuration(200).start();
    }

    private View createGrabPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题
        TextView title = new TextView(this);
        title.setText("抢单助手");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(20);
        title.setTypeface(null, 1);
        title.setPadding(dp(16), dp(16), dp(16), dp(8));
        page.addView(title);

        // 大图标
        LinearLayout iconWrap = new LinearLayout(this);
        iconWrap.setGravity(Gravity.CENTER);
        iconWrap.setPadding(0, dp(40), 0, dp(24));
        TextView icon = new TextView(this);
        icon.setText("⚡");
        icon.setTextSize(64);
        iconWrap.addView(icon);
        page.addView(iconWrap);

        // 提示文本
        TextView desc = new TextView(this);
        desc.setText("智能抢单引擎已就绪");
        desc.setTextColor(ThemeEngine.NEON_GREEN);
        desc.setTextSize(18);
        desc.setTypeface(null, 1);
        desc.setGravity(Gravity.CENTER);
        page.addView(desc);

        TextView sub = new TextView(this);
        sub.setText("自动扫描 · 多维评分 · 三级策略");
        sub.setTextColor(ThemeEngine.TEXT_DISABLED);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, dp(30));
        page.addView(sub);

        // 进入完整版按钮
        Button enterBtn = new Button(this);
        enterBtn.setText("⚡ 打开完整抢单面板");
        enterBtn.setTextColor(0xFF0A0A14);
        enterBtn.setTextSize(16);
        enterBtn.setTypeface(null, 1);
        enterBtn.setBackground(ThemeEngine.diagonalGradient(
                new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35}, ThemeEngine.RADIUS_XLARGE));
        enterBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        ((LinearLayout.LayoutParams)enterBtn.getLayoutParams()).setMargins(dp(24), 0, dp(24), 0);
        enterBtn.setElevation(dp(4));
        enterBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
        page.addView(enterBtn);

        return page;
    }

    private View createSFPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题
        TextView title = new TextView(this);
        title.setText("顺丰同城");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(20);
        title.setTypeface(null, 1);
        title.setPadding(dp(16), dp(16), dp(16), dp(8));
        page.addView(title);

        boolean installed = SFRiderBridge.isInstalled(this);

        // 状态
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER);
        statusRow.setPadding(0, dp(32), 0, dp(16));

        TextView icon = new TextView(this);
        icon.setText("📦");
        icon.setTextSize(56);
        statusRow.addView(icon);
        page.addView(statusRow);

        TextView statusText = new TextView(this);
        statusText.setText(installed ? "顺丰同城骑士 ✓ 已安装" : "顺丰同城骑士未安装");
        statusText.setTextColor(installed ? ThemeEngine.NEON_GREEN : ThemeEngine.TEXT_DISABLED);
        statusText.setTextSize(16);
        statusText.setTypeface(null, 1);
        statusText.setGravity(Gravity.CENTER);
        page.addView(statusText);

        TextView subText = new TextView(this);
        subText.setText(installed ? "点击下方按钮一键启动" : "请先安装顺丰同城骑士APP");
        subText.setTextColor(ThemeEngine.TEXT_DISABLED);
        subText.setTextSize(12);
        subText.setGravity(Gravity.CENTER);
        subText.setPadding(0, dp(6), 0, dp(32));
        page.addView(subText);

        // 按钮
        Button launchBtn = new Button(this);
        launchBtn.setText(installed ? "📦 打开顺丰同城" : "📥 前往安装");
        launchBtn.setTextColor(0xFF0A0A14);
        launchBtn.setTextSize(16);
        launchBtn.setTypeface(null, 1);
        launchBtn.setBackground(ThemeEngine.diagonalGradient(
                new int[]{0xFF00E5FF, 0xFF00B8D4}, ThemeEngine.RADIUS_XLARGE));
        launchBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(50)));
        ((LinearLayout.LayoutParams)launchBtn.getLayoutParams()).setMargins(dp(24), 0, dp(24), dp(12));
        launchBtn.setElevation(dp(4));
        launchBtn.setOnClickListener(v -> {
            if (!SFRiderBridge.launchSFApp(this)) {
                SFRiderBridge.openMarket(this);
            }
        });
        page.addView(launchBtn);

        // 同屏提示
        LinearLayout tipWrap = new LinearLayout(this);
        tipWrap.setGravity(Gravity.CENTER);
        tipWrap.setPadding(dp(24), dp(8), dp(24), 0);
        TextView tip = new TextView(this);
        tip.setText("💡 开启悬浮窗后，可在顺丰同城上叠加显示抢单数据");
        tip.setTextColor(ThemeEngine.TEXT_MUTED);
        tip.setTextSize(11);
        tip.setGravity(Gravity.CENTER);
        tipWrap.addView(tip);
        page.addView(tipWrap);

        return page;
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                getWindow().getAttributes().layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
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
}

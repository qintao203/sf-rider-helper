package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ConfigManager config;
    private TextView tvGrabbed, tvFailed, tvSkipped, tvIncome, tvUptime;
    private Button btnToggle;
    private View statusIndicator;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A14);

        // ========== 顶部状态区 ==========
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(20), dp(12), dp(20), dp(12));
        topBar.setBackgroundColor(0xFF12121E);
        topBar.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        statusIndicator = new View(this);
        statusIndicator.setBackground(dot(0xFF6B6B80, 8));
        statusIndicator.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
        ((LinearLayout.LayoutParams)statusIndicator.getLayoutParams()).setMargins(0, 0, dp(8), 0);
        topBar.addView(statusIndicator);

        TextView tvServiceLabel = new TextView(this);
        tvServiceLabel.setText("抢单服务");
        tvServiceLabel.setTextColor(0xFF9E9EB8);
        tvServiceLabel.setTextSize(14);
        topBar.addView(tvServiceLabel);

        TextView tvServiceStatus = new TextView(this);
        tvServiceStatus.setText("未启动");
        tvServiceStatus.setTextColor(0xFF6B6B80);
        tvServiceStatus.setTextSize(14);
        tvServiceStatus.setGravity(Gravity.END);
        tvServiceStatus.setTag("status_text");
        LinearLayout.LayoutParams ssp = new LinearLayout.LayoutParams(0, -2, 1);
        ssp.setMargins(dp(8), 0, 0, 0);
        tvServiceStatus.setLayoutParams(ssp);
        topBar.addView(tvServiceStatus);

        root.addView(topBar);

        // ========== 运行时间（大号数字） ==========
        LinearLayout centerWrap = new LinearLayout(this);
        centerWrap.setOrientation(LinearLayout.VERTICAL);
        centerWrap.setGravity(Gravity.CENTER);
        centerWrap.setPadding(0, dp(40), 0, dp(32));

        tvUptime = new TextView(this);
        tvUptime.setText("--:--:--");
        tvUptime.setTextColor(0xFFFFFFFF);
        tvUptime.setTextSize(52);
        tvUptime.setTypeface(null, 1);
        tvUptime.setGravity(Gravity.CENTER);
        centerWrap.addView(tvUptime);

        TextView tvUptimeLabel = new TextView(this);
        tvUptimeLabel.setText("运行时间");
        tvUptimeLabel.setTextColor(0xFF555570);
        tvUptimeLabel.setTextSize(12);
        tvUptimeLabel.setGravity(Gravity.CENTER);
        tvUptimeLabel.setPadding(0, dp(4), 0, 0);
        centerWrap.addView(tvUptimeLabel);

        root.addView(centerWrap);

        // ========== 统计卡片（4列） ==========
        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setPadding(dp(16), 0, dp(16), dp(16));

        tvGrabbed = makeStatCard(statRow, "成功", 0);
        tvFailed = makeStatCard(statRow, "失败", 0);
        tvSkipped = makeStatCard(statRow, "过滤", 0);
        tvIncome = makeStatCard(statRow, "收入", 0);
        root.addView(statRow);

        // ========== 功能网格 ==========
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(16), 0, dp(16), dp(12));
        grid.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        // 第一行：启动 + 暂停
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        btnToggle = new Button(this);
        btnToggle.setText("启动服务");
        btnToggle.setTextColor(0xFFFFFFFF);
        btnToggle.setTextSize(15);
        btnToggle.setTypeface(null, 1);
        btnToggle.setBackground(btnBg(0xFFE94560, 12));
        btnToggle.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
        btnToggle.setOnClickListener(v -> toggle());
        row1.addView(btnToggle);

        root.addView(row1);

        // 第二行：功能按钮（设置/日志/重置）
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(0, dp(12), 0, 0);

        row2.addView(makeGridBtn("设置", "⚙️", () -> startActivity(new Intent(this, SettingsActivity.class))));
        row2.addView(makeGridBtn("日志", "📊", () -> startActivity(new Intent(this, StatsActivity.class))));
        row2.addView(makeGridBtn("重置", "🔄", () -> { config.resetStats(); refreshStats(); }));

        root.addView(row2);

        // ========== 版本信息（底部） ==========
        TextView tvVer = new TextView(this);
        tvVer.setText("顺丰抢单助手 v5.5.0");
        tvVer.setTextColor(0xFF2A2A3E);
        tvVer.setTextSize(11);
        tvVer.setGravity(Gravity.CENTER);
        tvVer.setPadding(0, dp(16), 0, dp(12));
        root.addView(tvVer);

        setContentView(root);
        refreshStats();
        updateServiceUI();

        // 定时刷新
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() { if (!isFinishing()) { refreshStats(); updateServiceUI(); getWindow().getDecorView().postDelayed(this, 3000); } }
        }, 3000);
    }

    // ---------- 构建方法 ----------

    private TextView makeStatCard(LinearLayout parent, String label, int colorRes) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(card(0xFF151525));
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(80), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);

        TextView num = new TextView(this);
        num.setText("0");
        num.setTextSize(22);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        card.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF6B6B80);
        lbl.setTextSize(11);
        lbl.setGravity(Gravity.CENTER);
        lbl.setPadding(0, dp(2), 0, 0);
        card.addView(lbl);

        parent.addView(card);
        return num;
    }

    private LinearLayout makeGridBtn(String label, String icon, Runnable onClick) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(card(0xFF151525));
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(72), 1));
        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        btn.setClickable(true);
        btn.setFocusable(true);

        TextView iv = new TextView(this);
        iv.setText(icon);
        iv.setTextSize(20);
        btn.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFF9E9EB8);
        tv.setTextSize(12);
        btn.addView(tv);

        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    // ---------- 全屏设置 ----------

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemInsets(false);
            getWindow().getInsetsController().hide(
                android.view.WindowInsets.Type.statusBars() |
                android.view.WindowInsets.Type.navigationBars());
            getWindow().getInsetsController().setSystemBarsBehavior(
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // ---------- 服务控制 ----------

    private void toggle() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (s != null && s.isActive()) {
            s.stopGrabLoop();
        } else {
            if (!isAccEnabled()) {
                try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
                return;
            }
            if (s == null) return;
            s.startGrabLoop();
        }
        updateServiceUI();
    }

    private boolean isAccEnabled() {
        try {
            String a = getPackageName() + "/" + GrabAccessibilityService.class.getCanonicalName();
            String e = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return e != null && e.contains(a);
        } catch (Exception ex) { return false; }
    }

    private void updateServiceUI() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        boolean r = s != null && s.isActive();
        TextView tvStatus = findViewById(android.R.id.content).getRootView().findViewWithTag("status_text");
        if (tvStatus == null) return;

        if (r) {
            btnToggle.setText("停止服务");
            btnToggle.setBackground(btnBg(0xFF33334D, 12));
            statusIndicator.setBackground(dot(0xFF4CAF50, 8));
            tvStatus.setText("运行中");
            tvStatus.setTextColor(0xFF4CAF50);
        } else {
            btnToggle.setText("启动服务");
            btnToggle.setBackground(btnBg(0xFFE94560, 12));
            statusIndicator.setBackground(dot(0xFF6B6B80, 8));
            tvStatus.setText("未启动");
            tvStatus.setTextColor(0xFF6B6B80);
        }
    }

    private void refreshStats() {
        int g = config.getStatGrabbed(), f = config.getStatFailed(), sk = config.getStatSkipped();
        tvGrabbed.setText(String.valueOf(g));
        tvFailed.setText(String.valueOf(f));
        tvSkipped.setText(String.valueOf(sk));
        tvIncome.setText("¥" + (g * 12));
        tvGrabbed.setTextColor(0xFF4CAF50);
        tvFailed.setTextColor(0xFFE94560);
        tvSkipped.setTextColor(0xFF888888);
        tvIncome.setTextColor(0xFFFFD700);
    }

    // ---------- 工具 ----------

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private android.graphics.drawable.GradientDrawable dot(int color, int r) {
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL);
        g.setColor(color); g.setSize(dp(r), dp(r)); return g;
    }
    private android.graphics.drawable.GradientDrawable card(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(10));
        g.setStroke(dp(1), 0x1AFFFFFF); return g;
    }
    private android.graphics.drawable.GradientDrawable btnBg(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }
}

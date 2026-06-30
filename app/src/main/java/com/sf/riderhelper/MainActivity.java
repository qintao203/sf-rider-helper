package com.sf.riderhelper;

import android.animation.ValueAnimator;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ConfigManager config;
    private Handler handler = new Handler();
    private long startTime = 0;
    private boolean isRunning = false;

    private TextView tvGrabbed, tvFailed, tvSkipped, tvIncome, tvUptime, tvStatusText;
    private Button btnToggle;
    private View statusDot, glowBg;

    // 霓虹配色映射表
    private static final int[] STAT_COLORS = {
        ThemeEngine.NEON_GREEN,   // 成功
        ThemeEngine.NEON_ROSE,    // 失败
        ThemeEngine.TEXT_DISABLED,// 过滤
        ThemeEngine.NEON_GOLD     // 收入
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);

        // ========== 根布局 ==========
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // ========== 背景光晕 ==========
        glowBg = new View(this);
        glowBg.setBackground(diagonalGradient(
                new int[]{0x22FF3366, 0x0800E5FF, 0x00000000}));
        glowBg.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(280)));
        ((LinearLayout.LayoutParams)glowBg.getLayoutParams()).topMargin = dp(-80);
        root.addView(glowBg);

        // 光晕呼吸动画
        ValueAnimator glowAnim = ValueAnimator.ofFloat(0.3f, 0.8f);
        glowAnim.setDuration(2500);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAnim.addUpdateListener(a -> glowBg.setAlpha((float) a.getAnimatedValue()));
        glowAnim.start();

        // ========== 顶部状态栏 ==========
        LinearLayout topBar = makeTopBar();
        root.addView(topBar);

        // ========== 策略状态行 ==========
        LinearLayout strategyBar = new LinearLayout(this);
        strategyBar.setOrientation(LinearLayout.HORIZONTAL);
        strategyBar.setGravity(Gravity.CENTER_VERTICAL);
        strategyBar.setPadding(dp(20), dp(4), dp(20), dp(8));

        View sDot = new View(this);
        sDot.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_PURPLE, dp(3)));
        sDot.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
        ((LinearLayout.LayoutParams)sDot.getLayoutParams()).setMargins(0, 0, dp(6), 0);
        strategyBar.addView(sDot);

        TextView tvStratLabel = new TextView(this);
        tvStratLabel.setText("策略");
        tvStratLabel.setTextColor(ThemeEngine.TEXT_DISABLED);
        tvStratLabel.setTextSize(11);
        strategyBar.addView(tvStratLabel);

        final TextView tvStratValue = new TextView(this);
        tvStratValue.setText(config.getStrategyMode());
        tvStratValue.setTextColor(ThemeEngine.NEON_PURPLE);
        tvStratValue.setTextSize(11);
        tvStratValue.setTypeface(null, 1);
        tvStratValue.setPadding(dp(6), 0, dp(16), 0);
        tvStratValue.setTag("strategy_value");
        strategyBar.addView(tvStratValue);

        TextView tvFilterLabel = new TextView(this);
        tvFilterLabel.setText("过滤");
        tvFilterLabel.setTextColor(ThemeEngine.TEXT_DISABLED);
        tvFilterLabel.setTextSize(11);
        strategyBar.addView(tvFilterLabel);

        final TextView tvFilterValue = new TextView(this);
        tvFilterValue.setText("等待");
        tvFilterValue.setTextColor(ThemeEngine.TEXT_MUTED);
        tvFilterValue.setTextSize(11);
        tvFilterValue.setTag("filter_value");
        strategyBar.addView(tvFilterValue);

        // 省电/勿扰/悬浮球状态图标
        if (config.isPowerSaving()) addStatusTag(strategyBar, "省电", ThemeEngine.NEON_GREEN);
        if (config.isDndEnabled()) addStatusTag(strategyBar, "勿扰", ThemeEngine.NEON_ORANGE);
        if (config.isFloatingBall()) addStatusTag(strategyBar, "悬浮", ThemeEngine.NEON_PURPLE);

        root.addView(strategyBar);

        // ========== 中间大时钟 ==========
        LinearLayout clockWrap = new LinearLayout(this);
        clockWrap.setOrientation(LinearLayout.VERTICAL);
        clockWrap.setGravity(Gravity.CENTER);
        clockWrap.setPadding(0, dp(36), 0, dp(28));

        tvUptime = new TextView(this);
        tvUptime.setText("--:--:--");
        tvUptime.setTextColor(ThemeEngine.NEON_CYAN);
        tvUptime.setTextSize(54);
        tvUptime.setTypeface(null, 1);
        tvUptime.setGravity(Gravity.CENTER);
        tvUptime.setAlpha(0f);
        clockWrap.addView(tvUptime);

        TextView uptimeLabel = new TextView(this);
        uptimeLabel.setText("━ 运行时长 ━");
        uptimeLabel.setTextColor(ThemeEngine.TEXT_DISABLED);
        uptimeLabel.setTextSize(11);
        uptimeLabel.setLetterSpacing(0.1f);
        uptimeLabel.setGravity(Gravity.CENTER);
        uptimeLabel.setPadding(0, dp(6), 0, 0);
        uptimeLabel.setAlpha(0f);
        clockWrap.addView(uptimeLabel);

        root.addView(clockWrap);

        // ========== 四列统计卡片（玻璃拟态） ==========
        LinearLayout statRow = new LinearLayout(this);
        statRow.setOrientation(LinearLayout.HORIZONTAL);
        statRow.setPadding(dp(16), 0, dp(16), dp(16));
        statRow.setAlpha(0f);

        tvGrabbed = makeGlassStatCard(statRow, "成功", 0);
        tvFailed  = makeGlassStatCard(statRow, "失败", 1);
        tvSkipped = makeGlassStatCard(statRow, "过滤", 2);
        tvIncome  = makeGlassStatCard(statRow, "收入", 3);
        root.addView(statRow);

        // ========== 启动按钮（渐变霓虹） ==========
        LinearLayout btnWrap = new LinearLayout(this);
        btnWrap.setGravity(Gravity.CENTER);
        btnWrap.setPadding(dp(16), 0, dp(16), 0);

        btnToggle = new Button(this);
        btnToggle.setText("⚡ 启动服务");
        btnToggle.setTextColor(0xFF0A0A14);
        btnToggle.setTextSize(16);
        btnToggle.setTypeface(null, 1);
        btnToggle.setIncludeFontPadding(false);
        btnToggle.setBackground(neonButtonGradient(
                new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35},
                new int[]{0xFFFF3366, 0xFFFF4500},
                ThemeEngine.RADIUS_XLARGE));
        btnToggle.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52)));
        btnToggle.setElevation(dp(4));
        btnToggle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                v.setElevation(dp(2));
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                       event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                        .setInterpolator(new AccelerateDecelerateInterpolator()).start();
                v.setElevation(dp(4));
            }
            return false;
        });
        btnToggle.setOnClickListener(v -> toggle());
        btnWrap.addView(btnToggle);

        root.addView(btnWrap);

        // ========== 三功能按钮 ==========
        LinearLayout funcRow = new LinearLayout(this);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        funcRow.setPadding(dp(16), dp(14), dp(16), 0);
        funcRow.setAlpha(0f);

        funcRow.addView(makeNeonFuncBtn("设置", "⚙", () ->
                startActivity(new Intent(this, SettingsActivity.class))));
        funcRow.addView(makeNeonFuncBtn("日志", "▦", () ->
                startActivity(new Intent(this, StatsActivity.class))));
        funcRow.addView(makeNeonFuncBtn("重置", "↺", () -> {
            config.resetStats(); refreshStats();
        }));

        root.addView(funcRow);

        // ========== 版本号 ==========
        TextView tvVer = new TextView(this);
        tvVer.setText("顺丰抢单助手 v6.0 · 霓虹版");
        tvVer.setTextColor(ThemeEngine.TEXT_MUTED);
        tvVer.setTextSize(10);
        tvVer.setGravity(Gravity.CENTER);
        tvVer.setPadding(0, dp(20), 0, dp(14));
        tvVer.setAlpha(0f);
        root.addView(tvVer);

        setContentView(root);

        // ========== 入场序列动画 ==========
        tvUptime.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        uptimeLabel.animate().alpha(1f).setDuration(400).setStartDelay(400).start();
        statRow.animate().alpha(1f).setDuration(400).setStartDelay(550).start();
        funcRow.animate().alpha(1f).setDuration(400).setStartDelay(700).start();
        tvVer.animate().alpha(1f).setDuration(400).setStartDelay(850).start();

        refreshStats();
        updateServiceUI();

        // ========== 定时刷新 ==========
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    refreshStats();
                    updateServiceUI();
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    // ==================== UI构建 ====================

    private LinearLayout makeTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(12), dp(20), dp(12));
        bar.setBackground(null);

        statusDot = new View(this);
        statusDot.setBackground(ThemeEngine.dot(ThemeEngine.TEXT_DISABLED, 8, statusDot));
        statusDot.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
        ((LinearLayout.LayoutParams)statusDot.getLayoutParams()).setMargins(0, 0, dp(8), 0);
        bar.addView(statusDot);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("抢单服务");
        tvLabel.setTextColor(ThemeEngine.TEXT_SECONDARY);
        tvLabel.setTextSize(14);
        bar.addView(tvLabel);

        tvStatusText = new TextView(this);
        tvStatusText.setText("未启动");
        tvStatusText.setTextColor(ThemeEngine.TEXT_DISABLED);
        tvStatusText.setTextSize(13);
        tvStatusText.setGravity(Gravity.END);
        tvStatusText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        ((LinearLayout.LayoutParams)tvStatusText.getLayoutParams()).setMargins(dp(8), 0, 0, 0);
        bar.addView(tvStatusText);

        return bar;
    }

    private TextView makeGlassStatCard(LinearLayout parent, String label, int idx) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(82), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);

        // 顶部小装饰线
        View accentLine = new View(this);
        accentLine.setBackground(ThemeEngine.roundedBg(STAT_COLORS[idx], 1));
        accentLine.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(2)));
        card.addView(accentLine);

        TextView num = new TextView(this);
        num.setText("0");
        num.setTextColor(STAT_COLORS[idx]);
        num.setTextSize(20);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        num.setPadding(0, dp(6), 0, dp(2));
        card.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        lbl.setTextSize(10);
        lbl.setGravity(Gravity.CENTER);
        card.addView(lbl);

        parent.addView(card);
        return num;
    }

    private LinearLayout makeNeonFuncBtn(String label, String icon, Runnable onClick) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, dp(70), 1));
        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(dp(4), 0, dp(4), 0);
        btn.setClickable(true);
        btn.setFocusable(true);

        // 触摸反馈
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                       event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }
            return false;
        });

        TextView iv = new TextView(this);
        iv.setText(icon);
        iv.setTextColor(ThemeEngine.TEXT_SECONDARY);
        iv.setTextSize(20);
        btn.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(ThemeEngine.TEXT_DISABLED);
        tv.setTextSize(11);
        btn.addView(tv);

        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    // ==================== 服务控制 ====================

    private void toggle() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (s != null && s.isActive()) {
            s.stopGrabLoop();
            isRunning = false;
            startTime = 0;
        } else {
            if (!isAccEnabled()) {
                try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
                return;
            }
            if (s == null) return;
            s.startGrabLoop();
            isRunning = true;
            startTime = SystemClock.elapsedRealtime();
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
        boolean p = s != null && s.isPaused();
        if (tvStatusText == null || btnToggle == null) return;

        if (r) {
            statusDot.setBackground(ThemeEngine.dot(
                    p ? ThemeEngine.NEON_ORANGE : ThemeEngine.NEON_GREEN, 8, statusDot));
            tvStatusText.setText(p ? "已暂停" : "运行中");
            tvStatusText.setTextColor(p ? ThemeEngine.NEON_ORANGE : ThemeEngine.NEON_GREEN);
            btnToggle.setText("⏻ 停止服务");
            btnToggle.setBackground(neonButtonGradient(
                    new int[]{ThemeEngine.BG_CARD_DARK, ThemeEngine.BG_CARD},
                    new int[]{0xFF1A1A30, 0xFF151525},
                    ThemeEngine.RADIUS_XLARGE));
            btnToggle.setTextColor(ThemeEngine.TEXT_SECONDARY);

            // 更新策略状态
            if (s.getLastFilterResult() != null) {
                updateFilterDisplay(s);
            }
        } else {
            statusDot.setBackground(ThemeEngine.dot(ThemeEngine.TEXT_DISABLED, 8, statusDot));
            tvStatusText.setText("未启动");
            tvStatusText.setTextColor(ThemeEngine.TEXT_DISABLED);
            btnToggle.setText("⚡ 启动服务");
            btnToggle.setBackground(neonButtonGradient(
                    new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35},
                    new int[]{0xFFFF3366, 0xFFFF4500},
                    ThemeEngine.RADIUS_XLARGE));
            btnToggle.setTextColor(0xFF0A0A14);
        }
    }

    private void updateFilterDisplay(GrabAccessibilityService s) {
        GrabFilterEngine.FilterResult fr = s.getLastFilterResult();
        TextView tvFilter = findViewById(android.R.id.content).getRootView()
                .findViewWithTag("filter_value");
        TextView tvStrat = findViewById(android.R.id.content).getRootView()
                .findViewWithTag("strategy_value");
        if (tvFilter != null) {
            tvFilter.setText(fr.getStrategyName() + " " + fr.score + "分");
            int color = fr.score >= 80 ? ThemeEngine.NEON_GREEN :
                        fr.score >= 50 ? ThemeEngine.NEON_CYAN :
                        fr.score >= 20 ? ThemeEngine.NEON_ORANGE : ThemeEngine.NEON_ROSE;
            tvFilter.setTextColor(color);
        }
        if (tvStrat != null && s.getLastTier() != null) {
            tvStrat.setText(s.getLastTier().label);
        }
    }

    private void refreshStats() {
        int g = config.getStatGrabbed(), f = config.getStatFailed(), sk = config.getStatSkipped();
        tvGrabbed.setText(String.valueOf(g));
        tvFailed.setText(String.valueOf(f));
        tvSkipped.setText(String.valueOf(sk));
        tvIncome.setText("¥" + (g * 12));
    }

    /** 添加状态标签 */
    private void addStatusTag(LinearLayout parent, String text, int color) {
        View dot = new View(this);
        dot.setBackground(ThemeEngine.dot(color, 5, dot));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(5), dp(5)));
        ((LinearLayout.LayoutParams)dot.getLayoutParams()).setMargins(dp(8), 0, dp(3), 0);
        parent.addView(dot);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(9);
        parent.addView(tv);
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    // ==================== 工具 ====================

        private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

        private GradientDrawable diagonalGradient(int[] colors) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        return g;
    }

    private GradientDrawable neonButtonGradient(int[] normalColors, int[] pressedColors, int radius) {
        return ThemeEngine.diagonalGradient(normalColors, radius);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

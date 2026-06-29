package com.sf.riderhelper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private View glowView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // ========== 顶部光晕 ==========
        glowView = new View(this);
        glowView.setBackground(diagonalGradient(
                new int[]{0x33FF3366, 0x0000E5FF, 0x00000000}));
        glowView.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(240)));
        ((LinearLayout.LayoutParams)glowView.getLayoutParams()).topMargin = dp(-60);
        root.addView(glowView);

        // ========== LOGO ==========
        LinearLayout logoWrap = new LinearLayout(this);
        logoWrap.setOrientation(LinearLayout.VERTICAL);
        logoWrap.setGravity(Gravity.CENTER);
        logoWrap.setPadding(0, dp(40), 0, 0);

        // 图标容器（圆形霓虹框）
        LinearLayout iconCircle = new LinearLayout(this);
        iconCircle.setGravity(Gravity.CENTER);
        iconCircle.setBackground(glassCard(0x1A00E5FF, dp(50), 0x3300E5FF));
        iconCircle.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(100)));

        TextView icon = new TextView(this);
        icon.setText("⚡");
        icon.setTextSize(44);
        iconCircle.addView(icon);
        logoWrap.addView(iconCircle);

        // 标题
        TextView appName = new TextView(this);
        appName.setText("顺丰抢单助手");
        appName.setTextColor(ThemeEngine.TEXT_PRIMARY);
        appName.setTextSize(24);
        appName.setTypeface(null, 1);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, dp(16), 0, dp(4));
        logoWrap.addView(appName);

        // 副标题
        TextView sub = new TextView(this);
        sub.setText("SF RIDER HELPER · PRO");
        sub.setTextColor(ThemeEngine.NEON_CYAN);
        sub.setTextSize(12);
        sub.setLetterSpacing(0.15f);
        sub.setGravity(Gravity.CENTER);
        logoWrap.addView(sub);

        root.addView(logoWrap);

        // ========== 版本号 ==========
        TextView ver = new TextView(this);
        ver.setText("v6.0");
        ver.setTextColor(ThemeEngine.TEXT_MUTED);
        ver.setTextSize(12);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(8), 0, 0);
        root.addView(ver);

        // ========== 底部加载动画 ==========
        LinearLayout bottomWrap = new LinearLayout(this);
        bottomWrap.setOrientation(LinearLayout.HORIZONTAL);
        bottomWrap.setGravity(Gravity.CENTER);
        bottomWrap.setPadding(0, dp(60), 0, 0);

        final View dot1 = new View(this);
        dot1.setBackground(ThemeEngine.dot(ThemeEngine.NEON_CYAN, 6, dot1));
        dot1.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
        ((LinearLayout.LayoutParams)dot1.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        bottomWrap.addView(dot1);

        final View dot2 = new View(this);
        dot2.setBackground(ThemeEngine.dot(ThemeEngine.NEON_GREEN, 6, dot2));
        dot2.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
        ((LinearLayout.LayoutParams)dot2.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        bottomWrap.addView(dot2);

        final View dot3 = new View(this);
        dot3.setBackground(ThemeEngine.dot(ThemeEngine.NEON_ROSE, 6, dot3));
        dot3.setLayoutParams(new LinearLayout.LayoutParams(dp(6), dp(6)));
        ((LinearLayout.LayoutParams)dot3.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        bottomWrap.addView(dot3);

        // 加载点动画
        startDotAnim(dot1, 0);
        startDotAnim(dot2, 300);
        startDotAnim(dot3, 600);

        root.addView(bottomWrap);

        // ========== 加载文本 ==========
        final TextView loading = new TextView(this);
        loading.setText("正在初始化系统");
        loading.setTextColor(ThemeEngine.TEXT_DISABLED);
        loading.setTextSize(12);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(12), 0, 0);
        root.addView(loading);

        // 加载文本闪烁
        loading.postDelayed(new Runnable() {
            String[] msgs = {"正在初始化系统", "加载配置中...", "启动服务引擎...", "准备就绪"};
            int idx = 0;
            @Override
            public void run() {
                idx = (idx + 1) % msgs.length;
                loading.setText(msgs[idx]);
                loading.postDelayed(this, 400);
            }
        }, 400);

        setContentView(root);

        // ========== 光晕呼吸动画 ==========
        ValueAnimator glowAnim = ValueAnimator.ofFloat(0.3f, 1.0f);
        glowAnim.setDuration(2000);
        glowAnim.setRepeatCount(ValueAnimator.INFINITE);
        glowAnim.setRepeatMode(ValueAnimator.REVERSE);
        glowAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        glowAnim.addUpdateListener(a ->
                glowView.setAlpha((float) a.getAnimatedValue()));
        glowAnim.start();

        // ========== 入场动画 ==========
        iconCircle.setAlpha(0f);
        iconCircle.setScaleX(0.5f);
        iconCircle.setScaleY(0.5f);
        iconCircle.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(600).setStartDelay(200).start();

        appName.setAlpha(0f);
        appName.animate().alpha(1f).setDuration(500).setStartDelay(500).start();

        sub.setAlpha(0f);
        sub.animate().alpha(1f).setDuration(400).setStartDelay(700).start();

        // ========== 跳转主页面 ==========
        new Handler(getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2500);
    }

    private void startDotAnim(final View dot, long delay) {
        dot.postDelayed(new Runnable() {
            boolean up = true;
            @Override
            public void run() {
                if (dot.getWindowToken() == null) return;
                float s = up ? 1.8f : 0.6f;
                dot.animate().scaleX(s).scaleY(s).setDuration(400).start();
                up = !up;
                dot.postDelayed(this, 600);
            }
        }, delay);
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

        private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
            findViewById(android.R.id.content) : new View(this)); }

    private GradientDrawable glassCard(int color, int radius, int borderColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(radius); g.setStroke(1, borderColor);
        return g;
    }

    private GradientDrawable diagonalGradient(int[] colors) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        return g;
    }
}

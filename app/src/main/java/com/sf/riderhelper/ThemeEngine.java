package com.sf.riderhelper;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * 主题引擎：统一管理配色、圆角、动效、Drawable工厂
 */
public class ThemeEngine {

    // ========== 色彩系统 ==========
    public static final int BG_DARK        = 0xFF07070F;  // 最深背景
    public static final int BG_SURFACE     = 0xFF0D0D1A;  // 表面层
    public static final int BG_CARD        = 0xCC151525;  // 卡片（半透明）
    public static final int BG_CARD_DARK   = 0xCC0F0F1E;  // 深色卡片
    public static final int BG_TOPBAR      = 0xCC0A0A18;  // 顶栏

    public static final int NEON_CYAN      = 0xFF00E5FF;  // 青霓虹（主色）
    public static final int NEON_GREEN     = 0xFF00FF88;  // 绿霓虹（成功）
    public static final int NEON_ROSE      = 0xFFFF3366;  // 玫红霓虹（危险）
    public static final int NEON_GOLD      = 0xFFFFD700;  // 金色（收入）
    public static final int NEON_PURPLE    = 0xFFBB86FC;  // 紫霓虹（辅助）
    public static final int NEON_ORANGE    = 0xFFFF6D00;  // 橙霓虹（警告）

    public static final int TEXT_PRIMARY   = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFF9E9EB8;
    public static final int TEXT_DISABLED  = 0xFF555570;
    public static final int TEXT_MUTED     = 0xFF3A3A55;

    public static final int BORDER_CARD    = 0x1AFFFFFF;  // 卡片边框
    public static final int BORDER_GLOW    = 0x33FFFFFF;  // 发光边框

    // ========== 圆角系统 ==========
    public static final int RADIUS_SMALL  = 8;
    public static final int RADIUS_MEDIUM = 12;
    public static final int RADIUS_LARGE  = 16;
    public static final int RADIUS_XLARGE = 24;

    // ========== 间距工具 ==========
    public static int dp(float n, View v) {
        return (int)(n * v.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ========== Drawable工厂 ==========

    /** 玻璃卡片 - 半透明+描边 */
    public static GradientDrawable glassCard(int color, int radius, int borderColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        g.setStroke(1, borderColor);
        return g;
    }

    /** 圆角纯色背景 */
    public static GradientDrawable roundedBg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    /** 水平渐变背景 */
    public static GradientDrawable gradientBg(int startColor, int endColor, int radius) {
        GradientDrawable g = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{startColor, endColor});
        g.setCornerRadius(radius);
        return g;
    }

    /** 对角线渐变背景（科技感） */
    public static GradientDrawable diagonalGradient(int[] colors, int radius) {
        GradientDrawable g = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors);
        g.setCornerRadius(radius);
        return g;
    }

    /** 霓虹按钮（带按下状态） */
    public static StateListDrawable neonButton(int normalColor, int pressedColor, int radius) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed},
                roundedBg(pressedColor, radius));
        sld.addState(new int[]{},
                roundedBg(normalColor, radius));
        return sld;
    }

    /** 圆形指示点 */
    public static GradientDrawable dot(int color, int size, View v) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        g.setSize(dp(size, v), dp(size, v));
        return g;
    }

    // ========== 动效工具 ==========

    /** 按钮按下缩放动画 */
    public static void pressAnim(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f)
                .setDuration(80).start();
    }

    /** 按钮释放回弹 */
    public static void releaseAnim(View v) {
        v.animate().scaleX(1.0f).scaleY(1.0f)
                .setDuration(150)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /** 入场：淡入+上移 */
    public static void fadeInUp(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(20, v));
        v.animate().alpha(1f).translationY(0)
                .setDuration(400)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** 呼吸脉冲动画（持续） */
    public static ValueAnimator breatheAnim(View v, int color1, int color2, long duration) {
        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), color1, color2);
        anim.setDuration(duration);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.addUpdateListener(a ->
                v.setBackgroundColor((int) a.getAnimatedValue()));
        anim.start();
        return anim;
    }

    /** 数字滚动动画 */
    public static void countUp(final View v, final int from, final int to, long duration) {
        if (Build.VERSION.SDK_INT >= 26) {
            ValueAnimator anim = ValueAnimator.ofInt(from, to);
            anim.setDuration(duration);
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.addUpdateListener(a -> {
                if (v instanceof android.widget.TextView) {
                    ((android.widget.TextView) v).setText(
                            String.valueOf(a.getAnimatedValue()));
                }
            });
            anim.start();
        } else {
            if (v instanceof android.widget.TextView) {
                ((android.widget.TextView) v).setText(String.valueOf(to));
            }
        }
    }

    /** 脉冲光晕背景（呼吸光效） */
    public static void startBreathingBg(final View v, long delay) {
        v.postDelayed(new Runnable() {
            boolean up = true;
            float alpha = 0.3f;
            @Override
            public void run() {
                if (v.getWindowToken() == null) return;
                alpha = up ? alpha + 0.02f : alpha - 0.02f;
                if (alpha >= 0.6f) up = false;
                if (alpha <= 0.15f) up = true;
                v.setAlpha(alpha);
                v.postDelayed(this, 50);
            }
        }, delay);
    }
}

package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * 我的页面 - 骑手资料+收入统计+设置
 */
public class ProfileFragment {

    public static View create(Activity activity, RiderDataManager dataManager) {
        RiderDataManager.RiderProfile profile = dataManager.getProfile();
        Object[] stats = dataManager.getStats();
        int totalOrders = (int) stats[0];
        int completed = (int) stats[1];
        float rate = totalOrders > 0 ? (completed * 100f / totalOrders) : 0;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题
        root.addView(pageTitle(activity, "我的"));

        ScrollView sv = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16, activity), dp(8, activity), dp(16, activity), dp(24, activity));

        // 骑手资料卡
        content.addView(profileCard(activity, profile));

        // 收入概览
        content.addView(sectionTitle(activity, "💵 收入概览"));
        content.addView(incomeRow(activity, "今日收入", profile.todayIncome, ThemeEngine.NEON_CYAN));
        content.addView(incomeRow(activity, "本周收入", profile.weekIncome, ThemeEngine.NEON_GREEN));
        content.addView(incomeRow(activity, "本月收入", profile.monthIncome, ThemeEngine.NEON_GOLD));

        // 数据统计
        content.addView(sectionTitle(activity, "📊 数据统计"));
        content.addView(statsRow(activity, "完成单数", String.valueOf(profile.completedOrders), ThemeEngine.NEON_GREEN));
        content.addView(statsRow(activity, "成功率", String.format("%.0f%%", rate), ThemeEngine.NEON_CYAN));
        content.addView(statsRow(activity, "在线时长", profile.onlineHours + "小时", ThemeEngine.NEON_PURPLE));
        content.addView(statsRow(activity, "总里程", profile.totalDistance + "km", ThemeEngine.NEON_ORANGE));

        // 底部按钮
        Button btnGrab = new Button(activity);
        btnGrab.setText("⚡ 打开抢单助手");
        btnGrab.setTextColor(0xFF0A0A14);
        btnGrab.setTextSize(15);
        btnGrab.setTypeface(null, 1);
        btnGrab.setBackground(ThemeEngine.diagonalGradient(
                new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35}, ThemeEngine.RADIUS_LARGE));
        btnGrab.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48, activity)));
        ((LinearLayout.LayoutParams)btnGrab.getLayoutParams()).setMargins(0, dp(16, activity), 0, 0);
        btnGrab.setOnClickListener(v -> {
            Intent intent = new Intent(activity, MainActivity2.class);
            activity.startActivity(intent);
        });
        content.addView(btnGrab);

        sv.addView(content);
        root.addView(sv);
        return root;
    }

    private static View profileCard(Activity a, RiderDataManager.RiderProfile p) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_LARGE, ThemeEngine.BORDER_GLOW));
        card.setPadding(dp(16, a), dp(14, a), dp(16, a), dp(14, a));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(12, a), 0, dp(8, a));
        card.setLayoutParams(clp);

        // 头像占位
        TextView avatar = new TextView(a);
        avatar.setText("👤");
        avatar.setTextSize(36);
        avatar.setPadding(dp(12, a), dp(12, a), dp(12, a), dp(12, a));
        avatar.setBackground(ThemeEngine.glassCard(0x2200E5FF, dp(36, a), 0x4400E5FF));
        card.addView(avatar);

        LinearLayout info = new LinearLayout(a);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12, a), 0, 0, 0);

        TextView name = new TextView(a);
        name.setText(p.name + " · Lv." + p.level);
        name.setTextColor(ThemeEngine.TEXT_PRIMARY);
        name.setTextSize(18);
        name.setTypeface(null, 1);
        info.addView(name);

        TextView phone = new TextView(a);
        phone.setText(p.phone);
        phone.setTextColor(ThemeEngine.TEXT_DISABLED);
        phone.setTextSize(12);
        info.addView(phone);

        // 评分
        TextView rating = new TextView(a);
        rating.setText("⭐ " + p.rating + " 分  ·  " + p.completedOrders + " 单");
        rating.setTextColor(ThemeEngine.NEON_GOLD);
        rating.setTextSize(12);
        info.addView(rating);

        card.addView(info);
        return card;
    }

    private static View incomeRow(Activity a, String label, float value, int color) {
        LinearLayout row = glassRow(a);
        TextView lbl = mkLabel(a, label);
        row.addView(lbl);

        TextView val = new TextView(a);
        val.setText("¥" + String.format("%.1f", value));
        val.setTextColor(color);
        val.setTextSize(16);
        val.setTypeface(null, 1);
        row.addView(val);
        return row;
    }

    private static View statsRow(Activity a, String label, String value, int color) {
        LinearLayout row = glassRow(a);
        row.addView(mkLabel(a, label));
        TextView val = new TextView(a);
        val.setText(value);
        val.setTextColor(color);
        val.setTextSize(16);
        val.setTypeface(null, 1);
        row.addView(val);
        return row;
    }

    // ========== 工具 ==========

    private static TextView pageTitle(Activity a, String text) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(ThemeEngine.TEXT_PRIMARY);
        tv.setTextSize(20);
        tv.setTypeface(null, 1);
        tv.setPadding(dp(16, a), dp(16, a), dp(16, a), dp(8, a));
        return tv;
    }

    private static TextView sectionTitle(Activity a, String text) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setGravity(Gravity.CENTER_VERTICAL);
        wrap.setPadding(dp(4, a), dp(16, a), dp(4, a), dp(6, a));

        View dot = new View(a);
        dot.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_CYAN, dp(3, a)));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(6, a), dp(14, a)));
        wrap.addView(dot);

        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(ThemeEngine.NEON_CYAN);
        tv.setTextSize(12);
        tv.setTypeface(null, 1);
        tv.setPadding(dp(8, a), 0, 0, 0);
        wrap.addView(tv);
        return wrap;
    }

    private static LinearLayout glassRow(Activity a) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
        row.setPadding(dp(14, a), dp(10, a), dp(14, a), dp(10, a));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(3, a), 0, dp(3, a));
        row.setLayoutParams(rlp);
        return row;
    }

    private static TextView mkLabel(Activity a, String text) {
        TextView lbl = new TextView(a);
        lbl.setText(text);
        lbl.setTextColor(ThemeEngine.TEXT_SECONDARY);
        lbl.setTextSize(14);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        return lbl;
    }

    private static int dp(int n, Activity a) {
        return (int)(n * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}

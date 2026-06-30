package com.sf.riderhelper;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

/**
 * 收入统计页 - 收益看板+图表
 */
public class EarningsFragment {

    public static View create(Activity activity, RiderDataManager dataManager) {
        RiderDataManager.RiderProfile profile = dataManager.getProfile();
        Object[] stats = dataManager.getStats();
        int totalOrders = (int) stats[0];
        int completed = (int) stats[1];
        float successRate = totalOrders > 0 ? (completed * 100f / totalOrders) : 0;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题
        TextView title = new TextView(activity);
        title.setText("收入");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(20);
        title.setTypeface(null, 1);
        title.setPadding(dp(16, activity), dp(16, activity), dp(16, activity), dp(8, activity));
        root.addView(title);

        // 大号今日收入
        LinearLayout todayWrap = new LinearLayout(activity);
        todayWrap.setOrientation(LinearLayout.VERTICAL);
        todayWrap.setGravity(Gravity.CENTER);
        todayWrap.setPadding(0, dp(20, activity), 0, dp(16, activity));

        TextView todayVal = new TextView(activity);
        todayVal.setText("¥" + String.format("%.0f", profile.todayIncome));
        todayVal.setTextColor(ThemeEngine.NEON_GOLD);
        todayVal.setTextSize(48);
        todayVal.setTypeface(null, 1);
        todayWrap.addView(todayVal);

        TextView todayLbl = new TextView(activity);
        todayLbl.setText("今日收入");
        todayLbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        todayLbl.setTextSize(13);
        todayWrap.addView(todayLbl);

        root.addView(todayWrap);

        // 三日/周/月对比
        LinearLayout periodRow = new LinearLayout(activity);
        periodRow.setOrientation(LinearLayout.HORIZONTAL);
        periodRow.setPadding(dp(16, activity), 0, dp(16, activity), dp(16, activity));

        float[][] periods = {
            {"日".hashCode(), profile.todayIncome, 0xFF4CAF50},
            {"周".hashCode(), profile.weekIncome, 0xFF00E5FF},
            {"月".hashCode(), profile.monthIncome, 0xFFFFD700},
        };

        periodRow.addView(periodCard(activity, "今日", profile.todayIncome, ThemeEngine.NEON_GREEN));
        periodRow.addView(periodCard(activity, "本周", profile.weekIncome, ThemeEngine.NEON_CYAN));
        periodRow.addView(periodCard(activity, "本月", profile.monthIncome, ThemeEngine.NEON_GOLD));

        root.addView(periodRow);

        // 收入走势迷你图
        LinearLayout chartSection = new LinearLayout(activity);
        chartSection.setOrientation(LinearLayout.VERTICAL);
        chartSection.setPadding(dp(16, activity), 0, dp(16, activity), dp(16, activity));
        chartSection.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));

        TextView chartTitle = new TextView(activity);
        chartTitle.setText("📈 本周收入走势");
        chartTitle.setTextColor(ThemeEngine.TEXT_SECONDARY);
        chartTitle.setTextSize(12);
        chartTitle.setTypeface(null, 1);
        chartTitle.setPadding(dp(4, activity), dp(8, activity), 0, dp(6, activity));
        chartSection.addView(chartTitle);

        // 迷你柱状图
        LinearLayout miniChart = new LinearLayout(activity);
        miniChart.setOrientation(LinearLayout.HORIZONTAL);
        miniChart.setGravity(Gravity.BOTTOM);
        miniChart.setPadding(dp(8, activity), dp(8, activity), dp(8, activity), dp(8, activity));

        Random rng = new Random(42);
        String[] days = {"一", "二", "三", "四", "五", "六", "日"};
        float[] values = new float[7];
        float maxVal = 0;
        for (int i = 0; i < 7; i++) {
            values[i] = 80 + rng.nextFloat() * 120;
            if (values[i] > maxVal) maxVal = values[i];
        }
        values[6] = profile.todayIncome; // 今天用真实值

        for (int i = 0; i < 7; i++) {
            LinearLayout barCol = new LinearLayout(activity);
            barCol.setOrientation(LinearLayout.VERTICAL);
            barCol.setGravity(Gravity.CENTER);
            barCol.setLayoutParams(new LinearLayout.LayoutParams(0, dp(80, activity), 1));

            float h = (values[i] / Math.max(maxVal, 1)) * dp(50, activity);

            View bar = new View(activity);
            bar.setBackground(ThemeEngine.roundedBg(i == 6 ? ThemeEngine.NEON_GOLD : ThemeEngine.NEON_CYAN, dp(4, activity)));
            bar.setLayoutParams(new LinearLayout.LayoutParams(dp(16, activity), (int)h));
            barCol.addView(bar);

            TextView lbl = new TextView(activity);
            lbl.setText(days[i]);
            lbl.setTextColor(i == 6 ? ThemeEngine.NEON_GOLD : ThemeEngine.TEXT_MUTED);
            lbl.setTextSize(8);
            lbl.setGravity(Gravity.CENTER);
            lbl.setPadding(0, dp(2, activity), 0, 0);
            barCol.addView(lbl);

            miniChart.addView(barCol);
        }

        chartSection.addView(miniChart);
        root.addView(chartSection);

        // 统计数据
        LinearLayout statsSection = new LinearLayout(activity);
        statsSection.setOrientation(LinearLayout.VERTICAL);
        statsSection.setPadding(dp(16, activity), 0, dp(16, activity), dp(24, activity));

        TextView statsTitle = new TextView(activity);
        statsTitle.setText("📊 数据汇总");
        statsTitle.setTextColor(ThemeEngine.NEON_CYAN);
        statsTitle.setTextSize(12);
        statsTitle.setTypeface(null, 1);
        statsTitle.setPadding(dp(4, activity), dp(12, activity), 0, dp(6, activity));
        statsSection.addView(statsTitle);

        statsSection.addView(statRow(activity, "总订单", String.valueOf(totalOrders), ThemeEngine.TEXT_PRIMARY));
        statsSection.addView(statRow(activity, "已完成", String.valueOf(completed), ThemeEngine.NEON_GREEN));
        statsSection.addView(statRow(activity, "成功率", String.format("%.0f%%", successRate), ThemeEngine.NEON_CYAN));
        statsSection.addView(statRow(activity, "总收入", "¥" + String.format("%.0f", profile.monthIncome), ThemeEngine.NEON_GOLD));
        statsSection.addView(statRow(activity, "总里程", profile.totalDistance + " km", ThemeEngine.NEON_PURPLE));
        statsSection.addView(statRow(activity, "在线时长", profile.onlineHours + " 小时", ThemeEngine.NEON_ORANGE));

        root.addView(statsSection);

        return root;
    }

    private static View periodCard(Activity a, String label, float value, int color) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(60, a), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(3, a), 0, dp(3, a), 0);

        TextView v = new TextView(a);
        v.setText("¥" + String.format("%.0f", value));
        v.setTextColor(color);
        v.setTextSize(13);
        v.setTypeface(null, 1);
        card.addView(v);

        TextView l = new TextView(a);
        l.setText(label);
        l.setTextColor(ThemeEngine.TEXT_DISABLED);
        l.setTextSize(10);
        card.addView(l);
        return card;
    }

    private static View statRow(Activity a, String label, String value, int color) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
        row.setPadding(dp(14, a), dp(8, a), dp(14, a), dp(8, a));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, dp(3, a), 0, dp(3, a));
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(a);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_SECONDARY);
        lbl.setTextSize(13);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(lbl);

        TextView val = new TextView(a);
        val.setText(value);
        val.setTextColor(color);
        val.setTextSize(15);
        val.setTypeface(null, 1);
        row.addView(val);
        return row;
    }

    private static int dp(int n, Activity a) {
        return (int)(n * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}

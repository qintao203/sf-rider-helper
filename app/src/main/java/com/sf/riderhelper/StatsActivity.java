package com.sf.riderhelper;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

public class StatsActivity extends Activity {
    private ConfigManager config;
    private OrderDatabase orderDb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);
        orderDb = OrderDatabase.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // ========== 顶部导航 ==========
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(12), dp(16), dp(12));
        header.setBackgroundColor(ThemeEngine.BG_TOPBAR);

        Button back = new Button(this);
        back.setText("←");
        back.setTextColor(ThemeEngine.TEXT_SECONDARY);
        back.setTextSize(18);
        back.setBackground(null);
        back.setPadding(dp(12), dp(4), dp(12), dp(4));
        back.setOnClickListener(v -> finish());
        header.addView(back);

        View accent = new View(this);
        accent.setBackground(ThemeEngine.roundedBg(ThemeEngine.NEON_GREEN, dp(4)));
        accent.setLayoutParams(new LinearLayout.LayoutParams(dp(4), dp(4)));
        ((LinearLayout.LayoutParams)accent.getLayoutParams()).setMargins(dp(4), 0, dp(8), 0);
        header.addView(accent);

        TextView title = new TextView(this);
        title.setText("订单历史");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(title);

        root.addView(header);

        // ========== 获取数据 ==========
        OrderDatabase.Stats stats = orderDb.getStats();
        List<OrderDatabase.OrderRecord> records = orderDb.getRecent(100);

        // ========== 统计概览卡 ==========
        LinearLayout statBar = new LinearLayout(this);
        statBar.setOrientation(LinearLayout.HORIZONTAL);
        statBar.setPadding(dp(16), dp(12), dp(16), dp(4));

        statBar.addView(makeStatPill("成功", String.valueOf(stats.totalGrabbed), ThemeEngine.NEON_GREEN));
        statBar.addView(makeStatPill("失败", String.valueOf(stats.totalFailed), ThemeEngine.NEON_ROSE));
        statBar.addView(makeStatPill("过滤", String.valueOf(stats.totalSkipped), ThemeEngine.TEXT_DISABLED));
        statBar.addView(makeStatPill("收入", "¥" + String.format("%.0f", stats.totalIncome), ThemeEngine.NEON_GOLD));

        root.addView(statBar);

        // ========== 订单列表 ==========
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        LinearLayout listContent = new LinearLayout(this);
        listContent.setOrientation(LinearLayout.VERTICAL);
        listContent.setPadding(dp(16), dp(8), dp(16), dp(24));

        if (records.isEmpty()) {
            // 空状态
            LinearLayout emptyWrap = new LinearLayout(this);
            emptyWrap.setOrientation(LinearLayout.VERTICAL);
            emptyWrap.setGravity(Gravity.CENTER);
            emptyWrap.setPadding(0, dp(60), 0, 0);

            TextView ei = new TextView(this);
            ei.setText("📋");
            ei.setTextSize(36);
            emptyWrap.addView(ei);

            TextView et = new TextView(this);
            et.setText("暂无记录");
            et.setTextColor(ThemeEngine.TEXT_DISABLED);
            et.setTextSize(14);
            et.setPadding(0, dp(12), 0, dp(4));
            emptyWrap.addView(et);

            TextView es = new TextView(this);
            es.setText("启动服务后自动记录");
            es.setTextColor(ThemeEngine.TEXT_MUTED);
            es.setTextSize(12);
            emptyWrap.addView(es);

            listContent.addView(emptyWrap);
        } else {
            // 列表头
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setPadding(dp(12), dp(6), dp(12), dp(4));

            String[] cols = {"时间", "策略", "金额", "方向", "结果"};
            int[] widths = {56, 36, 48, 60, 36};
            for (int i = 0; i < cols.length; i++) {
                TextView h = new TextView(this);
                h.setText(cols[i]);
                h.setTextColor(ThemeEngine.TEXT_MUTED);
                h.setTextSize(9);
                h.setLayoutParams(new LinearLayout.LayoutParams(dp(widths[i]), -2));
                headerRow.addView(h);
            }
            listContent.addView(headerRow);

            // 分割线
            View divider = new View(this);
            divider.setBackgroundColor(0x1AFFFFFF);
            divider.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
            ((LinearLayout.LayoutParams)divider.getLayoutParams()).setMargins(0, dp(4), 0, dp(4));
            listContent.addView(divider);

            // 每行记录
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            for (OrderDatabase.OrderRecord rec : records) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD,
                        ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
                row.setPadding(dp(12), dp(8), dp(12), dp(8));
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
                rlp.setMargins(0, dp(2), 0, dp(2));
                row.setLayoutParams(rlp);

                int resultColor = rec.isSuccess() ? ThemeEngine.NEON_GREEN : ThemeEngine.NEON_ROSE;

                // 时间
                TextView tvTime = new TextView(this);
                tvTime.setText(sdf.format(new Date(rec.timestamp)));
                tvTime.setTextColor(ThemeEngine.TEXT_MUTED);
                tvTime.setTextSize(10);
                tvTime.setLayoutParams(new LinearLayout.LayoutParams(dp(56), -2));
                row.addView(tvTime);

                // 策略标签
                TextView tvStrat = new TextView(this);
                tvStrat.setText(rec.strategy != null ? rec.strategy : "-");
                tvStrat.setTextColor(stratColor(rec.strategy));
                tvStrat.setTextSize(9);
                tvStrat.setTypeface(null, 1);
                tvStrat.setLayoutParams(new LinearLayout.LayoutParams(dp(36), -2));
                row.addView(tvStrat);

                // 金额
                TextView tvPrice = new TextView(this);
                tvPrice.setText("¥" + String.format("%.1f", rec.price));
                tvPrice.setTextColor(ThemeEngine.TEXT_PRIMARY);
                tvPrice.setTextSize(13);
                tvPrice.setTypeface(null, 1);
                tvPrice.setLayoutParams(new LinearLayout.LayoutParams(dp(48), -2));
                row.addView(tvPrice);

                // 方向
                TextView tvDir = new TextView(this);
                tvDir.setText(rec.direction != null && !rec.direction.isEmpty() ?
                        rec.direction : "-");
                tvDir.setTextColor(ThemeEngine.TEXT_SECONDARY);
                tvDir.setTextSize(10);
                tvDir.setLayoutParams(new LinearLayout.LayoutParams(dp(60), -2));
                row.addView(tvDir);

                // 结果
                TextView tvRes = new TextView(this);
                tvRes.setText(rec.isSuccess() ? "✓" : "✗");
                tvRes.setTextColor(resultColor);
                tvRes.setTextSize(14);
                tvRes.setTypeface(null, 1);
                tvRes.setGravity(Gravity.CENTER);
                tvRes.setLayoutParams(new LinearLayout.LayoutParams(dp(36), -2));
                row.addView(tvRes);

                listContent.addView(row);
            }

            // 清除按钮
            if (!records.isEmpty()) {
                Button clearBtn = new Button(this);
                clearBtn.setText("🗑 清除全部记录");
                clearBtn.setTextColor(ThemeEngine.NEON_ROSE);
                clearBtn.setTextSize(12);
                clearBtn.setBackground(ThemeEngine.glassCard(
                        0x22FF3366, ThemeEngine.RADIUS_SMALL, 0x22FF3366));
                clearBtn.setPadding(dp(20), dp(8), dp(20), dp(8));
                clearBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                ((LinearLayout.LayoutParams)clearBtn.getLayoutParams()).setMargins(0, dp(12), 0, 0);
                clearBtn.setOnClickListener(v -> {
                    orderDb.clearAll();
                    config.resetStats();
                    recreate();
                });
                listContent.addView(clearBtn);
            }
        }

        sv.addView(listContent);
        root.addView(sv);

        setContentView(root);

        // 入场动画
        statBar.setAlpha(0f);
        statBar.animate().alpha(1f).setDuration(300).setStartDelay(200).start();
    }

    private int stratColor(String strategy) {
        if (strategy == null) return ThemeEngine.TEXT_DISABLED;
        switch (strategy) {
            case "高优": return ThemeEngine.NEON_GREEN;
            case "中优": return ThemeEngine.NEON_CYAN;
            case "保底": return ThemeEngine.NEON_ORANGE;
            default: return ThemeEngine.TEXT_DISABLED;
        }
    }

    private LinearLayout makeStatPill(String label, String value, int color) {
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD,
                ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));
        pill.setLayoutParams(new LinearLayout.LayoutParams(0, dp(64), 1));
        ((LinearLayout.LayoutParams)pill.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        pill.setPadding(0, dp(6), 0, dp(6));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(16);
        v.setTypeface(null, 1);
        v.setGravity(Gravity.CENTER);
        pill.addView(v);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        lbl.setTextSize(10);
        lbl.setGravity(Gravity.CENTER);
        pill.addView(lbl);

        return pill;
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setNavigationBarColor(ThemeEngine.BG_DARK);
        getWindow().setStatusBarColor(ThemeEngine.BG_DARK);
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}

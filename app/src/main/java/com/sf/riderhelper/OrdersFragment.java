package com.sf.riderhelper;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 订单列表页 - 完整骑手订单管理
 */
public class OrdersFragment {

    public static View create(Activity activity, RiderDataManager dataManager) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题+状态标签
        root.addView(titleBar(activity, dataManager));

        ScrollView sv = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12, activity), dp(4, activity), dp(12, activity), dp(24, activity));

        List<RiderDataManager.OrderData> orders = dataManager.getOrders();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        if (orders.isEmpty()) {
            content.addView(emptyState(activity));
        } else {
            for (RiderDataManager.OrderData order : orders) {
                content.addView(orderCard(activity, order, sdf, dataManager));
            }
        }

        sv.addView(content);
        root.addView(sv);
        return root;
    }

    private static View titleBar(Activity a, RiderDataManager dm) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16, a), dp(14, a), dp(16, a), dp(10, a));

        TextView title = new TextView(a);
        title.setText("订单");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(20);
        title.setTypeface(null, 1);
        bar.addView(title);

        // 统计标签
        Object[] stats = dm.getStats();
        int available = dm.getOrdersByStatus(0).size();
        int active = dm.getOrdersByStatus(1).size() + dm.getOrdersByStatus(2).size() + dm.getOrdersByStatus(3).size();

        bar.addView(tag(a, "待抢 " + available, ThemeEngine.NEON_ROSE));
        bar.addView(tag(a, "进行 " + active, ThemeEngine.NEON_ORANGE));
        bar.addView(tag(a, "完成 " + stats[1], ThemeEngine.NEON_GREEN));

        return bar;
    }

    private static View orderCard(Activity a, RiderDataManager.OrderData order,
                                   SimpleDateFormat sdf, RiderDataManager dm) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_MEDIUM, ThemeEngine.BORDER_CARD));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, dp(4, a), 0, dp(4, a));
        card.setLayoutParams(clp);

        // 第一行：商家+金额+状态
        LinearLayout row1 = new LinearLayout(a);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        row1.setPadding(dp(14, a), dp(10, a), dp(14, a), dp(4, a));

        // 状态圆点
        View dot = new View(a);
        dot.setBackground(dot(a, statusColor(order.status), 8));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(8, a), dp(8, a)));
        row1.addView(dot);

        TextView store = new TextView(a);
        store.setText(order.storeName);
        store.setTextColor(ThemeEngine.TEXT_PRIMARY);
        store.setTextSize(15);
        store.setTypeface(null, 1);
        store.setPadding(dp(8, a), 0, dp(8, a), 0);
        row1.addView(store);

        View sp1 = new View(a);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row1.addView(sp1);

        TextView price = new TextView(a);
        price.setText("¥" + String.format("%.1f", order.price));
        price.setTextColor(ThemeEngine.NEON_GOLD);
        price.setTextSize(18);
        price.setTypeface(null, 1);
        row1.addView(price);

        card.addView(row1);

        // 第二行：地址+距离
        LinearLayout row2 = new LinearLayout(a);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setPadding(dp(14, a), dp(2, a), dp(14, a), dp(2, a));

        TextView addr = new TextView(a);
        addr.setText(order.storeAddress + " → " + order.customerAddress);
        addr.setTextColor(ThemeEngine.TEXT_SECONDARY);
        addr.setTextSize(11);
        addr.setMaxLines(1);
        row2.addView(addr);

        card.addView(row2);

        // 第三行：时间+距离+品类
        LinearLayout row3 = new LinearLayout(a);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setPadding(dp(14, a), dp(2, a), dp(14, a), dp(8, a));

        row3.addView(infoTag(a, "🕐 " + sdf.format(new Date(order.createTime))));
        row3.addView(infoTag(a, "📏 " + order.distance + "km"));

        if (order.weight > 0) {
            row3.addView(infoTag(a, "⚖ " + order.weight + "kg"));
        }

        View sp3 = new View(a);
        sp3.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1));
        row3.addView(sp3);

        // 类别标签
        TextView cat = new TextView(a);
        cat.setText(order.category);
        cat.setTextColor(ThemeEngine.TEXT_MUTED);
        cat.setTextSize(10);
        cat.setBackground(ThemeEngine.roundedBg(0x1AFFFFFF, dp(6, a)));
        cat.setPadding(dp(6, a), dp(2, a), dp(6, a), dp(2, a));
        row3.addView(cat);

        card.addView(row3);

        // 操作按钮（根据状态显示）
        if (order.status == 0) {
            Button grabBtn = new Button(a);
            grabBtn.setText("⚡ 抢单");
            grabBtn.setTextColor(0xFF0A0A14);
            grabBtn.setTextSize(13);
            grabBtn.setTypeface(null, 1);
            grabBtn.setBackground(ThemeEngine.diagonalGradient(
                    new int[]{ThemeEngine.NEON_ROSE, 0xFFFF6B35}, ThemeEngine.RADIUS_SMALL));
            grabBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(36, a)));
            ((LinearLayout.LayoutParams)grabBtn.getLayoutParams()).setMargins(
                    dp(8, a), dp(4, a), dp(8, a), dp(8, a));
            grabBtn.setOnClickListener(v -> {
                order.status = 1;
                dm.updateOrderStatus(order.id, 1);
            });
            card.addView(grabBtn);
        } else if (order.status == 1) {
            card.addView(actionBtn(a, "📦 标记已取货", 2, order, dm));
        } else if (order.status == 2) {
            card.addView(actionBtn(a, "🚚 开始配送", 3, order, dm));
        } else if (order.status == 3) {
            card.addView(actionBtn(a, "✅ 确认送达", 4, order, dm));
        } else if (order.status == 4) {
            LinearLayout doneRow = new LinearLayout(a);
            doneRow.setGravity(Gravity.CENTER);
            doneRow.setPadding(0, dp(4, a), 0, dp(8, a));

            TextView done = new TextView(a);
            done.setText("✓ 已完成 · " + order.strategy + "策略" +
                    (order.score > 0 ? " · " + order.score + "分" : ""));
            done.setTextColor(ThemeEngine.TEXT_MUTED);
            done.setTextSize(11);
            doneRow.addView(done);

            card.addView(doneRow);
        }

        return card;
    }

    private static View actionBtn(Activity a, String text, int targetStatus,
                                   RiderDataManager.OrderData order, RiderDataManager dm) {
        Button btn = new Button(a);
        btn.setText(text);
        btn.setTextColor(0xFF0A0A14);
        btn.setTextSize(13);
        btn.setTypeface(null, 1);
        btn.setBackground(ThemeEngine.diagonalGradient(
                new int[]{ThemeEngine.NEON_CYAN, 0xFF00B8D4}, ThemeEngine.RADIUS_SMALL));
        btn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(36, a)));
        ((LinearLayout.LayoutParams)btn.getLayoutParams()).setMargins(
                dp(8, a), dp(4, a), dp(8, a), dp(8, a));
        btn.setOnClickListener(v -> dm.updateOrderStatus(order.id, targetStatus));
        return btn;
    }

    private static View emptyState(Activity a) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(0, dp(80, a), 0, 0);

        TextView icon = new TextView(a);
        icon.setText("📋");
        icon.setTextSize(48);
        wrap.addView(icon);

        TextView t = new TextView(a);
        t.setText("暂无订单");
        t.setTextColor(ThemeEngine.TEXT_DISABLED);
        t.setTextSize(16);
        t.setPadding(0, dp(12, a), 0, dp(4, a));
        wrap.addView(t);

        TextView s = new TextView(a);
        s.setText("启动抢单服务后自动接收订单");
        s.setTextColor(ThemeEngine.TEXT_MUTED);
        s.setTextSize(12);
        wrap.addView(s);
        return wrap;
    }

    private static View tag(Activity a, String text, int color) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(10);
        tv.setTypeface(null, 1);
        tv.setBackground(ThemeEngine.roundedBg(0x22FFFFFF, dp(8, a)));
        tv.setPadding(dp(8, a), dp(3, a), dp(8, a), dp(3, a));
        tv.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        ((LinearLayout.LayoutParams)tv.getLayoutParams()).setMargins(dp(4, a), 0, 0, 0);
        return tv;
    }

    private static TextView infoTag(Activity a, String text) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(ThemeEngine.TEXT_MUTED);
        tv.setTextSize(10);
        tv.setPadding(0, 0, dp(10, a), 0);
        return tv;
    }

    private static int statusColor(int status) {
        switch (status) {
            case 0: return ThemeEngine.NEON_ROSE;
            case 1: return 0xFF2196F3;
            case 2: return ThemeEngine.NEON_ORANGE;
            case 3: return ThemeEngine.NEON_PURPLE;
            case 4: return ThemeEngine.NEON_GREEN;
            case 5: return ThemeEngine.TEXT_DISABLED;
            default: return ThemeEngine.TEXT_DISABLED;
        }
    }

    private static GradientDrawable dot(Activity a, int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private static int dp(int n, Activity a) {
        return (int)(n * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}

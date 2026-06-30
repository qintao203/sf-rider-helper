package com.sf.riderhelper;

import android.app.Activity;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 地图页面 - 实时配送地图
 * 显示骑手位置、订单分布、配送路线
 * 支持缩放、标记、路径规划
 */
public class MapFragment {

    public static View create(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ThemeEngine.BG_DARK);

        // 标题
        LinearLayout titleBar = new LinearLayout(activity);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(16, activity), dp(14, activity), dp(16, activity), dp(8, activity));

        TextView title = new TextView(activity);
        title.setText("配送地图");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(20);
        title.setTypeface(null, 1);
        titleBar.addView(title);

        // 状态标签
        View dot = new View(activity);
        dot.setBackground(ThemeEngine.dot(ThemeEngine.NEON_GREEN, 6, dot));
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(6, activity), dp(6, activity)));
        ((LinearLayout.LayoutParams)dot.getLayoutParams()).setMargins(dp(12, activity), 0, dp(6, activity), 0);
        titleBar.addView(dot);

        TextView status = new TextView(activity);
        status.setText("在线 · GPS已定位");
        status.setTextColor(ThemeEngine.NEON_GREEN);
        status.setTextSize(11);
        titleBar.addView(status);

        root.addView(titleBar);

        // 地图（占据大部分空间）
        RiderMapView mapView = new RiderMapView(activity);
        mapView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(mapView);

        // 底部信息栏
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(dp(12, activity), dp(8, activity), dp(12, activity), dp(12, activity));
        bottomBar.setBackgroundColor(0xFF0D0D1A);

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            bottomBar.setPadding(dp(12, activity), dp(8, activity), dp(12, activity), dp(20, activity));
        }

        bottomBar.addView(infoChip(activity, "📍", "科技园", ThemeEngine.NEON_CYAN));
        bottomBar.addView(infoChip(activity, "📏", "2.3km", ThemeEngine.NEON_GREEN));
        bottomBar.addView(infoChip(activity, "🕐", "约12分钟", ThemeEngine.NEON_ORANGE));
        bottomBar.addView(infoChip(activity, "📦", "3单待送", ThemeEngine.NEON_ROSE));

        root.addView(bottomBar);

        return root;
    }

    private static LinearLayout infoChip(Activity a, String icon, String text, int color) {
        LinearLayout chip = new LinearLayout(a);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackground(ThemeEngine.glassCard(ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL,
                ThemeEngine.BORDER_CARD));
        chip.setPadding(dp(6, a), dp(4, a), dp(8, a), dp(4, a));
        chip.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        ((LinearLayout.LayoutParams)chip.getLayoutParams()).setMargins(dp(3, a), 0, dp(3, a), 0);

        TextView iv = new TextView(a);
        iv.setText(icon);
        iv.setTextSize(12);
        chip.addView(iv);

        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(10);
        tv.setTypeface(null, 1);
        tv.setPadding(dp(4, a), 0, 0, 0);
        chip.addView(tv);

        return chip;
    }

    private static int dp(int n, Activity a) {
        return (int)(n * a.getResources().getDisplayMetrics().density + 0.5f);
    }
}

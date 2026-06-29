package com.sf.riderhelper;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StatsActivity extends Activity {
    private ConfigManager config;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0A14);

        // 顶部导航
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(12), dp(16), dp(12));
        header.setBackgroundColor(0xFF12121E);

        Button back = new Button(this);
        back.setText("← 返回");
        back.setTextColor(0xFF9E9EB8);
        back.setTextSize(14);
        back.setBackground(null);
        back.setOnClickListener(v -> finish());
        header.addView(back);

        TextView title = new TextView(this);
        title.setText("数据统计");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(title);

        TextView spacer = new TextView(this);
        spacer.setText("    ");
        spacer.setTextSize(14);
        header.addView(spacer);

        root.addView(header);

        // 统计卡
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(16), dp(24), dp(16), dp(24));
        card.setBackground(card(0xFF151525));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(dp(16), dp(16), dp(16), 0);
        card.setLayoutParams(clp);

        card.addView(statCell(config.getStatGrabbed(), "成功", 0xFF4CAF50));
        card.addView(statCell(config.getStatFailed(), "失败", 0xFFE94560));
        card.addView(statCell(config.getStatGrabbed() * 12, "收入", 0xFFFFD700));

        root.addView(card);

        // 空状态
        LinearLayout emptyWrap = new LinearLayout(this);
        emptyWrap.setOrientation(LinearLayout.VERTICAL);
        emptyWrap.setGravity(Gravity.CENTER);
        emptyWrap.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        TextView emptyIcon = new TextView(this);
        emptyIcon.setText("📋");
        emptyIcon.setTextSize(40);
        emptyWrap.addView(emptyIcon);

        TextView emptyText = new TextView(this);
        emptyText.setText("暂无抢单记录");
        emptyText.setTextColor(0xFF3A3A55);
        emptyText.setTextSize(14);
        emptyText.setPadding(0, dp(8), 0, dp(4));
        emptyText.setGravity(Gravity.CENTER);
        emptyWrap.addView(emptyText);

        TextView emptySub = new TextView(this);
        emptySub.setText("启动服务后自动记录");
        emptySub.setTextColor(0xFF2A2A3E);
        emptySub.setTextSize(12);
        emptySub.setGravity(Gravity.CENTER);
        emptyWrap.addView(emptySub);

        root.addView(emptyWrap);

        setContentView(root);
    }

    private LinearLayout statCell(int value, String label, int color) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        TextView num = new TextView(this);
        num.setText(value == 0 ? "0" : String.valueOf(value));
        num.setTextColor(color);
        num.setTextSize(28);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        cell.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF6B6B80);
        lbl.setTextSize(12);
        lbl.setGravity(Gravity.CENTER);
        lbl.setPadding(0, dp(4), 0, 0);
        cell.addView(lbl);

        return cell;
    }

    private void makeFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemInsets(false);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        getWindow().setNavigationBarColor(0xFF0A0A14);
        getWindow().setStatusBarColor(0xFF0A0A14);
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable card(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(12));
        g.setStroke(dp(1), 0x1AFFFFFF); return g;
    }
}

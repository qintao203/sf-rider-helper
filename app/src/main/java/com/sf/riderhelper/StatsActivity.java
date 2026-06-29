package com.sf.riderhelper;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
        title.setText("数据统计");
        title.setTextColor(ThemeEngine.TEXT_PRIMARY);
        title.setTextSize(17);
        title.setTypeface(null, 1);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(title);

        root.addView(header);

        // ========== 统计大卡片 ==========
        LinearLayout bigCard = new LinearLayout(this);
        bigCard.setOrientation(LinearLayout.HORIZONTAL);
        bigCard.setPadding(dp(20), dp(32), dp(20), dp(32));
        bigCard.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_LARGE, ThemeEngine.BORDER_GLOW));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(dp(16), dp(20), dp(16), 0);
        bigCard.setLayoutParams(clp);

        bigCard.addView(makeBigStatCell(config.getStatGrabbed(), "成功", ThemeEngine.NEON_GREEN));
        bigCard.addView(makeBigStatCell(config.getStatFailed(), "失败", ThemeEngine.NEON_ROSE));
        bigCard.addView(makeBigStatCell(config.getStatGrabbed() * 12, "收入", ThemeEngine.NEON_GOLD));

        root.addView(bigCard);

        // ========== 今日汇总小卡片 ==========
        LinearLayout miniRow = new LinearLayout(this);
        miniRow.setOrientation(LinearLayout.HORIZONTAL);
        miniRow.setPadding(dp(16), dp(10), dp(16), 0);

        int grabbed = config.getStatGrabbed();
        int failed = config.getStatFailed();
        float rate = (grabbed + failed) > 0 ? (float)grabbed / (grabbed + failed) * 100 : 0;
        float income = grabbed * 12;

        miniRow.addView(makeMiniCard("成功率", String.format("%.0f%%", rate), ThemeEngine.NEON_CYAN));
        miniRow.addView(makeMiniCard("总收入", "¥" + income, ThemeEngine.NEON_GOLD));
        miniRow.addView(makeMiniCard("总单数", String.valueOf(grabbed + failed), ThemeEngine.NEON_PURPLE));

        root.addView(miniRow);

        // ========== 空状态 ==========
        LinearLayout emptyWrap = new LinearLayout(this);
        emptyWrap.setOrientation(LinearLayout.VERTICAL);
        emptyWrap.setGravity(Gravity.CENTER);
        emptyWrap.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        emptyWrap.setPadding(0, dp(40), 0, 0);

        // 环形进度占位
        LinearLayout ringPlaceholder = new LinearLayout(this);
        ringPlaceholder.setGravity(Gravity.CENTER);
        ringPlaceholder.setBackground(ThemeEngine.glassCard(
                0x1A00E5FF, dp(50), 0x2200E5FF));
        ringPlaceholder.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(80)));

        TextView ringIcon = new TextView(this);
        ringIcon.setText("📊");
        ringIcon.setTextSize(32);
        ringPlaceholder.addView(ringIcon);
        emptyWrap.addView(ringPlaceholder);

        TextView emptyText = new TextView(this);
        emptyText.setText("暂无抢单记录");
        emptyText.setTextColor(ThemeEngine.TEXT_DISABLED);
        emptyText.setTextSize(14);
        emptyText.setPadding(0, dp(16), 0, dp(4));
        emptyText.setGravity(Gravity.CENTER);
        emptyWrap.addView(emptyText);

        TextView emptySub = new TextView(this);
        emptySub.setText("启动服务后自动记录每笔订单");
        emptySub.setTextColor(ThemeEngine.TEXT_MUTED);
        emptySub.setTextSize(12);
        emptySub.setGravity(Gravity.CENTER);
        emptyWrap.addView(emptySub);

        // 清除按钮
        if (grabbed + failed > 0) {
            Button clearBtn = new Button(this);
            clearBtn.setText("清除数据");
            clearBtn.setTextColor(ThemeEngine.NEON_ROSE);
            clearBtn.setTextSize(12);
            clearBtn.setBackground(ThemeEngine.glassCard(
                    0x22FF3366, ThemeEngine.RADIUS_SMALL, 0x22FF3366));
            clearBtn.setPadding(dp(16), dp(6), dp(16), dp(6));
            clearBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            ((LinearLayout.LayoutParams)clearBtn.getLayoutParams()).setMargins(0, dp(16), 0, 0);
            clearBtn.setOnClickListener(v -> {
                config.resetStats();
                recreate();
            });
            emptyWrap.addView(clearBtn);
        }

        root.addView(emptyWrap);

        setContentView(root);

        // ========== 入场动画 ==========
        bigCard.setAlpha(0f);
        bigCard.setTranslationY(dp(20));
        bigCard.animate().alpha(1f).translationY(0)
                .setDuration(400).setStartDelay(200).start();

        miniRow.setAlpha(0f);
        miniRow.animate().alpha(1f).setDuration(300).setStartDelay(400).start();

        ringPlaceholder.setScaleX(0f);
        ringPlaceholder.setScaleY(0f);
        ringPlaceholder.animate().scaleX(1f).scaleY(1f)
                .setDuration(400).setStartDelay(600).start();
    }

    private LinearLayout makeBigStatCell(int value, String label, int color) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        // 小装饰线
        View line = new View(this);
        line.setBackground(ThemeEngine.roundedBg(color, 1));
        line.setLayoutParams(new LinearLayout.LayoutParams(dp(16), dp(2)));
        cell.addView(line);

        TextView num = new TextView(this);
        num.setText(value == 0 ? "0" : String.valueOf(value));
        num.setTextColor(color);
        num.setTextSize(30);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        num.setPadding(0, dp(8), 0, dp(2));
        cell.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        lbl.setTextSize(12);
        lbl.setGravity(Gravity.CENTER);
        cell.addView(lbl);

        return cell;
    }

    private LinearLayout makeMiniCard(String label, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(ThemeEngine.glassCard(
                ThemeEngine.BG_CARD, ThemeEngine.RADIUS_SMALL, ThemeEngine.BORDER_CARD));
        card.setLayoutParams(new LinearLayout.LayoutParams(0, dp(56), 1));
        ((LinearLayout.LayoutParams)card.getLayoutParams()).setMargins(dp(3), 0, dp(3), 0);
        card.setPadding(0, dp(8), 0, dp(8));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(15);
        v.setTypeface(null, 1);
        v.setGravity(Gravity.CENTER);
        card.addView(v);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(ThemeEngine.TEXT_DISABLED);
        lbl.setTextSize(10);
        lbl.setGravity(Gravity.CENTER);
        card.addView(lbl);

        return card;
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

    private int dp(int n) { return ThemeEngine.dp(n, findViewById(android.R.id.content)); }
}

package com.sf.riderhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private ConfigManager config;
    private Handler handler = new Handler();
    private long startTime = 0;
    private TextView tvGrabbed, tvFailed, tvSkipped, tvIncome, tvUptime, tvShortStatus;
    private Button btnToggle, btnPause;
    private View statusDot;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        config = ConfigManager.getInstance(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0F0F1A);
        root.setPadding(16, 16, 16, 16);

        // 状态行
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);

        statusDot = new View(this);
        statusDot.setBackgroundColor(0xFF888888);
        statusDot.setLayoutParams(new LinearLayout.LayoutParams(10, 10));
        ((LinearLayout.LayoutParams)statusDot.getLayoutParams()).setMargins(0, 0, 6, 0);
        statusRow.addView(statusDot);

        tvShortStatus = new TextView(this);
        tvShortStatus.setText("未启动");
        tvShortStatus.setTextColor(0xFF888888);
        tvShortStatus.setTextSize(14);
        statusRow.addView(tvShortStatus);

        root.addView(statusRow);

        // 运行时间
        tvUptime = new TextView(this);
        tvUptime.setText("--:--:--");
        tvUptime.setTextColor(0xFF333355);
        tvUptime.setTextSize(36);
        tvUptime.setTypeface(null, 1);
        tvUptime.setGravity(Gravity.CENTER);
        root.addView(tvUptime);

        // 统计卡片
        LinearLayout statCard = new LinearLayout(this);
        statCard.setOrientation(LinearLayout.HORIZONTAL);
        statCard.setBackgroundColor(0xFF16213E);
        statCard.setPadding(4, 8, 4, 8);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, 16, 0, 0);
        statCard.setLayoutParams(slp);

        tvGrabbed = makeStatItem(statCard, "成功", 0xFF4CAF50);
        tvFailed = makeStatItem(statCard, "失败", 0xFFE94560);
        tvSkipped = makeStatItem(statCard, "过滤", 0xFF888888);
        tvIncome = makeStatItem(statCard, "收入", 0xFFFFD700);
        root.addView(statCard);

        // 控制按钮行
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 12, 0, 0);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        btnToggle = new Button(this);
        btnToggle.setText("启动服务");
        btnToggle.setTextColor(0xFFFFFFFF);
        btnToggle.setTextSize(15);
        btnToggle.setTypeface(null, 1);
        btnToggle.setBackgroundColor(0xFFE94560);
        btnToggle.setLayoutParams(new LinearLayout.LayoutParams(0, 48, 1));
        btnRow.addView(btnToggle);

        btnPause = new Button(this);
        btnPause.setText("暂停");
        btnPause.setTextColor(0xFFCCCCCC);
        btnPause.setTextSize(15);
        btnPause.setBackgroundColor(0xFF333355);
        btnPause.setLayoutParams(new LinearLayout.LayoutParams(0, 48, 1));
        ((LinearLayout.LayoutParams)btnPause.getLayoutParams()).leftMargin = 8;
        btnPause.setVisibility(View.GONE);
        btnRow.addView(btnPause);

        root.addView(btnRow);

        // 底部功能按钮
        LinearLayout funcRow = new LinearLayout(this);
        funcRow.setOrientation(LinearLayout.HORIZONTAL);
        funcRow.setPadding(0, 16, 0, 0);
        funcRow.setGravity(Gravity.CENTER_HORIZONTAL);

        funcRow.addView(makeFuncBtn("设置", "⚙️"));
        funcRow.addView(makeFuncBtn("日志", "📊"));
        funcRow.addView(makeFuncBtn("重置", "🔄"));

        root.addView(funcRow);

        // 版本号
        TextView tvVersion = new TextView(this);
        tvVersion.setText("顺丰抢单助手 v5.5.0");
        tvVersion.setTextColor(0xFF333355);
        tvVersion.setTextSize(11);
        tvVersion.setGravity(Gravity.CENTER);
        tvVersion.setPadding(0, 8, 0, 0);
        root.addView(tvVersion);

        setContentView(root);

        // 事件绑定
        btnToggle.setOnClickListener(v -> toggle());
        btnPause.setOnClickListener(v -> pause());

        doUpdateUI();
        doRefresh();

        // 定时刷新
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) { doRefresh(); doUpdateUI(); handler.postDelayed(this, 3000); }
            }
        }, 3000);
    }

    private TextView makeStatItem(LinearLayout parent, String label, int color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView num = new TextView(this);
        num.setText("0");
        num.setTextColor(color);
        num.setTextSize(22);
        num.setTypeface(null, 1);
        num.setGravity(Gravity.CENTER);
        item.addView(num);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF888888);
        lbl.setTextSize(11);
        lbl.setGravity(Gravity.CENTER);
        item.addView(lbl);

        parent.addView(item);
        return num;
    }

    private LinearLayout makeFuncBtn(String label, String icon) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(0xFF16213E);
        btn.setPadding(8, 12, 8, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 76, 1);
        lp.setMargins(4, 0, 4, 0);
        btn.setLayoutParams(lp);
        btn.setClickable(true);
        btn.setFocusable(true);

        TextView iv = new TextView(this);
        iv.setText(icon);
        iv.setTextSize(22);
        btn.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFFCCCCCC);
        tv.setTextSize(12);
        btn.addView(tv);

        btn.setOnClickListener(v -> {
            switch (label) {
                case "设置": startActivity(new Intent(MainActivity.this, SettingsActivity.class)); break;
                case "日志": startActivity(new Intent(MainActivity.this, StatsActivity.class)); break;
                case "重置": config.resetStats(); doRefresh(); break;
            }
        });

        return btn;
    }

    private void toggle() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (s != null && s.isActive()) {
            s.stopGrabLoop();
            startTime = 0;
        } else {
            if (!isAccEnabled()) {
                openAccessibilitySettings();
                return;
            }
            if (s == null) return;
            s.startGrabLoop();
            startTime = SystemClock.elapsedRealtime();
        }
        doUpdateUI();
    }

    private void pause() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (s == null) return;
        s.setPaused(!s.isPaused());
        doUpdateUI();
    }

    private boolean isAccEnabled() {
        try {
            String a = getPackageName() + "/" + GrabAccessibilityService.class.getCanonicalName();
            String e = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return e != null && e.contains(a);
        } catch (Exception ex) { return false; }
    }

    private void openAccessibilitySettings() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception ignored) {}
    }

    private void doUpdateUI() {
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        boolean r = s != null && s.isActive();
        boolean p = s != null && s.isPaused();
        btnToggle.setText(r ? "停止服务" : "启动服务");
        btnToggle.setBackgroundColor(r ? 0xFF333355 : 0xFFE94560);
        btnPause.setVisibility(r ? View.VISIBLE : View.GONE);
        if (r) btnPause.setText(p ? "继续" : "暂停");
        if (r && p) { tvShortStatus.setText("已暂停"); statusDot.setBackgroundColor(0xFFFFA500); }
        else if (r) { tvShortStatus.setText("运行中"); statusDot.setBackgroundColor(0xFF4CAF50); }
        else { tvShortStatus.setText("未启动"); statusDot.setBackgroundColor(0xFF888888); }
    }

    private void doRefresh() {
        int g = config.getStatGrabbed(), f = config.getStatFailed(), sk = config.getStatSkipped();
        tvGrabbed.setText(String.valueOf(g));
        tvFailed.setText(String.valueOf(f));
        tvSkipped.setText(String.valueOf(sk));
        tvIncome.setText("¥" + (g * 12));
        if (startTime > 0) {
            long e = SystemClock.elapsedRealtime() - startTime;
            int sec = (int)(e/1000);
            tvUptime.setText(String.format("%02d:%02d:%02d", sec/3600, (sec/60)%60, sec%60));
        } else tvUptime.setText("--:--:--");
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

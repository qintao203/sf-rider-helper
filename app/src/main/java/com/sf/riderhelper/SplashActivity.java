package com.sf.riderhelper;

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
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        makeFullScreen();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF0A0A14);

        // Logo占位
        TextView icon = new TextView(this);
        icon.setText("📦");
        icon.setTextSize(56);
        icon.setPadding(0, 0, 0, dp(12));
        root.addView(icon);

        TextView appName = new TextView(this);
        appName.setText("顺丰抢单助手");
        appName.setTextColor(0xFFFFFFFF);
        appName.setTextSize(22);
        appName.setTypeface(null, 1);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, 0, 0, dp(6));
        root.addView(appName);

        TextView sub = new TextView(this);
        sub.setText("SF Rider Helper");
        sub.setTextColor(0xFF555570);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView loading = new TextView(this);
        loading.setText("加载中...");
        loading.setTextColor(0xFF3A3A55);
        loading.setTextSize(12);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(48), 0, 0);
        root.addView(loading);

        setContentView(root);

        new Handler(getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1200);
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
}

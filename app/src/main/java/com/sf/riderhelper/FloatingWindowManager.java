package com.sf.riderhelper;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 闭环联动悬浮窗
 * 运行在顺丰同城骑士APP之上，实现"同屏协作"效果
 * 可拖动 + 抢单控制 + 顺丰同城一键切换
 */
public class FloatingWindowManager {
    private final Context ctx;
    private final WindowManager wm;
    private View floatView;
    private TextView tvStatus, tvOrder, tvStats;
    private Button btnSf, btnPause, btnMain;
    private boolean showing = false;
    private boolean isPaused = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 拖动
    private float startX, startY;

    public FloatingWindowManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        wm = (WindowManager) this.ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        if (showing) return;
        try {
            LinearLayout root = new LinearLayout(ctx);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(0xE50A0A18);

            // === 标题栏（可拖动） ===
            LinearLayout titleBar = new LinearLayout(ctx);
            titleBar.setOrientation(LinearLayout.HORIZONTAL);
            titleBar.setGravity(Gravity.CENTER_VERTICAL);
            titleBar.setPadding(10, 6, 10, 4);
            titleBar.setBackgroundColor(0x33151525);

            // 抢单状态
            tvStatus = new TextView(ctx);
            tvStatus.setText("⚡ 抢单中");
            tvStatus.setTextColor(0xFF4CAF50);
            tvStatus.setTextSize(11);
            tvStatus.setTypeface(null, 1);
            titleBar.addView(tvStatus);

            // 撑开
            TextView sp1 = new TextView(ctx);
            sp1.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            titleBar.addView(sp1);

            // 统计简讯
            tvStats = new TextView(ctx);
            tvStats.setText("0单");
            tvStats.setTextColor(0xFF9E9EB8);
            tvStats.setTextSize(10);
            tvStats.setPadding(0, 0, 6, 0);
            titleBar.addView(tvStats);

            root.addView(titleBar);

            // === 订单信息行 ===
            tvOrder = new TextView(ctx);
            tvOrder.setText("等待订单...");
            tvOrder.setTextColor(0xFF9E9EB8);
            tvOrder.setTextSize(10);
            tvOrder.setPadding(12, 4, 12, 4);
            root.addView(tvOrder);

            // === 控制按钮行 ===
            LinearLayout btnRow = new LinearLayout(ctx);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(6, 2, 6, 6);

            // 顺丰同城快捷按钮（核心闭环入口）
            btnSf = new Button(ctx);
            btnSf.setText("📦 顺丰同城");
            btnSf.setTextColor(0xFFFFFFFF);
            btnSf.setTextSize(10);
            btnSf.setTypeface(null, 1);
            btnSf.setBackgroundColor(0xFF00BCD4);
            btnSf.setPadding(8, 4, 8, 4);
            btnSf.setLayoutParams(new LinearLayout.LayoutParams(0, 32, 1));
            btnSf.setOnClickListener(v -> {
                if (!SFRiderBridge.launchSFApp(ctx)) {
                    SFRiderBridge.openMarket(ctx);
                }
            });
            btnRow.addView(btnSf);

            // 暂停/继续按钮
            btnPause = new Button(ctx);
            btnPause.setText("⏸");
            btnPause.setTextColor(0xFFFFFFFF);
            btnPause.setTextSize(10);
            btnPause.setBackgroundColor(0xFFFF9800);
            btnPause.setPadding(4, 4, 4, 4);
            btnPause.setLayoutParams(new LinearLayout.LayoutParams(-2, 32));
            btnPause.setOnClickListener(v -> togglePause());
            btnRow.addView(btnPause);

            // 返回主控
            btnMain = new Button(ctx);
            btnMain.setText("≡");
            btnMain.setTextColor(0xFFFFFFFF);
            btnMain.setTextSize(14);
            btnMain.setBackgroundColor(0xFF555570);
            btnMain.setPadding(4, 4, 4, 4);
            btnMain.setLayoutParams(new LinearLayout.LayoutParams(-2, 32));
            btnMain.setOnClickListener(v -> {
                // 回到我们的主控APP
                Intent intent = ctx.getPackageManager()
                        .getLaunchIntentForPackage(ctx.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    ctx.startActivity(intent);
                }
            });
            btnRow.addView(btnMain);

            root.addView(btnRow);

            // === 触摸拖动 ===
            root.setOnTouchListener(new View.OnTouchListener() {
                private float initX, initY;
                private long touchTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX();
                            startY = event.getRawY();
                            if (floatView != null) {
                                initX = ((WindowManager.LayoutParams) floatView.getLayoutParams()).x;
                                initY = ((WindowManager.LayoutParams) floatView.getLayoutParams()).y;
                            }
                            touchTime = System.currentTimeMillis();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (floatView != null) {
                                try {
                                    WindowManager.LayoutParams p =
                                            (WindowManager.LayoutParams) floatView.getLayoutParams();
                                    p.x = (int) (initX + (event.getRawX() - startX));
                                    p.y = (int) (initY + (event.getRawY() - startY));
                                    wm.updateViewLayout(floatView, p);
                                } catch (Exception ignored) {}
                            }
                            return true;
                    }
                    return false;
                }
            });

            // 窗口参数
            int type = Build.VERSION.SDK_INT >= 26 ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    260, -2, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 10;
            params.y = 120;

            wm.addView(root, params);
            floatView = root;
            showing = true;

            // 更新顺丰安装状态
            updateSFState();
            updateStats(0, 0, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hide() {
        if (!showing || floatView == null) return;
        try { wm.removeView(floatView); } catch (Exception ignored) {}
        showing = false;
        floatView = null;
    }

    public boolean isShowing() { return showing; }

    /** 更新抢单状态 */
    public void updateStatus(String text, int color) {
        if (tvStatus != null) {
            handler.post(() -> {
                tvStatus.setText(text);
                tvStatus.setTextColor(color);
            });
        }
    }

    /** 更新订单信息 */
    public void updateOrderInfo(String info) {
        if (tvOrder != null) {
            handler.post(() -> tvOrder.setText(info != null ? info : "等待订单..."));
        }
    }

    /** 更新统计 */
    public void updateStats(int grabbed, int failed, float income) {
        if (tvStats != null) {
            handler.post(() ->
                tvStats.setText(String.format("%d单 ¥%.0f", grabbed, income)));
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        GrabAccessibilityService s = GrabAccessibilityService.getInstance();
        if (s != null) {
            s.setPaused(isPaused);
        }
        btnPause.setText(isPaused ? "▶" : "⏸");
        btnPause.setBackgroundColor(isPaused ? 0xFF4CAF50 : 0xFFFF9800);
        tvStatus.setText(isPaused ? "⏸ 已暂停" : "⚡ 抢单中");
        tvStatus.setTextColor(isPaused ? 0xFFFF9800 : 0xFF4CAF50);
    }

    private void updateSFState() {
        if (btnSf != null) {
            boolean installed = SFRiderBridge.isInstalled(ctx);
            btnSf.setText(installed ? "📦 顺丰同城" : "📥 安装顺丰");
            btnSf.setAlpha(installed ? 1f : 0.7f);
        }
    }
}

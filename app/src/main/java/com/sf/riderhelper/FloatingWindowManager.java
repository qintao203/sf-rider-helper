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
 * 增强悬浮窗：可拖动 + 抢单信息 + 顺丰同城快捷入口
 */
public class FloatingWindowManager {
    private final Context ctx;
    private final WindowManager wm;
    private View floatView;
    private TextView tvStatus, tvOrder;
    private boolean showing = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 拖动相关
    private float touchX, touchY, initialX, initialY;

    public FloatingWindowManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        wm = (WindowManager) this.ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        if (showing) return;
        try {
            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundColor(0xE50A0A18);
            layout.setPadding(10, 6, 10, 6);
            layout.setAlpha(0.92f);

            // 第一行：状态 + 图标
            LinearLayout topRow = new LinearLayout(ctx);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            tvStatus = new TextView(ctx);
            tvStatus.setText("⚡ 运行中");
            tvStatus.setTextColor(0xFF4CAF50);
            tvStatus.setTextSize(11);
            tvStatus.setTypeface(null, 1);
            topRow.addView(tvStatus);

            // 占位
            TextView spacer = new TextView(ctx);
            spacer.setText("  ");
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            topRow.addView(spacer);

            // 顺丰同城快捷按钮
            Button sfBtn = new Button(ctx);
            sfBtn.setText("📦");
            sfBtn.setTextSize(10);
            sfBtn.setBackgroundColor(0x2200E5FF);
            sfBtn.setPadding(6, 2, 6, 2);
            sfBtn.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            sfBtn.setOnClickListener(v -> SFRiderBridge.launchSFApp(ctx));
            topRow.addView(sfBtn);

            layout.addView(topRow);

            // 第二行：订单信息
            tvOrder = new TextView(ctx);
            tvOrder.setText("等待订单...");
            tvOrder.setTextColor(0xFF9E9EB8);
            tvOrder.setTextSize(10);
            tvOrder.setPadding(0, 2, 0, 0);
            layout.addView(tvOrder);

            // 触摸拖动（防null检查）
            layout.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (floatView != null) {
                            try {
                                WindowManager.LayoutParams p =
                                        (WindowManager.LayoutParams) floatView.getLayoutParams();
                                p.x = (int)((event.getRawX() - touchX));
                                p.y = (int)((event.getRawY() - touchY));
                                wm.updateViewLayout(floatView, p);
                            } catch (Exception ignored) {}
                        }
                        return true;
                }
                return false;
            });

            int type;
            if (Build.VERSION.SDK_INT >= 26) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 10;
            params.y = 120;

            wm.addView(layout, params);
            floatView = layout;
            showing = true;

            // 更新SF安装状态
            updateSFButton(sfBtn);

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

    public void updateStatus(String text, int color) {
        if (tvStatus != null) {
            handler.post(() -> {
                tvStatus.setText(text);
                tvStatus.setTextColor(color);
            });
        }
    }

    public void updateOrderInfo(String info) {
        if (tvOrder != null) {
            handler.post(() -> tvOrder.setText(info != null ? info : "等待订单..."));
        }
    }

    private void updateSFButton(Button btn) {
        if (SFRiderBridge.isInstalled(ctx)) {
            btn.setAlpha(1f);
        } else {
            btn.setAlpha(0.4f);
        }
    }

    public boolean isShowing() { return showing; }
}

package com.sf.riderhelper;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingWindowManager {
    private final Context ctx;
    private final WindowManager wm;
    private View floatView;
    private TextView tvShort;
    private boolean showing = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    public FloatingWindowManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        wm = (WindowManager) this.ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        if (showing) return;
        try {
            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setBackgroundColor(0xCC000000);
            layout.setPadding(12, 6, 12, 6);
            layout.setAlpha(0.85f);

            tvShort = new TextView(ctx);
            tvShort.setText("抢单助手 运行中");
            tvShort.setTextColor(0xFF4CAF50);
            tvShort.setTextSize(12);
            layout.addView(tvShort);

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
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 10;
            params.y = 100;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;

            wm.addView(layout, params);
            floatView = layout;
            showing = true;
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
        if (tvShort != null) {
            handler.post(() -> {
                tvShort.setText(text);
                tvShort.setTextColor(color);
            });
        }
    }

    public boolean isShowing() { return showing; }
}

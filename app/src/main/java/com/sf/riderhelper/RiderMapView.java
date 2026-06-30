package com.sf.riderhelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 内置地图组件（纯Canvas绘制）
 * 用于显示订单分布、骑手位置、配送路线
 * 当高德SDK集成后可替换为原生地图
 */
public class RiderMapView extends View {

    private Paint routePaint, markerPaint, textPaint, gridPaint, dotPaint;
    private float density;
    private float centerX, centerY;
    private float scale = 1.0f;
    private float lastTouchDist = 0;

    // 模拟数据
    private List<PointF> markers = new ArrayList<>();
    private List<String> markerLabels = new ArrayList<>();
    private List<Integer> markerColors = new ArrayList<>();
    private PointF riderPos;
    private List<PointF> route = new ArrayList<>();

    // 捏合缩放
    private float oldX1, oldY1, oldX2, oldY2;
    private boolean isPinching = false;

    public RiderMapView(Context ctx) {
        super(ctx);
        density = ctx.getResources().getDisplayMetrics().density;
        initPaints();
        generateMockData();
    }

    private void initPaints() {
        routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        routePaint.setStyle(Paint.Style.STROKE);
        routePaint.setStrokeWidth(dp(3));
        routePaint.setColor(0xFF00E5FF);

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(dp(10));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(0.5f));
        gridPaint.setColor(0x1AFFFFFF);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    private void generateMockData() {
        Random rng = new Random(42);
        centerX = dp(150);
        centerY = dp(200);

        // 骑手位置
        riderPos = new PointF(centerX + dp(10), centerY + dp(5));

        // 订单标记
        String[][] mockPOIs = {
            {"老娘舅", "¥25", "科技园A座"},
            {"麦当劳", "¥18", "深南大道"},
            {"华润万家", "¥32", "科技路"},
            {"星巴克", "¥15", "高新南"},
            {"必胜客", "¥38", "科技中"},
        };
        int[] colors = {
            0xFFFF3366, 0xFFFF9800, 0xFF4CAF50,
            0xFF00E5FF, 0xFFBB86FC
        };

        for (int i = 0; i < mockPOIs.length; i++) {
            float x = centerX + (rng.nextFloat() - 0.5f) * dp(200);
            float y = centerY + (rng.nextFloat() - 0.5f) * dp(150);
            markers.add(new PointF(x, y));
            markerLabels.add(mockPOIs[i][0] + "\n" + mockPOIs[i][1]);
            markerColors.add(colors[i]);
        }

        // 配送路线
        route.add(new PointF(centerX - dp(60), centerY - dp(30)));
        route.add(new PointF(centerX - dp(20), centerY - dp(10)));
        route.add(new PointF(centerX + dp(20), centerY + dp(20)));
        route.add(new PointF(centerX + dp(80), centerY + dp(50)));
        route.add(new PointF(centerX + dp(120), centerY + dp(70)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();

        // 背景网格（模拟地图）
        for (float x = 0; x < w; x += dp(40)) {
            canvas.drawLine(x, 0, x, h, gridPaint);
        }
        for (float y = 0; y < h; y += dp(40)) {
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        // 道路线（模拟路网）
        Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setStrokeWidth(dp(6));
        roadPaint.setColor(0x2AFFFFFF);
        canvas.drawLine(dp(30), dp(50), dp(300), dp(100), roadPaint);
        canvas.drawLine(dp(150), dp(30), dp(120), dp(350), roadPaint);
        canvas.drawLine(dp(200), dp(50), dp(250), dp(350), roadPaint);

        // 配送路线
        if (route.size() > 1) {
            Path path = new Path();
            path.moveTo(route.get(0).x, route.get(0).y);
            for (int i = 1; i < route.size(); i++) {
                path.lineTo(route.get(i).x, route.get(i).y);
            }
            canvas.drawPath(path, routePaint);

            // 路线箭头
            routePaint.setColor(0xFF00E5FF);
            routePaint.setStrokeWidth(dp(1));
            for (int i = 1; i < route.size(); i++) {
                PointF p1 = route.get(i - 1);
                PointF p2 = route.get(i);
                float dx = p2.x - p1.x;
                float dy = p2.y - p1.y;
                float angle = (float) Math.atan2(dy, dx);
                float size = dp(6);
                canvas.drawLine(p2.x, p2.y,
                        p2.x - size * (float) Math.cos(angle - 0.5f),
                        p2.y - size * (float) Math.sin(angle - 0.5f), routePaint);
                canvas.drawLine(p2.x, p2.y,
                        p2.x - size * (float) Math.cos(angle + 0.5f),
                        p2.y - size * (float) Math.sin(angle + 0.5f), routePaint);
            }
        }

        // 订单标记
        for (int i = 0; i < markers.size(); i++) {
            PointF m = markers.get(i);
            int color = markerColors.get(i);

            // 光晕
            markerPaint.setColor(color & 0x44FFFFFF);
            canvas.drawCircle(m.x, m.y, dp(24), markerPaint);

            // 标记圆
            markerPaint.setColor(color);
            canvas.drawCircle(m.x, m.y, dp(10), markerPaint);

            // 标记外圈
            markerPaint.setColor(Color.TRANSPARENT);
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(2));
            border.setColor(color);
            canvas.drawCircle(m.x, m.y, dp(10), border);

            // 标签
            textPaint.setTextSize(dp(9));
            String label = markerLabels.get(i);
            String[] lines = label.split("\n");
            for (int j = 0; j < lines.length; j++) {
                float tw = textPaint.measureText(lines[j]);
                canvas.drawText(lines[j], m.x - tw / 2,
                        m.y - dp(16) + j * dp(12), textPaint);
            }
        }

        // 骑手位置（当前位置）
        if (riderPos != null) {
            // 脉冲光晕
            markerPaint.setColor(0x2200E5FF);
            float pulseR = dp(18) + (float) Math.sin(System.currentTimeMillis() / 500.0) * dp(4);
            canvas.drawCircle(riderPos.x, riderPos.y, pulseR, markerPaint);

            // 蓝色定位点
            markerPaint.setColor(0xFF00E5FF);
            canvas.drawCircle(riderPos.x, riderPos.y, dp(8), markerPaint);

            // 白色中心点
            markerPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(riderPos.x, riderPos.y, dp(3), markerPaint);

            // "我的位置"标签
            textPaint.setTextSize(dp(10));
            textPaint.setColor(0xFF00E5FF);
            String riderLabel = "📍 当前位置";
            canvas.drawText(riderLabel, riderPos.x - textPaint.measureText(riderLabel) / 2,
                    riderPos.y + dp(18), textPaint);
        }

        // 右下角缩放提示
        textPaint.setTextSize(dp(9));
        textPaint.setColor(0xFF3A3A55);
        String zoom = "缩放: " + String.format("%.1fx", scale);
        canvas.drawText(zoom, w - textPaint.measureText(zoom) - dp(8), h - dp(8), textPaint);

        // 地图信息
        textPaint.setColor(0xFF3A3A55);
        canvas.drawText("科技园·5个订单·配送路线", dp(8), dp(18), textPaint);

        // 请求重绘（动画循环）
        postInvalidateDelayed(500);
    }

    // ========== 工具 ==========

    private int dp(float n) { return (int)(n * density + 0.5f); }

    /** 更新骑手位置（外部调用） */
    public void updateRiderPosition(float x, float y) {
        if (riderPos != null) {
            riderPos.x = centerX + x;
            riderPos.y = centerY + y;
            invalidate();
        }
    }

    /** 添加订单标记 */
    public void addMarker(float x, float y, String label, int color) {
        markers.add(new PointF(centerX + x, centerY + y));
        markerLabels.add(label);
        markerColors.add(color);
        invalidate();
    }
}

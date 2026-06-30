package com.sf.riderhelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

/**
 * 自定义Canvas柱状图/折线图 — 收入趋势可视化
 */
public class ChartView extends View {

    private List<Float> data;
    private int chartColor = 0xFF00E5FF;
    private String title = "";
    private String unit = "元";

    private Paint barPaint, linePaint, textPaint, gridPaint, fillPaint;
    private float density;

    public ChartView(Context ctx) {
        super(ctx);
        density = ctx.getResources().getDisplayMetrics().density;
        initPaints();
    }

    private void initPaints() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(chartColor);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setColor(0xFFBB86FC);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF9E9EB8);
        textPaint.setTextSize(dp(9));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(0.5f));
        gridPaint.setColor(0x1AFFFFFF);
    }

    public void setData(List<Float> data) { this.data = data; invalidate(); }
    public void setChartColor(int c) { chartColor = c; barPaint.setColor(c); invalidate(); }
    public void setTitle(String t) { title = t; }
    public void setUnit(String u) { unit = u; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        float w = getWidth() - dp(40);
        float h = getHeight() - dp(50);
        float left = dp(32);
        float top = dp(20);

        // 画标题
        if (!title.isEmpty()) {
            textPaint.setTextSize(dp(11));
            textPaint.setColor(0xFF555570);
            canvas.drawText(title, dp(32), dp(14), textPaint);
            textPaint.setTextSize(dp(9));
        }

        // 画网格线
        for (int i = 0; i <= 4; i++) {
            float y = top + h * i / 4;
            canvas.drawLine(left, y, left + w, y, gridPaint);
        }

        // 找最大值
        float maxVal = 0;
        for (float v : data) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        int n = data.size();
        float barW = Math.min(dp(20), w / n * 0.6f);
        float gap = w / n;

        // 画柱状图
        for (int i = 0; i < n; i++) {
            float val = data.get(i);
            float barH = (val / maxVal) * h;
            float cx = left + gap * i + gap / 2;

            // 柱子
            RectF bar = new RectF(cx - barW / 2, top + h - barH, cx + barW / 2, top + h);
            canvas.drawRoundRect(bar, dp(2), dp(2), barPaint);

            // 数值
            if (val > 0) {
                textPaint.setColor(0xFF9E9EB8);
                String label = val == (int)val ? String.valueOf((int)val) : String.format("%.0f", val);
                float tw = textPaint.measureText(label);
                canvas.drawText(label, cx - tw / 2, top + h - barH - dp(3), textPaint);
            }
        }

        // 画折线（趋势线）
        Path path = new Path();
        boolean first = true;
        for (int i = 0; i < n; i++) {
            float val = data.get(i);
            float barH = (val / maxVal) * h;
            float cx = left + gap * i + gap / 2;
            float cy = top + h - barH;
            if (first) { path.moveTo(cx, cy); first = false; }
            else path.lineTo(cx, cy);
        }
        canvas.drawPath(path, linePaint);

        // 底部标签
        textPaint.setColor(0xFF3A3A55);
        for (int i = 0; i < n; i++) {
            float cx = left + gap * i + gap / 2;
            String lbl = String.valueOf(i + 1);
            float tw = textPaint.measureText(lbl);
            canvas.drawText(lbl, cx - tw / 2, top + h + dp(14), textPaint);
        }
    }

    private void drawEmptyState(Canvas canvas) {
        textPaint.setColor(0xFF3A3A55);
        textPaint.setTextSize(dp(12));
        String msg = "暂无数据";
        float tw = textPaint.measureText(msg);
        canvas.drawText(msg, (getWidth() - tw) / 2, getHeight() / 2, textPaint);
    }

    private int dp(float n) { return (int)(n * density + 0.5f); }
}

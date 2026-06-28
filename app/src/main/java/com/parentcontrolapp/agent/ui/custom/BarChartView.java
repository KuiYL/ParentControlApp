package com.parentcontrolapp.agent.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<BarData> bars = new ArrayList<>();
    private float barWidth;
    private float barSpacing;
    private float chartHeight;
    private int maxValue = 1;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.parseColor("#616161"));
        textPaint.setTextSize(13 * getResources().getDisplayMetrics().density);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        valuePaint.setStyle(Paint.Style.FILL);
        valuePaint.setColor(Color.parseColor("#333333"));
        valuePaint.setTextSize(12 * getResources().getDisplayMetrics().density);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.DEFAULT_BOLD);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(1);

        emptyPaint.setColor(Color.parseColor("#F5F5F5"));
    }

    public void setData(List<BarData> bars) {
        this.bars = bars;

        maxValue = 1;
        for (BarData bar : bars) {
            if (bar.value > maxValue) {
                maxValue = bar.value;
            }
        }

        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int barCount = bars.size();
        if (barCount > 0) {
            float totalWidth = w - 40;
            barWidth = totalWidth / (barCount * 1.5f);
            barSpacing = barWidth * 0.5f;
        }
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int availableHeight = h - paddingTop - paddingBottom;
        chartHeight = availableHeight - 100;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (bars.isEmpty()) {
            emptyPaint.setColor(Color.parseColor("#F5F5F5"));
            int paddingTop = getPaddingTop();
            canvas.drawRoundRect(new RectF(20, paddingTop + 20, getWidth() - 20, paddingTop + chartHeight + 20), 12, 12, emptyPaint);
            return;
        }

        float startX = 20;
        int paddingTop = getPaddingTop();
        float baseline = paddingTop + chartHeight + 20;

        for (int i = 0; i < bars.size(); i++) {
            BarData bar = bars.get(i);

            float barHeight = (bar.value / (float) maxValue) * chartHeight;
            if (barHeight < 4) barHeight = 4;

            float left = startX + (i * (barWidth + barSpacing));
            float top = baseline - barHeight;
            float right = left + barWidth;
            barPaint.setColor(bar.color);
            RectF barRect = new RectF(left, top, right, baseline);
            float cornerRadius = Math.min(barWidth / 2, 8);
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);

            if (bar.value > 0) {
                String valueText = bar.value + "м";
                canvas.drawText(valueText, left + barWidth / 2, top - 12, valuePaint);
            }
            canvas.drawText(bar.label, left + barWidth / 2, baseline + 32, textPaint);
        }
        drawGridLines(canvas, baseline);
    }

    private void drawGridLines(Canvas canvas, float baseline) {
        int lines = 3;
        for (int i = 1; i < lines; i++) {
            float y = baseline - (chartHeight * i / lines);
            canvas.drawLine(20, y, getWidth() - 20, y, gridPaint);
        }
    }

    public static class BarData {
        public String label;
        public int value;
        public int color;

        public BarData(String label, int value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }
}
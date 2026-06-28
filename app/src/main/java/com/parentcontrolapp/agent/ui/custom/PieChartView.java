package com.parentcontrolapp.agent.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private List<PieSlice> slices = new ArrayList<>();
    private float centerX, centerY, radius;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<PieSlice> slices) {
        this.slices = slices;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f - 10;
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (slices == null || slices.isEmpty()) {
            paint.setColor(Color.LTGRAY);
            canvas.drawCircle(centerX, centerY, radius, paint);
            return;
        }

        float startAngle = -90;
        for (PieSlice slice : slices) {
            paint.setColor(slice.color);
            float sweepAngle = slice.percentage * 360f;
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }

        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius * 0.6f, paint);
    }

    public static class PieSlice {
        public String label;
        public float percentage;
        public int color;

        public PieSlice(String label, float percentage, int color) {
            this.label = label;
            this.percentage = percentage;
            this.color = color;
        }
    }
}
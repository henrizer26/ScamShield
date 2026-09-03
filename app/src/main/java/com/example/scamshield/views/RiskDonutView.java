package com.example.scamshield.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.scamshield.R;

/**
 * The "Risk Overview" ring: one arc per risk bucket around a total.
 *
 * <p>Drawn rather than pulled from a chart library so it follows the app's own
 * palette in both themes and costs nothing at build time.
 */
public class RiskDonutView extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint totalPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private int high;
    private int medium;
    private int low;

    private String totalText = "0";
    private String labelText = "";

    public RiskDonutView(Context context) {
        this(context, null);
    }

    public RiskDonutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RiskDonutView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dp(18));

        totalPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        totalPaint.setTextSize(sp(22));
        totalPaint.setFakeBoldText(true);
        totalPaint.setTextAlign(Paint.Align.CENTER);

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextSize(sp(12));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        labelText = context.getString(R.string.donut_total);
    }

    /** Counts per bucket; the ring and the centre total both follow. */
    public void setCounts(int high, int medium, int low) {
        this.high = Math.max(0, high);
        this.medium = Math.max(0, medium);
        this.low = Math.max(0, low);
        this.totalText = String.valueOf(this.high + this.medium + this.low);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float inset = arcPaint.getStrokeWidth() / 2f + dp(2);
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);

        int total = high + medium + low;
        if (total == 0) {
            arcPaint.setColor(ContextCompat.getColor(getContext(), R.color.bg_muted));
            canvas.drawArc(bounds, 0, 360, false, arcPaint);
        } else {
            // Starts at the top and runs clockwise: high, medium, low.
            float start = -90f;
            start = drawSlice(canvas, start, high / (float) total, R.color.risk_high);
            start = drawSlice(canvas, start, medium / (float) total, R.color.risk_medium);
            drawSlice(canvas, start, low / (float) total, R.color.risk_low);
        }

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        canvas.drawText(totalText, centerX, centerY + dp(2), totalPaint);
        canvas.drawText(labelText, centerX, centerY + dp(20), labelPaint);
    }

    private float drawSlice(Canvas canvas, float startAngle, float fraction, int colorRes) {
        float sweep = fraction * 360f;
        if (sweep <= 0) {
            return startAngle;
        }
        arcPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
        canvas.drawArc(bounds, startAngle, sweep, false, arcPaint);
        return startAngle + sweep;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}

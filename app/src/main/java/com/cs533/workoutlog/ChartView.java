package com.cs533.workoutlog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Line, vertical bar and horizontal bar charts drawn with Canvas (FR-23). No chart
 * library. Sizes are in dp/sp and computed in onDraw; colors come from colors.xml.
 * The owner sets a contentDescription that summarizes the data for TalkBack.
 */
public class ChartView extends View {
    public static final int LINE = 0;
    public static final int BARS = 1;
    public static final int HORIZONTAL_BARS = 2;

    private final float dp;
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint valuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint emptyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int type = BARS;
    private List<Stats.Point> points = new ArrayList<>();
    private int minPoints = 1;
    private String emptyText = "";

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        float sp = getResources().getDisplayMetrics().scaledDensity;
        linePaint.setColor(context.getColor(R.color.chart_line));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * dp);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        barPaint.setColor(context.getColor(R.color.chart_bar));
        gridPaint.setColor(context.getColor(R.color.chart_grid));
        gridPaint.setStrokeWidth(1 * dp);
        axisPaint.setColor(context.getColor(R.color.chart_axis));
        axisPaint.setStrokeWidth(1.5f * dp);
        dotHolePaint.setColor(context.getColor(R.color.card_background));
        labelPaint.setColor(context.getColor(R.color.chart_label));
        labelPaint.setTextSize(12 * sp);
        valuePaint.setColor(context.getColor(R.color.chart_value));
        valuePaint.setTextSize(12 * sp);
        valuePaint.setFakeBoldText(true);
        emptyPaint.setColor(context.getColor(R.color.text_secondary));
        emptyPaint.setTextSize(14 * sp);
    }

    /**
     * @param minPoints the fewest points that make a meaningful chart; with fewer, or when
     *                  every bar is zero, the empty text is drawn instead
     */
    public void setData(int type, List<Stats.Point> points, int minPoints, String emptyText, String description) {
        this.type = type;
        this.points = new ArrayList<>(points);
        this.minPoints = minPoints;
        this.emptyText = emptyText;
        setContentDescription(hasData() ? description : emptyText);
        requestLayout();
        invalidate();
    }

    public boolean hasData() {
        if (points.size() < Math.max(1, minPoints)) {
            return false;
        }
        if (type == LINE) {
            return true;
        }
        for (Stats.Point p : points) {
            if (p.value > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        float desired;
        if (!hasData()) {
            desired = 88 * dp;
        } else if (type == HORIZONTAL_BARS) {
            desired = points.size() * rowHeight() + 36 * dp;
        } else {
            desired = 220 * dp;
        }
        setMeasuredDimension(width, resolveSize((int) desired, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!hasData()) {
            drawEmpty(canvas);
        } else if (type == HORIZONTAL_BARS) {
            drawHorizontalBars(canvas);
        } else {
            drawVertical(canvas);
        }
    }

    private float rowHeight() {
        return 40 * dp;
    }

    private void drawEmpty(Canvas canvas) {
        int width = Math.max(1, getWidth() - (int) (16 * dp));
        StaticLayout layout = StaticLayout.Builder.obtain(emptyText, 0, emptyText.length(), emptyPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .build();
        canvas.save();
        canvas.translate((getWidth() - width) / 2f, (getHeight() - layout.getHeight()) / 2f);
        layout.draw(canvas);
        canvas.restore();
    }

    /** Line chart and vertical bar chart: value axis on the left, one slot per point along the bottom. */
    private void drawVertical(Canvas canvas) {
        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (Stats.Point p : points) {
            lo = Math.min(lo, p.value);
            hi = Math.max(hi, p.value);
        }
        double min;
        double max;
        double step;
        if (type == BARS) {
            step = Math.max(1, niceStep(hi, 4));
            min = 0;
            max = Math.ceil(hi / step) * step;
        } else {
            double range = hi - lo;
            step = Math.max(1, niceStep(range > 0 ? range : Math.max(1, hi * 0.1), 4));
            min = Math.max(0, Math.floor(lo / step) * step);
            max = Math.ceil(hi / step) * step;
            if (max <= min) {
                max = min + step;
            }
        }

        float textHeight = labelPaint.getTextSize();
        float yLabelWidth = 0;
        for (double v = min; v <= max + step / 2; v += step) {
            yLabelWidth = Math.max(yLabelWidth, labelPaint.measureText(compact(v)));
        }
        float left = getPaddingLeft() + yLabelWidth + 8 * dp;
        float right = getWidth() - getPaddingRight() - 8 * dp;
        float top = getPaddingTop() + textHeight + 8 * dp;
        float bottom = getHeight() - getPaddingBottom() - textHeight - 10 * dp;

        // Gridlines at rounded steps, labelled on the left. Labels skip if they would touch.
        float lastLabelY = Float.MAX_VALUE;
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (double v = min; v <= max + step / 2; v += step) {
            float y = map(v, min, max, bottom, top);
            canvas.drawLine(left, y, right, y, gridPaint);
            if (lastLabelY - y >= textHeight * 1.2f) {
                canvas.drawText(compact(v), left - 6 * dp, y + textHeight / 3, labelPaint);
                lastLabelY = y;
            }
        }
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        int n = points.size();
        float slot = (right - left) / n;
        float[] xs = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = type == BARS || n == 1 ? left + slot * (i + 0.5f) : left + (right - left) * i / (n - 1);
        }
        drawXLabels(canvas, xs, type == BARS ? slot : (n > 1 ? (right - left) / (n - 1) : right - left), bottom + textHeight + 6 * dp);

        valuePaint.setTextAlign(Paint.Align.CENTER);
        if (type == BARS) {
            float barWidth = Math.min(slot * 0.62f, 40 * dp);
            for (int i = 0; i < n; i++) {
                double v = points.get(i).value;
                if (v <= 0) {
                    continue;
                }
                float y = map(v, min, max, bottom, top);
                rect.set(xs[i] - barWidth / 2, y, xs[i] + barWidth / 2, bottom);
                canvas.drawRoundRect(rect, 3 * dp, 3 * dp, barPaint);
                canvas.drawText(compact(v), xs[i], y - 4 * dp, valuePaint);
            }
        } else {
            path.reset();
            for (int i = 0; i < n; i++) {
                float y = map(points.get(i).value, min, max, bottom, top);
                if (i == 0) {
                    path.moveTo(xs[i], y);
                } else {
                    path.lineTo(xs[i], y);
                }
            }
            canvas.drawPath(path, linePaint);
            float radius = (n > 40 ? 2.5f : 4f) * dp;
            linePaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < n; i++) {
                float y = map(points.get(i).value, min, max, bottom, top);
                canvas.drawCircle(xs[i], y, radius, linePaint);
                if (n <= 40) {
                    canvas.drawCircle(xs[i], y, radius * 0.45f, dotHolePaint);
                }
            }
            linePaint.setStyle(Paint.Style.STROKE);
            // Label the latest value so the current level is readable without TalkBack.
            Stats.Point lastPoint = points.get(n - 1);
            String text = compact(lastPoint.value);
            float lastY = map(lastPoint.value, min, max, bottom, top);
            float half = valuePaint.measureText(text) / 2;
            float x = Math.min(Math.max(xs[n - 1], left + half), getWidth() - getPaddingRight() - half);
            canvas.drawText(text, x, Math.max(lastY - 9 * dp, textHeight), valuePaint);
        }
    }

    /** Draws labels centred under each x, skipping evenly from the newest so none overlap. */
    private void drawXLabels(Canvas canvas, float[] xs, float spacing, float baseline) {
        float widest = 0;
        for (Stats.Point p : points) {
            widest = Math.max(widest, labelPaint.measureText(p.label));
        }
        int every = Math.max(1, (int) Math.ceil((widest + 8 * dp) / Math.max(1, spacing)));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        float minX = getPaddingLeft();
        float maxX = getWidth() - getPaddingRight();
        int n = points.size();
        float nextLeft = Float.MAX_VALUE;
        for (int i = n - 1; i >= 0; i -= every) {
            String label = points.get(i).label;
            float half = labelPaint.measureText(label) / 2;
            float x = Math.min(Math.max(xs[i], minX + half), maxX - half);
            // Clamping at the edges can pull a label towards its neighbour; skip it rather than overlap.
            if (x + half > nextLeft - 6 * dp) {
                continue;
            }
            canvas.drawText(label, x, baseline, labelPaint);
            nextLeft = x - half;
        }
    }

    /** Horizontal bars: names on the left, value at the end of each bar, value gridlines along the bottom. */
    private void drawHorizontalBars(Canvas canvas) {
        double hi = 0;
        float valueWidth = 0;
        for (Stats.Point p : points) {
            hi = Math.max(hi, p.value);
            valueWidth = Math.max(valueWidth, valuePaint.measureText(compact(p.value)));
        }
        double step = Math.max(1, niceStep(hi, 4));
        double max = Math.ceil(hi / step) * step;
        float textHeight = labelPaint.getTextSize();
        float labelWidth = 0;
        for (Stats.Point p : points) {
            labelWidth = Math.max(labelWidth, labelPaint.measureText(p.label));
        }
        float start = getPaddingLeft();
        float labelArea = Math.min(labelWidth + 10 * dp, getWidth() * 0.38f);
        float left = start + labelArea;
        float right = getWidth() - getPaddingRight() - valueWidth - 8 * dp;
        float top = getPaddingTop() + 4 * dp;
        float bottom = top + points.size() * rowHeight();

        labelPaint.setTextAlign(Paint.Align.CENTER);
        float lastLabelRight = -Float.MAX_VALUE;
        for (double v = 0; v <= max + step / 2; v += step) {
            float x = map(v, 0, max, left, right);
            canvas.drawLine(x, top, x, bottom, gridPaint);
            String text = compact(v);
            float half = labelPaint.measureText(text) / 2;
            if (x - half >= lastLabelRight + 6 * dp) {
                canvas.drawText(text, x, bottom + textHeight + 6 * dp, labelPaint);
                lastLabelRight = x + half;
            }
        }
        canvas.drawLine(left, top, left, bottom, axisPaint);
        canvas.drawLine(left, bottom, right, bottom, axisPaint);

        labelPaint.setTextAlign(Paint.Align.LEFT);
        valuePaint.setTextAlign(Paint.Align.LEFT);
        float barHeight = rowHeight() * 0.55f;
        for (int i = 0; i < points.size(); i++) {
            Stats.Point p = points.get(i);
            float cy = top + rowHeight() * (i + 0.5f);
            CharSequence name = TextUtils.ellipsize(p.label, labelPaint, labelArea - 10 * dp, TextUtils.TruncateAt.END);
            canvas.drawText(name, 0, name.length(), start, cy + textHeight / 3, labelPaint);
            float end = map(p.value, 0, max, left, right);
            rect.set(left, cy - barHeight / 2, Math.max(left + 2 * dp, end), cy + barHeight / 2);
            canvas.drawRoundRect(rect, 3 * dp, 3 * dp, barPaint);
            canvas.drawText(compact(p.value), rect.right + 4 * dp, cy + textHeight / 3, valuePaint);
        }
    }

    private static float map(double value, double from, double to, float start, float end) {
        return (float) (start + (end - start) * (value - from) / (to - from));
    }

    /** A step of 1, 2 or 5 × 10^n that splits the range into about the given number of intervals. */
    static double niceStep(double range, int intervals) {
        if (range <= 0 || intervals <= 0) {
            return 1;
        }
        double raw = range / intervals;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        double fraction = raw / magnitude;
        double nice = fraction <= 1 ? 1 : fraction <= 2 ? 2 : fraction <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    /** Short number for axis and bar labels: 135, 137.5, 12.5k, 1.2M. */
    static String compact(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000) {
            return trim(value / 1_000_000) + "M";
        }
        if (abs >= 10_000) {
            return trim(value / 1_000) + "k";
        }
        return Stats.format(Math.round(value * 10) / 10.0);
    }

    private static String trim(double value) {
        String s = String.format(Locale.US, "%.1f", value);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}

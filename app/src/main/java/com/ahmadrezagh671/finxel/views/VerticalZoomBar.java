package com.ahmadrezagh671.finxel.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;

import com.ahmadrezagh671.finxel.R;


/**
 * Minimal vertical "seek bar" for zoom control.
 *
 * Behaves like a real SeekBar for progress: supports android:max and
 * android:progress as plain ints.
 *
 * Visual behavior:
 *  - At rest it sits flush against the screen edge with the outer corners
 *    squared off (like a docked tab).
 *  - While the user drags, it slides away from the edge (revealing a margin)
 *    and the squared corners round out into a full pill/capsule shape.
 *  - On release it animates back to the docked state.
 *
 * IMPORTANT: give the view a layout_marginEnd equal to app:edgeMargin so the
 * math lines up (the view translates INTO that reserved margin space when
 * docked, so nothing gets clipped by the parent).
 */
public class VerticalZoomBar extends View {

    public interface OnZoomChangeListener {
        void onZoomChanged(VerticalZoomBar bar, int progress, boolean fromUser);
        void onStartTrackingTouch(VerticalZoomBar bar);
        void onStopTrackingTouch(VerticalZoomBar bar);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF trackRect = new RectF();
    private final RectF fillRect = new RectF();
    private final float[] radii = new float[8];


    private float iconHalfLen;
    private float iconMargin;


    private int max = 100;
    private int progress = 50;

    private float stuckFactor = 1f; // 1 = docked at edge, 0 = fully floating
    private float edgeMarginPx;
    private float fullRadius;

    private ValueAnimator stuckAnimator;
    private OnZoomChangeListener listener;

    public VerticalZoomBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;

        int trackColor = Color.parseColor("#33000000");
        int fillColor = Color.parseColor("#3D7DFF");
        int iconColor = Color.parseColor("#FFFFFFFF");
        edgeMarginPx = 16 * density;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VerticalZoomBar);
            trackColor = ta.getColor(R.styleable.VerticalZoomBar_trackColor, trackColor);
            fillColor = ta.getColor(R.styleable.VerticalZoomBar_fillColor, fillColor);
            iconColor = ta.getColor(R.styleable.VerticalZoomBar_iconColor, iconColor);
            edgeMarginPx = ta.getDimension(R.styleable.VerticalZoomBar_edgeMargin, edgeMarginPx);
            ta.recycle();

            // Standard framework attrs, same ones SeekBar/ProgressBar use.
            TypedArray sa = context.obtainStyledAttributes(attrs,
                    new int[]{android.R.attr.max, android.R.attr.progress});
            max = sa.getInt(0, max);
            progress = sa.getInt(1, progress);
            sa.recycle();
        }

        progress = clamp(progress);

        trackPaint.setColor(trackColor);
        trackPaint.setStyle(Paint.Style.FILL);

        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);

        iconPaint.setColor(iconColor);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(1.8f * density);
        iconPaint.setStrokeCap(Paint.Cap.ROUND);

        iconMargin = 14 * density;

        setElevation(4 * density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fullRadius = w / 2f;
        trackRect.set(0, 0, w, h);
        iconHalfLen = w * 0.16f; // scales the +/- glyphs with bar width
        applyTranslation();
        rebuildRadii();
    }

    private void rebuildRadii() {
        // Left side always fully rounded (never touches an edge).
        // Right side rounds from 0 (docked/squared) up to fullRadius (floating/pill).
        float rightRadius = fullRadius * (1f - stuckFactor);

        radii[0] = fullRadius;   radii[1] = fullRadius;   // top-left
        radii[2] = rightRadius; radii[3] = rightRadius;   // top-right
        radii[4] = rightRadius; radii[5] = rightRadius;   // bottom-right
        radii[6] = fullRadius;   radii[7] = fullRadius;   // bottom-left
        invalidate();
    }

    private void applyTranslation() {
        // stuckFactor 1 -> shift right by the margin, landing flush on the edge.
        // stuckFactor 0 -> sit at the laid-out position, margin gap visible.
        setTranslationX(edgeMarginPx * stuckFactor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        clipPath.reset();
        clipPath.addRoundRect(trackRect, radii, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);

        // Track
        canvas.drawRect(trackRect, trackPaint);

        // Fill grows from the bottom upward with progress
        float fraction = max == 0 ? 0f : (float) progress / (float) max;
        float fillTop = h * (1f - fraction);
        fillRect.set(0, fillTop, w, h);
        canvas.drawRect(fillRect, fillPaint);

        canvas.restore();

        drawIcons(canvas, w, h);
    }

    /** Purely decorative +/- glyphs, no touch behavior attached to them. */
    private void drawIcons(Canvas canvas, int w, int h) {
        float cx = w / 2f;

        // Plus icon, top
        float plusCy = iconMargin + iconHalfLen;
        canvas.drawLine(cx - iconHalfLen, plusCy, cx + iconHalfLen, plusCy, iconPaint);
        canvas.drawLine(cx, plusCy - iconHalfLen, cx, plusCy + iconHalfLen, iconPaint);

        // Minus icon, bottom
        float minusCy = h - iconMargin - iconHalfLen;
        canvas.drawLine(cx - iconHalfLen, minusCy, cx + iconHalfLen, minusCy, iconPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(true);
                animateStuck(false); // pull away from edge
                updateProgressFromTouch(event.getY());
                if (listener != null) listener.onStartTrackingTouch(this);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                updateProgressFromTouch(event.getY());
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                ViewParent parent = getParent();
                if (parent != null) parent.requestDisallowInterceptTouchEvent(false);
                animateStuck(true); // dock back to the edge
                if (listener != null) listener.onStopTrackingTouch(this);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void updateProgressFromTouch(float y) {
        int h = getHeight();
        float clamped = Math.max(0, Math.min(h, y));
        float fraction = 1f - (clamped / h); // top = 1.0, bottom = 0.0
        int newProgress = Math.round(fraction * max);
        setProgressInternal(newProgress, true);
    }

    private void animateStuck(boolean stuck) {
        float target = stuck ? 1f : 0f;
        if (stuckAnimator != null) stuckAnimator.cancel();
        stuckAnimator = ValueAnimator.ofFloat(stuckFactor, target);
        stuckAnimator.setDuration(220);
        stuckAnimator.setInterpolator(new DecelerateInterpolator());
        stuckAnimator.addUpdateListener(a -> {
            stuckFactor = (float) a.getAnimatedValue();
            applyTranslation();
            rebuildRadii();
        });
        stuckAnimator.start();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(max, value));
    }

    /** Set progress programmatically, no listener callback with fromUser=true. */
    public void setProgress(int value) {
        setProgressInternal(clamp(value), false);
    }

    private void setProgressInternal(int value, boolean fromUser) {
        progress = clamp(value);
        invalidate();
        if (listener != null) listener.onZoomChanged(this, progress, fromUser);
    }

    public int getProgress() {
        return progress;
    }

    public void setMax(int value) {
        max = Math.max(0, value);
        progress = clamp(progress);
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setOnZoomChangeListener(OnZoomChangeListener l) {
        this.listener = l;
    }
}

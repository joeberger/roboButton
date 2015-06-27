package com.ndipatri.roboButton.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.ndipatri.roboButton.R;
import com.ndipatri.roboButton.enums.ButtonState;

public class ProgressView extends View {

    private static final String TAG = ProgressView.class.getCanonicalName();

    ButtonState buttonState;

    Paint redPaint = null;
    Paint greenPaint = null;

    int strokeWidth;
    int animationDurationMillis;

    int width = 0;
    int height = 0;

    private ObjectAnimator arcAnimator;

    int currentValue = 0;

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAttributes(attrs);

        setup();
    }

    private void setAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressView);

        strokeWidth = a.getDimensionPixelSize(R.styleable.ProgressView_strokeWidth, -1);
        animationDurationMillis = a.getInteger(R.styleable.ProgressView_animationDurationMillis, -1);

        a.recycle();

    }

    //initialize the paints,colors,views, etc...
    private void setup() {
        Log.d(TAG, "init()");

        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setStrokeWidth(strokeWidth);
        redPaint.setStyle(Style.STROKE);
        redPaint.setColor(getResources().getColor(R.color.red));

        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setStrokeWidth(strokeWidth);
        greenPaint.setStyle(Style.STROKE);
        greenPaint.setColor(getResources().getColor(R.color.green));

        setBackgroundResource(R.color.transparent);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        setVisibility(View.GONE);
    }

    public void render(final ButtonState buttonState) {

        setVisibility(View.VISIBLE);

        this.buttonState = buttonState;

        start();
    }

    protected Paint getPaint(final ButtonState buttonState) {
        Paint paint = null;

        // Currently we only paint if in pending state...

        switch (buttonState) {
            case ON_PENDING:
                paint = greenPaint;
                break;
            case OFF_PENDING:
                paint = redPaint;
                break;
            default:
                paint = null;

        }

        return paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw()");

        Paint arcPaint = getPaint(buttonState);
        if (arcPaint != null) {
            canvas.save();

            canvas.drawPath(renderPath(getCurrentValue()), arcPaint);

            canvas.restore();

            super.onDraw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure()");

        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    // This will draw an arc given the current fuel level
    private Path renderPath(int percentage) {
        Log.d(TAG, "createPath(percentage:'" + percentage + "')");
        Path path = new Path();

        RectF boundingRectangle = new RectF(strokeWidth + getPaddingLeft(),
                strokeWidth + getPaddingTop(),
                width - strokeWidth - getPaddingRight(),
                height - strokeWidth - getPaddingBottom());

        path.addArc(boundingRectangle, -90, 360 * percentage / 100);

        return path;
    }

    public void start() {

        if (arcAnimator != null) {
            arcAnimator.cancel();
            arcAnimator = null;
        }

        arcAnimator = ObjectAnimator.ofInt(this, "currentValue", 0, 100);
        arcAnimator.setRepeatCount(0); // execute only once
        arcAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        arcAnimator.setDuration(animationDurationMillis);
        arcAnimator.setStartDelay(0);
        arcAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "onAnimationStart()");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "onAnimationEnd()");

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "onAnimationCancel()");

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                Log.d(TAG, "onAnimationRepeat()");

            }
        });
        arcAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (arcAnimator != null) {
            arcAnimator.cancel();
            arcAnimator = null;
        }
    }

    public synchronized int getCurrentValue() {
        return currentValue;
    }

    public synchronized void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
        Log.d(TAG, "setCurrentValue(currentValue='" + getCurrentValue() + "')");

        postInvalidate();
    }
}

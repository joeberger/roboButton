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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.ndipatri.roboButton.R;

public class ButtonView extends ViewGroup {

    private static final String TAG = ButtonView.class.getCanonicalName();

    int textPadding;

    boolean titleSpecificallySet = false;
    int titleTextSize;
    int titleTextColor;
    String titleTextSuffix;

    Paint solidRingPaint = null;
    Paint redPaint = null;
    Paint yellowPaint = null;
    Paint greenPaint = null;

    int strokeWidth;
    int animationDurationMillis;

    int width = 0;
    int height = 0;

    private ObjectAnimator arcAnimator;

    protected TextView titleTextView;

    /**
     * Should range between 0 and tankCapacity
     */
    int targetFuelLevel;

    int tankCapacity;

    int currentValue = 0;

    public ButtonView(Context context) {
        this(context, null);
    }

    public ButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAttributes(attrs);

        setup();
    }

    private void setAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ButtonView);

        strokeWidth = a.getDimensionPixelSize(R.styleable.ButtonView_strokeWidth, -1);
        animationDurationMillis = a.getInteger(R.styleable.ButtonView_animationDurationMillis, -1);
        textPadding = a.getDimensionPixelSize(R.styleable.ButtonView_textPadding, -1);
        titleTextSize = a.getDimensionPixelSize(R.styleable.ButtonView_titleTextSize, -1);
        titleTextColor = a.getInteger(R.styleable.ButtonView_titleTextColor, -1);

        a.recycle();

    }

    //initialize the paints,colors,views, etc...
    private void setup() {
        Log.d(TAG, "init()");

        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setStrokeWidth(strokeWidth);
        redPaint.setStyle(Style.STROKE);
        redPaint.setColor(getResources().getColor(R.color.red));

        yellowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yellowPaint.setStrokeWidth(strokeWidth);
        yellowPaint.setStyle(Style.STROKE);
        yellowPaint.setColor(getResources().getColor(R.color.yellow));

        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setStrokeWidth(strokeWidth);
        greenPaint.setStyle(Style.STROKE);
        greenPaint.setColor(getResources().getColor(R.color.green));

        solidRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        solidRingPaint.setStrokeWidth(strokeWidth);
        solidRingPaint.setStyle(Style.STROKE);
        solidRingPaint.setColor(getResources().getColor(R.color.grey));

        setBackgroundResource(R.color.transparent);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        titleTextView = new TextView(getContext());
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleTextSize);
        titleTextView.setTextColor(titleTextColor);
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);

        addView(titleTextView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure()");

        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);

        // Let these define their own size based on content...
        titleTextView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int titleTextWidth = titleTextView.getMeasuredWidth();
        int titleTextHeight = titleTextView.getMeasuredHeight();

        int leftLevelText = width/2 - titleTextWidth/2;
        int rightLevelText = width/2 + titleTextWidth/2;
        int topLevelText = height/2 - titleTextHeight - textPadding;
        int bottomLevelText = height/2 - textPadding;

        titleTextView.layout(leftLevelText, topLevelText, rightLevelText, bottomLevelText);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw()");

        canvas.save();

        canvas.drawPath(renderPath(100), solidRingPaint);

        canvas.drawPath(renderPath(getCurrentValue()), greenPaint);

        canvas.restore();
        super.onDraw(canvas);
    }

    // This will draw an arc given the current fuel level
    private Path renderPath(int fuelLevelPercentage) {
        Log.d(TAG, "createPath(currentValue:'" + fuelLevelPercentage + "')");
        Path fuelGaugePath = new Path();

        RectF boundingRectangle = new RectF(strokeWidth + getPaddingLeft(),
                                            strokeWidth + getPaddingTop(),
                                            width - strokeWidth - getPaddingRight(),
                                            height - strokeWidth - getPaddingBottom());

        fuelGaugePath.addArc(boundingRectangle, -90, 360 * fuelLevelPercentage/100);

        return fuelGaugePath;
    }

    public void start() {

        if(arcAnimator != null) {
            arcAnimator.cancel();
            arcAnimator = null;
        }

        arcAnimator = ObjectAnimator.ofInt(this, "currentValue", 0, getTargetFuelLevelPercentage());
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

    protected int getTargetFuelLevelPercentage() {
        return (int)(((float)targetFuelLevel/tankCapacity)*100);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if(arcAnimator != null) {
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

    public void setGaugeTitle(String titleText) {
        this.titleSpecificallySet = true;

        titleTextView.setText(titleText + (titleTextSuffix == null ? "" : titleTextSuffix));
    }
}

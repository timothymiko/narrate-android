package com.datonicgroup.narrate.app.ui.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.util.LogUtil;

/**
 * Created by timothymiko on 8/29/14.
 */
public abstract class AbsView extends View {


    Handler mLocalHandler;
    Choreographer.FrameCallback redrawCallback;

    protected float mWidth;
    protected float mHeight;

    protected float mCenterX;
    protected float mCenterY;

    protected float mMaxWidth;
    protected float mMaxHeight;

    protected int mPaddingLeft;
    protected int mPaddingTop;
    protected int mPaddingRight;
    protected int mPaddingBottom;
    protected final Rect mVisibleRect = new Rect();

    public AbsView(Context context) {
        this(context, null);
    }

    public AbsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AbsView);
        mMaxWidth = typedArray.getDimension(R.styleable.AbsView_maxWidth, -1);
        mMaxHeight = typedArray.getDimension(R.styleable.AbsView_maxHeight, -1);
        typedArray.recycle();
    }

    Handler getViewHandler() {
        return mLocalHandler;
    }

    @SuppressLint("NewApi")
    protected void init() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            redrawCallback = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    redrawInternal();
                }
            };
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        this.mHeight = h;
        this.mCenterX = mWidth / 2;
        this.mCenterY = mHeight / 2;

        if (mMaxWidth > 0)
            if (mWidth > mMaxWidth)
                setWidth(Math.round(mMaxWidth));

        if (mMaxWidth > 0)
            if (mHeight > mMaxHeight)
                setHeight(Math.round(mMaxHeight));

        mVisibleRect.set(mPaddingLeft, mPaddingTop, Math.round(mWidth - mPaddingRight), Math.round(mHeight - mPaddingBottom));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();

        mVisibleRect.set(mPaddingLeft, mPaddingTop, Math.round(mWidth - mPaddingRight), Math.round(mHeight - mPaddingBottom));
    }

    public void setHeight(int height) {
        if (getLayoutParams() != null) {
            getLayoutParams().height = height;
            requestLayout();
            invalidate();
        }
    }

    public void setWidth(int width) {
        if (getLayoutParams() != null) {
            getLayoutParams().width = width;
            requestLayout();
            invalidate();
        }
    }

    @Override
    public void invalidate() {
        redraw();
    }

    @Override
    public void postInvalidate() {
        redraw();
    }

    protected void redraw() {
        log("redraw()");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // stay in sync with display updates
            Choreographer choreographer = Choreographer.getInstance();
            choreographer.removeFrameCallback(redrawCallback);
            choreographer.postFrameCallback(redrawCallback);

        } else {
            redrawInternal();
        }
    }

    private void redrawInternal() {
        log("redrawInternal()");
        if (Looper.myLooper() == Looper.getMainLooper())
            super.invalidate();
        else
            super.postInvalidate();
    }

    public void setBackground(Drawable background) {
        if (Build.VERSION.SDK_INT > 15)
            super.setBackground(background);
        else
            super.setBackgroundDrawable(background);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        log("onDraw()");
    }

    protected void log(String message) {
        LogUtil.log(getClass().getSimpleName(), message);
    }
}

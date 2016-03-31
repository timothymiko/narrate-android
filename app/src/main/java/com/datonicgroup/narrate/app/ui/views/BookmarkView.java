package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 12/22/14.
 */
public class BookmarkView extends View {

    private final float OUTLINE_STROKE_WIDTH = 0.2f;

    private Paint mFilledPaint;
    private Paint mOutlinePaint;

    private final Path mPath = new Path();
    private final Path mOutlinePath = new Path();

    private boolean mDrawOutline;

    public BookmarkView(Context context) {
        this(context, null);
    }

    public BookmarkView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BookmarkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BookmarkView);

        mFilledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFilledPaint.setStyle(Paint.Style.FILL);
        mFilledPaint.setColor(typedArray.getColor(R.styleable.BookmarkView_fill_color, Color.BLACK));

        typedArray.recycle();

        mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setColor(getResources().getColor(R.color.divider));
    }

    public void setFilled(boolean filled) {
        this.mDrawOutline = !filled;
        invalidate();
    }

    public void setOutlineColor(int color) {
        mOutlinePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float sw = OUTLINE_STROKE_WIDTH * (float)w;
        float hsw = sw / 2;
        mOutlinePaint.setStrokeWidth(sw);

        mPath.reset();
        mPath.moveTo(0, 0);
        mPath.lineTo(w, 0);
        mPath.lineTo(w, h);
        mPath.lineTo(w/2, h - (0.1f * h));
        mPath.lineTo(0, h);
        mPath.close();

        mOutlinePath.reset();
        mOutlinePath.moveTo(hsw, hsw);
        mOutlinePath.lineTo(w - hsw, hsw);
        mOutlinePath.lineTo(w - hsw, h - hsw);
        mOutlinePath.lineTo(w/2, h - hsw - (0.1f * h));
        mOutlinePath.lineTo(hsw, h - hsw);
        mOutlinePath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if ( mDrawOutline )
            canvas.drawPath(mOutlinePath, mOutlinePaint);
        else
            canvas.drawPath(mPath, mFilledPaint);
    }
}

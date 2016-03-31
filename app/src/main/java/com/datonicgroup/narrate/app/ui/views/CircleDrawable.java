package com.datonicgroup.narrate.app.ui.views;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.Shape;

/**
 * Created by timothymiko on 8/28/14.
 */
public class CircleDrawable extends ShapeDrawable {

    private Paint mPaint;
    private int color;
    private float radius;

    public CircleDrawable(int color, float radius) {
        super(new OvalShape());

        this.color = color;
        this.radius = radius;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
        canvas.drawCircle(getIntrinsicWidth()/2, getIntrinsicHeight()/2, radius, mPaint);
    }
}

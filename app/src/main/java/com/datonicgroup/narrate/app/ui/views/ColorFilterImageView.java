package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 1/5/15.
 */
public class ColorFilterImageView extends ImageView {

    private int color;

    public ColorFilterImageView(Context context) {
        this(context, null);
    }

    public ColorFilterImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorFilterImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AbsView);
        color = typedArray.getColor(R.styleable.AbsView_colorFilter, Color.BLACK);
        typedArray.recycle();

        setColor(color);
    }

    public void setColor(int color) {
        this.color = color;
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    public int getColor() {
        return color;
    }
}

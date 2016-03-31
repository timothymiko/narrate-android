package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 11/14/14.
 */
public class RoundedImageView extends ImageView {

    private float corners;

    public RoundedImageView(Context context) {
        this(context, null);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundedImageView);
        corners = typedArray.getDimension(R.styleable.RoundedImageView_corners, -1);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if ( corners != -1 ) {
            Path clipPath = new Path();
            RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
            clipPath.addRoundRect(rect, corners, corners, Path.Direction.CW);
            canvas.clipPath(clipPath);
        }

        super.onDraw(canvas);
    }
}

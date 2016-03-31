package com.datonicgroup.narrate.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;

/**
 * Created by timothymiko on 12/21/14.
 */
public class RoundedCornerTransformation extends CenterCrop {

    private int cornerSize;

    public RoundedCornerTransformation(Context context, int cornerResId) {
        super(context);
        cornerSize = context.getResources().getDimensionPixelSize(cornerResId);
    }

    public RoundedCornerTransformation(Context context) {
        super(context);
    }

    public RoundedCornerTransformation(BitmapPool bitmapPool) {
        super(bitmapPool);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int width, int height) {

        Bitmap transformed = super.transform(pool, toTransform, width, height);

        Bitmap output = null; //pool.get(width, height, toTransform.getConfig() != null ? toTransform.getConfig() : Bitmap.Config.ARGB_8888);

        if ( output == null )
                output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, cornerSize, cornerSize, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(transformed, rect, rect, paint);

        transformed.recycle();

        return output;
    }

    @Override
    public String getId() {
        return "com.datonicgroup.narrate.app.RoundedTransformation";
    }
}

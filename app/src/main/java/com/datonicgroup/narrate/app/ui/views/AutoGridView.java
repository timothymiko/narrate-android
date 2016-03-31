package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.datonicgroup.narrate.app.R;

/**
 * A Custom GridView where you can set the default width of a cell
 * and it will set the numbers of columns accordingly.
 *
 * Created by Musenkishi on 2014-03-04 21:08.
 *
 * https://github.com/Musenkishi/wally/blob/master/wally/src/main/java/com/musenkishi/wally/views/AutoGridView.java
 */
public class AutoGridView extends GridRecyclerView {

    private int defaultCellWidth;

    public AutoGridView(Context context) {
        super(context);
    }

    public AutoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            TypedArray typedArray = theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.AutoGridView,
                    0, 0);
            if (typedArray != null) {
                try {
                    defaultCellWidth = (int) typedArray.getDimension(R.styleable.AutoGridView_defaultCellWidth, 0);
                } finally {
                    typedArray.recycle();
                }
            }
        }
    }

    public int getDefaultCellWidth(){
        return defaultCellWidth;
    }

    public void setDefaultCellWidth(int width){
        this.defaultCellWidth = width;
    }

}

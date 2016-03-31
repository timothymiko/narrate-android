package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by timothymiko on 11/16/14.
 */
public class ListeningScrollView extends ScrollView {

    public interface OnScrollListener {
        void onScrollChanged(int dx, int dy);
    }

    private OnScrollListener mListener;

    public ListeningScrollView(Context context) {
        super(context);
    }

    public ListeningScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListeningScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnScrollListener(OnScrollListener mListener) {
        this.mListener = mListener;
    }

    @Override
    protected void onScrollChanged(int dx, int dy, int oldDx, int oldDy) {
        super.onScrollChanged(dx, dy, oldDx, oldDy);

        if ( mListener != null )
            mListener.onScrollChanged(dx, dy);
    }
}

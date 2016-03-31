package com.datonicgroup.narrate.app.ui.views;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 12/24/14.
 */
public class ControllableViewPager extends ViewPager {

    private boolean mSwipeEnabled = true;
    private int mPlacesPadding;
    private boolean mOnMapFragment;

    public ControllableViewPager(Context context) {
        super(context);
        mPlacesPadding = getResources().getDimensionPixelOffset(R.dimen.map_view_view_pager_scroll_padding);
    }

    public ControllableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPlacesPadding = getResources().getDimensionPixelOffset(R.dimen.map_view_view_pager_scroll_padding);
    }

    public boolean isSwipeEnabled() {
        return mSwipeEnabled;
    }

    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
    }

    public void setOnMapFragment(boolean state) {
        this.mOnMapFragment = state;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if ( mSwipeEnabled )
            return super.onInterceptTouchEvent(ev);
        else
            return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if ( mSwipeEnabled )
            return super.onTouchEvent(ev);
        else
            return false;
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if ( mOnMapFragment ) {
            if ( x <= mPlacesPadding )
                return false;
            else if ( x >= (v.getWidth()-mPlacesPadding) )
                return false;
            else
                return super.canScroll(v, checkV, dx, x, y);
        } else
            return super.canScroll(v, checkV, dx, x, y);
    }
}

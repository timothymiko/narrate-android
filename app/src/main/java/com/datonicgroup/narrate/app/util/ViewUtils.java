package com.datonicgroup.narrate.app.util;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by timothymiko on 12/30/14.
 */
public class ViewUtils {

    public static void setWrapContent(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if ( lp == null ) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            v.setLayoutParams(lp);
        } else {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            v.requestLayout();
        }
    }

    public static void setMatchParent(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if ( lp == null ) {
            lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            v.setLayoutParams(lp);
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            v.requestLayout();
        }
    }
}

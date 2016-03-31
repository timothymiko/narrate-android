package com.datonicgroup.narrate.app.util;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.datonicgroup.narrate.app.ui.GlobalApplication;

/**
 * Created by timothymiko on 8/11/14.
 */
public class ScreenUtil {

    public static int getPixelsFromDP(float dip) {
        final DisplayMetrics dm = GlobalApplication.getAppContext().getResources().getDisplayMetrics();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, dm));
    }

}

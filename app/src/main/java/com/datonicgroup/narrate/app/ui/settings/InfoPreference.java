package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by timothymiko on 1/7/15.
 */
public class InfoPreference extends ButtonPreference {

    public InfoPreference(Context context) {
        super(context);
    }

    @Override
    public void init() {
        super.init();

        getButton().setTypeface(null, Typeface.NORMAL);
        getButton().setClickable(false);
    }
}

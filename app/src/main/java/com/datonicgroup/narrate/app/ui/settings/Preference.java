package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 12/30/14.
 */
public class Preference extends RelativeLayout {

    private String mKey;

    public Preference(Context context) {
        this(context, null);
    }

    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.Preference);
        mKey = typedArray.getString(R.styleable.Preference_key);
        typedArray.recycle();

        init();
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        this.mKey = key;
    }

    public void init() {

    }
}

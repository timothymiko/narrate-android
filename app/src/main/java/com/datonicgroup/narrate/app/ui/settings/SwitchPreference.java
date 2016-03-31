package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 12/30/14.
 */
public class SwitchPreference extends TextPreference {

    private SwitchCompat mSwitch;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mForced;

    public SwitchPreference(Context context) {
        super(context);
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isChecked() {
        return mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mForced = true;
        mSwitch.setChecked(checked);
    }

    public void setOnCheckedChangedListener(CompoundButton.OnCheckedChangeListener listener) {
        this.mListener = listener;
    }

    public void setId(int id) {
        mSwitch.setId(id);
    }

    public int getId() {
        return mSwitch.getId();
    }

    public void setTag(Object tag) {
        mSwitch.setTag(tag);
    }

    public String getTag() {
        return (String) mSwitch.getTag();
    }

    public void setSwitchVisibility(int visibility) {
        mSwitch.setVisibility(visibility);
    }

    public int getSwitchVisibility() { return mSwitch.getVisibility(); }

    @Override
    public void init() {
        super.init();

        mSwitch = new SwitchCompat(getContext()) {

            boolean isTouched = false;

            @Override
            public void setChecked(boolean checked) {
                if ( isTouched || mForced ) {
                    super.setChecked(checked);

                    if ( isTouched && mListener != null ) {
                        isTouched = false;
                        mListener.onCheckedChanged(this, checked);
                    }

                    mForced = false;

                }
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                isTouched = true;
                return super.onTouchEvent(ev);
            }
        };
        mSwitch.setId(R.id.settings_switch);
        RelativeLayout.LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mSwitch.setLayoutParams(lp);

        lp = (LayoutParams) mTextView.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.LEFT_OF, mSwitch.getId());
        mTextView.setLayoutParams(lp);

        int p = getResources().getDimensionPixelOffset(R.dimen.eight_dp) / 4;
        mSwitch.setPadding(p, p, p, p);

        addView(mSwitch);
    }
}

package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 12/31/14.
 */
public class ButtonPreference extends TextPreference implements View.OnClickListener {

    private TextView mButton;
    private OnClickListener mListener;

    public ButtonPreference(Context context) {
        super(context);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected TextView getButton() {
        return mButton;
    }

    public void setButtonText(String s) {
        mButton.setText(s.toUpperCase());
    }

    public void setButtonText(int resId) {
        mButton.setText(getResources().getString(resId).toUpperCase());
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public void init() {
        super.init();

        mButton = new TextView(getContext());
        mButton.setId(R.id.settings_button);
        mButton.setClickable(true);
        mButton.setBackgroundResource(R.drawable.default_selector);
        mButton.setTypeface(null, Typeface.BOLD);
        mButton.setTextColor(getResources().getColor(R.color.accent));
        RelativeLayout.LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mButton.setGravity(Gravity.RIGHT);
        mButton.setLayoutParams(lp);
        mButton.setMinWidth(getResources().getDimensionPixelOffset(R.dimen.default_gap)*3/2);
        mButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        mButton.setOnClickListener(this);


        lp = (LayoutParams) mTextView.getLayoutParams();
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.LEFT_OF, mButton.getId());
        mTextView.setLayoutParams(lp);

        int p = getResources().getDimensionPixelOffset(R.dimen.eight_dp) / 4;
        mButton.setPadding(p, p, p, p);

        addView(mButton);

        setBackgroundResource(R.drawable.default_selector);
        setClickable(true);
        setOnClickListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mButton.setMaxWidth(w/2);
    }

    @Override
    public void onClick(View v) {
        if ( mListener != null )
            mListener.onClick(this);
    }
}

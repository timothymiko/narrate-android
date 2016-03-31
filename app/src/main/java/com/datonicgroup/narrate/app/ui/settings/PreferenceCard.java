package com.datonicgroup.narrate.app.ui.settings;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.PreferenceListener;

/**
 * Created by timothymiko on 12/30/14.
 */
public class PreferenceCard extends LinearLayout implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    protected SwitchPreference mTitle;
    protected PreferenceListener mListener;
    protected boolean mExpanded;
    private int gap;

    public PreferenceCard(Context context) {
        this(context, null);
    }

    public PreferenceCard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setTitle(int resId) {
        this.mTitle.getTextView().setText(resId);
    }

    public void setTitle(String title) {
        this.mTitle.getTextView().setText(title);
    }

    public String getTitle() {
        return this.mTitle.getTextView().getText().toString();
    }

    public void setPreferenceListener(PreferenceListener listener) {
        this.mListener = listener;
    }

    protected void setSwitchEnabled(boolean enabled) {
        if (enabled)
            mTitle.setSwitchVisibility(VISIBLE);
        else
            mTitle.setSwitchVisibility(GONE);
    }

    protected void init() {
        setOrientation(LinearLayout.VERTICAL);

        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            setLayoutParams(lp);
        }

        gap = getResources().getDimensionPixelOffset(R.dimen.default_gap);
        setBackgroundResource(R.drawable.settings_card);
        setPadding(gap, gap, gap, gap);

        mTitle = new SwitchPreference(getContext());
        mTitle.setId(R.id.settings_title);
        mTitle.getTextView().setTypeface(null, Typeface.BOLD);
        mTitle.getTextView().setTextColor(getResources().getColor(R.color.accent));
        mTitle.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        mTitle.setSwitchVisibility(GONE);
        mTitle.setOnCheckedChangedListener(this);
        addView(mTitle);

        mTitle.setPadding(0, 0, 0, gap);

        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        mlp.bottomMargin = gap / 2;
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if ( mTitle.getSwitchVisibility() == VISIBLE && !mTitle.isChecked() ) {
            int toHeight = gap * 2;
            toHeight += getChildAt(0).getHeight();
            getLayoutParams().height = toHeight;
            requestLayout();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if ( buttonView.getId() == R.id.settings_title ) {
            if (isChecked) {
                animateOpen();
            } else {
                animateClosed();
            }
        }
    }

    protected void animateClosed() {
        mExpanded = false;
        int toHeight = gap * 2;
        toHeight += getChildAt(0).getHeight();

        ValueAnimator anim = ValueAnimator.ofInt(getHeight(), toHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getLayoutParams().height = (int) animation.getAnimatedValue();
                requestLayout();
            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    protected void animateOpen() {
        mExpanded = true;
        int toHeight = gap * 2;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            toHeight += child.getMeasuredHeight();
        }

        ValueAnimator anim = ValueAnimator.ofInt(getHeight(), toHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                getLayoutParams().height = (int) animation.getAnimatedValue();
                requestLayout();
            }
        });
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    @Override
    public void onClick(View v) {

    }
}

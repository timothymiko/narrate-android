package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.util.ViewUtils;

/**
 * Created by timothymiko on 12/30/14.
 */
public class TextPreference extends Preference {

    protected TextView mTextView;

    public TextPreference(Context context) {
        this(context, null);
    }

    public TextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabView);
        String title = typedArray.getString(R.styleable.TabView_text);
        typedArray.recycle();

        mTextView.setText(title);
    }

    public TextView getTextView() {
        return mTextView;
    }

    public String getTitle() {
        return this.mTextView.getText().toString();
    }

    public void setTitle(String title) {
        this.mTextView.setText(title);
    }

    public void setTitle(int resId) {
        this.mTextView.setText(resId);
    }

    @Override
    public void init() {
        super.init();

        mTextView = new TextView(getContext());
        mTextView.setId(R.id.settings_text);
        mTextView.setGravity(Gravity.CENTER_VERTICAL);
        ViewUtils.setWrapContent(mTextView);
        mTextView.setTextColor(getContext().getResources().getColor(R.color.primary_text));

        addView(mTextView);

        int p = getResources().getDimensionPixelOffset(R.dimen.eight_dp) / 4;
        setPadding(0, p, 0, p);
    }
}

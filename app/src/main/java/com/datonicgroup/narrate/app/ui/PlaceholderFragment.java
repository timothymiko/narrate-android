package com.datonicgroup.narrate.app.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.ui.base.BaseEntryFragment;

/**
 * Created by timothymiko on 12/14/14.
 */
public abstract class PlaceholderFragment extends BaseEntryFragment {

    private String title;

    protected PlaceholderFragment(String title) {
        this.title = title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView t = new TextView(getActivity());
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return t;
    }
}

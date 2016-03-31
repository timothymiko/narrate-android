/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.datetimepicker.date;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;

/**
 * An adapter for a list of {@link SimpleMonthView} items.
 */
public class SimpleMonthAdapter extends MonthAdapter {

    private boolean mTitlesVisible = true;

    private int mSelectedColor = -1;
    private int mTodayColor = -1;

    private HashSet<Integer> mDays;

    public SimpleMonthAdapter(Context context, DatePickerController controller) {
        super(context, controller);
    }

    @Override
    public MonthView createMonthView(Context context) {
        final MonthView monthView = new SimpleMonthView(context);
        monthView.setDatePickerController(mController);
        return monthView;
    }

    @Override
    public void showTitles(boolean show) {
        mTitlesVisible = show;
        notifyDataSetChanged();
    }

    @Override
    public boolean getShowingTitles() {
        return mTitlesVisible;
    }

    @Override
    public void setSelectedColor(int color) {
        this.mSelectedColor = color;
    }

    @Override
    public void setTodayColor(int color) {
        this.mTodayColor = color;
    }

    @Override
    public void setIndicators(HashSet<Integer> days) {
        this.mDays = days;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MonthView v = (MonthView) super.getView(position, convertView, parent);

        if ( mSelectedColor != -1 )
            v.setSelectedColor(mSelectedColor);

        if ( mTodayColor != -1 )
            v.setTodayColor(mSelectedColor);

//        v.setDrawTitles(mTitlesVisible);
        float targetAlpha = mTitlesVisible ? 255 : 0;
        if ( v.getTitleAlpha() != targetAlpha )
            v.animateTitles(!mTitlesVisible);

        v.setIndicators(mDays);

//        } else {
//
//            int alpha = 255;
//            long elapsed = System.currentTimeMillis() - mAnimStartTime;
//
//            if (elapsed < ANIM_TIME) {
//                float fraction = (float) elapsed / ANIM_TIME;
//                Log.d("", "Elapsed: " + elapsed);
//                Log.d("", "Anim Fraction: " + fraction);
//
//                if (!mTitlesVisible)
//                    fraction = 1.0f - fraction;
//
//                alpha = Math.round(fraction * 255);
//            }
//
//            v.setTitleAlpha(alpha);
//        }

        return v;
    }
}

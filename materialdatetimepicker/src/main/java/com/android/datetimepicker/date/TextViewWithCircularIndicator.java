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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.datetimepicker.R;

/**
 * A text view which, when pressed or activated, displays a blue circle around the text.
 */
public class TextViewWithCircularIndicator extends TextView {

    private static final int SELECTED_CIRCLE_ALPHA = 60;

    Paint mCirclePaint = new Paint();

    private final int mRadius;
    private final int mCircleColor;
    private final String mItemIsSelectedText;
    private final Rect mRect = new Rect();

    private boolean mDrawCircle;
    private boolean mDrawRectangle;

    private int mPadding;

    public TextViewWithCircularIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        mCircleColor = res.getColor(R.color.blue);
        mRadius = res.getDimensionPixelOffset(R.dimen.month_select_circle_radius);
        mItemIsSelectedText = context.getResources().getString(R.string.item_is_selected);
        mPadding = getResources().getDimensionPixelOffset(R.dimen.day_number_select_circle_radius);
        init();
    }

    private void init() {
        mCirclePaint.setFakeBoldText(true);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(mCircleColor);
        mCirclePaint.setTextAlign(Align.CENTER);
        mCirclePaint.setStyle(Style.FILL);
        mCirclePaint.setAlpha(SELECTED_CIRCLE_ALPHA);
    }

    public void setIndicatorColor(int color) {
        mCirclePaint.setColor(color);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_pressed},  // pressed
                new int[] { } // default
        };

        int[] colors = new int[] {
                color,
                getResources().getColor(R.color.date_picker_text_normal)
        };

        ColorStateList colorStateList = new ColorStateList(states, colors);
        setTextColor(colorStateList);
    }

    public void drawIndicator(boolean drawCircle) {
        mDrawCircle = drawCircle;
    }

    public void drawRectIndicator(boolean drawRect) {
        mDrawRectangle = drawRect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawCircle) {
            final int width = getWidth();
            final int height = getHeight();
            int radius = Math.min(width, height) / 2;
            canvas.drawCircle(width / 2, height / 2, radius, mCirclePaint);
        }
        if (mDrawRectangle) {
            getPaint().getTextBounds(getText().toString(), 0, getText().length(), mRect);
            int radius = Math.round(0.25f * mRect.height());

            int left = (getWidth()-mRect.width())/2 - mPadding;
            int top  = (getHeight()-mRect.height())/2 - (mPadding/2);
            int right = (getWidth()+mRect.width())/2 + mPadding;
            int bottom = (getHeight()+mRect.height())/2 + (mPadding/2);

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, radius, radius, mCirclePaint);
        }
    }

    @Override
    public CharSequence getContentDescription() {
        CharSequence itemText = getText();
        if (mDrawCircle) {
            return String.format(mItemIsSelectedText, itemText);
        } else {
            return itemText;
        }
    }
}

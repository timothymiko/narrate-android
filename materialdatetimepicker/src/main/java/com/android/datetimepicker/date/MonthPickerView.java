package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.datetimepicker.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 1/2/15.
 */
public class MonthPickerView extends ListView implements AdapterView.OnItemClickListener, DatePickerDialog.OnDateChangedListener {

    private final DatePickerController mController;
    private MonthAdapter mAdapter;
    private int mViewSize;
    private int mChildSize;
    private TextViewWithCircularIndicator mSelectedView;
    private Calendar mCalendar = Calendar.getInstance(Locale.getDefault());

    private int mSelectionColor = -1;

    public MonthPickerView(Context context, DatePickerController controller) {
        super(context);
        mController = controller;
        mController.registerOnDateChangedListener(this);
        ViewGroup.LayoutParams frame = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        setLayoutParams(frame);
        Resources res = context.getResources();
        mViewSize = res.getDimensionPixelOffset(R.dimen.date_picker_view_animator_height);
        mChildSize = res.getDimensionPixelOffset(R.dimen.year_label_height);
        setVerticalFadingEdgeEnabled(true);
        setFadingEdgeLength(mChildSize / 3);
        init(context);
        setOnItemClickListener(this);
        setSelector(new StateListDrawable());
        setDividerHeight(0);
        onDateChanged();
    }

    public void setSelectionColor(int color) {
        this.mSelectionColor = color;


    }

    private void init(Context context) {
        ArrayList<String> months = new ArrayList<String>();
        for (int month = 0; month < 12; month++) {
            mCalendar.set(Calendar.MONTH, month);
            months.add(mCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG,
                    Locale.getDefault()));
        }
        mAdapter = new MonthAdapter(context, R.layout.year_label_text_view, months);
        setAdapter(mAdapter);
    }

    private class MonthAdapter extends ArrayAdapter<String> {

        public MonthAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextViewWithCircularIndicator v = (TextViewWithCircularIndicator)
                    super.getView(position, convertView, parent);
            v.requestLayout();
            if ( mSelectionColor != -1 )
                v.setIndicatorColor(mSelectionColor);
            int month = position;
            boolean selected = mController.getSelectedDay().month == month;
            v.drawRectIndicator(selected);
            if (selected) {
                mSelectedView = v;
            }
            return v;
        }
    }

    public void postSetSelectionCentered(final int position) {
        postSetSelectionFromTop(position, mViewSize / 2 - mChildSize / 2);
    }

    public void postSetSelectionFromTop(final int position, final int offset) {
        post(new Runnable() {

            @Override
            public void run() {
                setSelectionFromTop(position, offset);
                requestLayout();
            }
        });
    }

    public int getFirstPositionOffset() {
        final View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    @Override
    public void onDateChanged() {
        mAdapter.notifyDataSetChanged();
        int month = mController.getSelectedDay().month;
        postSetSelectionCentered(month);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TextViewWithCircularIndicator clickedView = (TextViewWithCircularIndicator) view;
        if (clickedView != null) {
            if (clickedView != mSelectedView) {
                if (mSelectedView != null) {
                    mSelectedView.drawRectIndicator(false);
                    mSelectedView.requestLayout();
                }
                clickedView.drawRectIndicator(true);
                clickedView.requestLayout();
                mSelectedView = clickedView;
            }
            mController.onMonthSelected(i);
            mAdapter.notifyDataSetChanged();
        }
    }
}

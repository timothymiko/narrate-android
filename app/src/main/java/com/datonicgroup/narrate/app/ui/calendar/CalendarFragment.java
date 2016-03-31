package com.datonicgroup.narrate.app.ui.calendar;

import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.AccessibleDateAnimator;
import com.android.datetimepicker.date.DatePickerController;
import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.DayPickerView;
import com.android.datetimepicker.date.MonthAdapter;
import com.android.datetimepicker.date.MonthPickerView;
import com.android.datetimepicker.date.SimpleDayPickerView;
import com.android.datetimepicker.date.YearPickerView;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.predicates.EntryDateRangePredicate;
import com.datonicgroup.narrate.app.ui.base.BaseEntryFragment;
import com.datonicgroup.narrate.app.ui.entries.EntriesRecyclerAdapter;
import com.datonicgroup.narrate.app.ui.entries.ViewEntryActivity;
import com.datonicgroup.narrate.app.util.DateUtil;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import it.gmariotti.recyclerview.itemanimator.ScaleInOutItemAnimator;

/**
 * Created by timothymiko on 7/3/14.
 */
public class CalendarFragment extends BaseEntryFragment implements View.OnClickListener, DatePickerController, DayPickerView.DayPickerListener, RecyclerView.OnItemTouchListener, ViewPager.OnPageChangeListener {

    public static final String TAG = "Calendar";

    /**
     * Control
     */
    private HashSet<DatePickerDialog.OnDateChangedListener> mListeners = new HashSet<>();
    private EntriesRecyclerAdapter mAdapter;
    private GestureDetectorCompat mGestureDetector;
    private boolean mLandscape;
    private boolean mAddedToolbarView;

    private final String SAVE_DATE = "mCalendar";


    /**
     * Constants
     */
    private final int ANIMATION_DURATION = 300;
    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int ANIM_DAY_SELECTION = 0;
    private static final int ANIM_MONTH_SELECTION = 1;
    private static final int ANIM_YEAR_SELECTION = 2;

    /**
     * Data
     */
    private Calendar mCalendar = Calendar.getInstance(Locale.getDefault());
    private int mWeekStart = mCalendar.getFirstDayOfWeek();
    private int mMinYear = DEFAULT_START_YEAR;
    private int mMaxYear = DEFAULT_END_YEAR;
    private int mCurrentView = ANIM_DAY_SELECTION;
    private int mIndicatorsYear;
    private final MutableArrayList<Entry> mDisplayedEntries = new MutableArrayList<>();
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy");

    /**
     * Views
     */
    private AccessibleDateAnimator mAnimator;

    private SimpleDayPickerView mDayPickerView;
    private MonthPickerView mMonthPickerView;
    private YearPickerView mYearPickerView;

    private TextView mDayHeaderView;
    private TextView mDayOfWeekHeaderView;
    private TextView mMonthYearHeaderView;

    private TextView mToolbarTextView;
    private LinearLayout mHeaderLayout;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ScaleInOutItemAnimator mItemAnimator;


    public static CalendarFragment newInstance() {
        Log.d(TAG, "newInstance()");
        return new CalendarFragment();
    }

    public CalendarFragment() {
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        mCalendar.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        mGestureDetector = new GestureDetectorCompat(getActivity(), new GestureListener());

        if ( mLandscape ) {
            mainActivity.mViewPagerListeners.add(this);
        }

        if ( savedInstanceState != null ) {
            long date = savedInstanceState.getLong(SAVE_DATE, -1);

            if ( date > 0 )
                mCalendar.setTimeInMillis(date);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_calendar, container, false);

        // header views
        if ( mLandscape ) {

            mHeaderLayout = (LinearLayout) View.inflate(getActivity(), R.layout.calendar_toolbar_view, null);
            mToolbarTextView = (TextView) mHeaderLayout.findViewById(R.id.cal_header_text);
            mHeaderLayout.setOnClickListener(this);

            int position = 0;
            for ( position = 0; position < mainActivity.mViewPagerOrder.length; position++ )
                if ( mainActivity.mViewPagerOrder[position].equals(TAG) )
                    break;

            if ( mainActivity.mViewPager.getCurrentItem() == position ) {
                LinearLayout toolbar = (LinearLayout) mainActivity.getSupportActionBar().getCustomView();
                toolbar.addView(mHeaderLayout);
                mAddedToolbarView = true;
            }

        } else {

            mDayHeaderView = (TextView) findViewById(R.id.cal_header_day);
            mDayOfWeekHeaderView = (TextView) findViewById(R.id.cal_header_day_of_week);
            mMonthYearHeaderView = (TextView) findViewById(R.id.cal_header_month_year);

            findViewById(R.id.cal_header_month_layout).setOnClickListener(this);
        }

        // main content view
        mAnimator = (AccessibleDateAnimator) findViewById(R.id.animator);

        mDayPickerView = new SimpleDayPickerView(getActivity(), this);
        mMonthPickerView = new MonthPickerView(getActivity(), this);
        mYearPickerView = new YearPickerView(getActivity(), this);

        mDayPickerView.setStickyDisplaySelectedDate(true);

        if ( !mLandscape ) {
            mDayPickerView.setFixedHeight(getResources().getDimensionPixelOffset(R.dimen.calendar_height));
        }

        int calColor = getResources().getColor(R.color.section_calendar);
        int r = Color.red(calColor);
        int g = Color.green(calColor);
        int b = Color.blue(calColor);

        mDayPickerView.setSelectedColor(Color.argb(0x59, r, g, b));
        mMonthPickerView.setSelectionColor(Color.argb(0x59, r, g, b));
        mYearPickerView.setSelectionColor(Color.argb(0x59, r, g, b));

        mYearPickerView.useRectangleSelector(true);

        mDayPickerView.setTodayColor(Color.BLACK);
        mDayPickerView.setDayPickerListener(this);

        mAnimator.addView(mDayPickerView);
        mAnimator.addView(mMonthPickerView);
        mAnimator.addView(mYearPickerView);

        mDayPickerView.setSelected(true);
        mMonthPickerView.setSelected(false);
        mYearPickerView.setSelected(false);

        mAnimator.setDateMillis(mCalendar.getTimeInMillis());

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(ANIMATION_DURATION);
        mAnimator.setInAnimation(animation);

        Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(ANIMATION_DURATION);
        mAnimator.setOutAnimation(animation2);

        // list view
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnItemTouchListener(this);

        mAdapter = new EntriesRecyclerAdapter(mDisplayedEntries);
        mRecyclerView.setAdapter(mAdapter);

        mItemAnimator = new ScaleInOutItemAnimator(mRecyclerView);
        mRecyclerView.setItemAnimator(mItemAnimator);

        updateDisplay();
        updatePickers();

        return mRoot;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SAVE_DATE, mCalendar.getTimeInMillis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ( mLandscape ) {
            mainActivity.mViewPagerListeners.remove(this);

            LinearLayout toolbar = (LinearLayout) mainActivity.getSupportActionBar().getCustomView();
            toolbar.removeView(mHeaderLayout);
        }
    }

    @Override
    protected void onDataUpdated() {

        if ( mainActivity.entries != null &&
                mainActivity.entries.size() > 0 ) {
            updateIndicatorSet();
        }

        updateDisplay();
        updatePickers();
    }

    @Override
    public void onYearSelected(int year) {
        Log.d("", "onYearSelected(" + year + ")");

        adjustDayInMonthIfNeeded(mCalendar.get(Calendar.MONTH), year);
        mCalendar.set(Calendar.YEAR, year);

        updatePickers();
        updateDisplay();

        mDayPickerView.setSelected(true);
        mMonthPickerView.setSelected(false);
        mYearPickerView.setSelected(false);
        mAnimator.setDisplayedChild(ANIM_DAY_SELECTION);
        mCurrentView = ANIM_DAY_SELECTION;
    }

    @Override
    public void onMonthSelected(int month) {
        Log.d("", "onMonthSelected(" + month + ")");

        adjustDayInMonthIfNeeded(month, mCalendar.get(Calendar.YEAR));
        mCalendar.set(Calendar.MONTH, month);

        updatePickers();
        updateDisplay();

        mDayPickerView.setSelected(false);
        mMonthPickerView.setSelected(false);
        mYearPickerView.setSelected(true);
        mAnimator.setDisplayedChild(ANIM_YEAR_SELECTION);
        mCurrentView = ANIM_YEAR_SELECTION;
    }

    @Override
    public void onDayOfMonthSelected(int year, int month, int day) {
        Log.d("", "onDayOfMonthSelected(" + month + ", " + year + ", " + day + ")");

        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.DAY_OF_MONTH, day);

        updatePickers();
        updateDisplay();
    }

    @Override
    public void onDisplayMonthChanged(int month, int year) {
        Log.d("", "onDisplayMonthChanged(" + month + ", " + year + ")");

        mCalendar.set(Calendar.MONTH, month);
        mCalendar.set(Calendar.YEAR, year);
        mCalendar.set(Calendar.DAY_OF_MONTH, 1);

        updatePickers();
        updateDisplay();
    }

    // If the newly selected month / year does not contain the currently selected day number,
    // change the selected day number to the last day of the selected month or year.
    //      e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
    //      e.g. Switching from 2012 to 2013 when Feb 29, 2012 is se
    // lected -> Feb 28, 2013
    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = mCalendar.get(Calendar.DAY_OF_MONTH);
        int daysInMonth = Utils.getDaysInMonth(month, year);
        if (day > daysInMonth) {
            mCalendar.set(Calendar.DAY_OF_MONTH, daysInMonth);
        }
    }

    private void updateHeaderText() {

        if ( mLandscape ) {

            if ( mToolbarTextView != null )
                mToolbarTextView.setText(mDateFormat.format(mCalendar.getTime()));

        } else {
            String month = mCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            int year = mCalendar.get(Calendar.YEAR);

            // month & year text
            mMonthYearHeaderView.setText(String.format("%s %d", month, year));

            // week day name text
            mDayOfWeekHeaderView.setText(mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));

            // day number text
            boolean dayNumNeedsUpdating = !mDayHeaderView.getText().toString().equals(String.format("%02d", mCalendar.get(Calendar.DAY_OF_MONTH)));
            if (dayNumNeedsUpdating) {
                mDayHeaderView.setText(String.format("%02d", mCalendar.get(Calendar.DAY_OF_MONTH)));
                ObjectAnimator pulseAnimator = Utils.getPulseAnimator(mDayHeaderView, 0.9f,
                        1.05f);
                pulseAnimator.start();
            }
        }
    }

    @Override
    public void registerOnDateChangedListener(DatePickerDialog.OnDateChangedListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnDateChangedListener(DatePickerDialog.OnDateChangedListener listener) {
        mListeners.remove(listener);
    }

    private void updatePickers() {
        Iterator<DatePickerDialog.OnDateChangedListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDateChanged();
        }
    }

    private void updateDisplay() {

        if (mAdapter != null) {

            mAdapter.notifyItemRangeRemoved(0, mDisplayedEntries.size());
            mDisplayedEntries.clear();

            EntryDateRangePredicate pred = new EntryDateRangePredicate(mCalendar.getTimeInMillis(), mCalendar.getTimeInMillis() + DateUtil.DAY_IN_MILLISECONDS - 1);
            for ( int i = 0; i < mainActivity.entries.size(); i++ ) {
                Entry e = mainActivity.entries.get(i);
                if ( pred.apply(e) ) {
                    mDisplayedEntries.add(e);
                }

            }

            mAdapter.notifyItemRangeInserted(0, mDisplayedEntries.size());
        }

        updateHeaderText();
        updateIndicatorSet();
    }

    @Override
    public MonthAdapter.CalendarDay getSelectedDay() {
        return new MonthAdapter.CalendarDay(mCalendar);
    }

    @Override
    public int getFirstDayOfWeek() {
        return mWeekStart;
    }

    @Override
    public int getMinYear() {
        return mMinYear;
    }

    @Override
    public int getMaxYear() {
        return mMaxYear;
    }

    @Override
    public Calendar getMinDate() {
        return null;
    }

    @Override
    public Calendar getMaxDate() {
        return null;
    }

    @Override
    public void tryVibrate() {

    }

    private void updateIndicatorSet() {

        mIndicatorsYear = mCalendar.get(Calendar.YEAR);

        MutableArrayList<Entry> toFilter = new MutableArrayList<>();
        toFilter.addAll(mainActivity.entries);

        DateTime yearStart = DateTime.now();
        yearStart = yearStart.withDate(mCalendar.get(Calendar.YEAR), 1, 1);
        yearStart = yearStart.withTimeAtStartOfDay();

        DateTime yearEnd = yearStart.plusYears(1).withTimeAtStartOfDay();
        yearEnd = yearEnd.minusMillis(1);

        Log.d("", "Start Year: " + yearStart.toString());
        Log.d("", "End Year: " + yearEnd.toString());

        EntryDateRangePredicate pred = new EntryDateRangePredicate(yearStart.getMillis(), yearEnd.getMillis());
        toFilter.filter(pred);

        HashSet<Integer> mDays = new HashSet<>();
        for (int i = 0; i < toFilter.size(); i++) {
            Entry e = toFilter.get(i);
            int year = e.creationDate.get(Calendar.YEAR);
            int month = e.creationDate.get(Calendar.MONTH);
            int day = e.creationDate.get(Calendar.DAY_OF_MONTH);
            int key = year + (month * 10000) + (day * 1000000);

            if (!mDays.contains(key))
                mDays.add(key);
        }

        mDayPickerView.setIndicators(mDays);
        toFilter.clear();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.cal_header:
            case R.id.cal_header_month_layout:
                if (mCurrentView == ANIM_DAY_SELECTION) {
                    mDayPickerView.setSelected(false);
                    mMonthPickerView.setSelected(true);
                    mYearPickerView.setSelected(false);
                    mAnimator.setDisplayedChild(ANIM_MONTH_SELECTION);
                    mCurrentView = ANIM_MONTH_SELECTION;
                } else {
                    mDayPickerView.setSelected(true);
                    mMonthPickerView.setSelected(false);
                    mYearPickerView.setSelected(false);
                    mAnimator.setDisplayedChild(ANIM_DAY_SELECTION);
                    mCurrentView = ANIM_DAY_SELECTION;
                }
                break;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if ( mLandscape ) {
            if (mainActivity.mViewPagerOrder[position].equals(TAG)) {
                if (!mAddedToolbarView) {

                    if ( mHeaderLayout == null ) {
                        mHeaderLayout = (LinearLayout) View.inflate(getActivity(), R.layout.calendar_toolbar_view, null);
                        mToolbarTextView = (TextView) mHeaderLayout.findViewById(R.id.cal_header_text);
                        mHeaderLayout.setOnClickListener(this);
                    }

                    LinearLayout toolbar = (LinearLayout) mainActivity.getSupportActionBar().getCustomView();
                    toolbar.addView(mHeaderLayout);
                    mAddedToolbarView = true;
                }

            } else {
                LinearLayout toolbar = (LinearLayout) mainActivity.getSupportActionBar().getCustomView();
                toolbar.removeView(mHeaderLayout);
                mAddedToolbarView = false;
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

            if (view != null) {
                view.playSoundEffect(SoundEffectConstants.CLICK);

                int pos = mRecyclerView.getChildPosition(view);

                Intent i = new Intent(getActivity(), ViewEntryActivity.class);
                Bundle b = new Bundle();
                b.putParcelable(ViewEntryActivity.ENTRY_KEY, mDisplayedEntries.get(pos));
                i.putExtras(b);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.buildDrawingCache(true);
                    Bitmap drawingCache = view.getDrawingCache(true);
                    Bundle bundle = ActivityOptions.makeThumbnailScaleUpAnimation(view, drawingCache, 0, 0).toBundle();
                    getActivity().startActivity(i, bundle);
                } else {
                    startActivity(i);
                }
            }

            return super.onSingleTapUp(e);
        }
    }

    @Override
    protected void showLoader() {
        // This is ignored because this section does not have a loader.
    }
}

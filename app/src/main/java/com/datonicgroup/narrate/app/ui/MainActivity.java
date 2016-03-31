package com.datonicgroup.narrate.app.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.UserInfoDao;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.dataprovider.tasks.BaseTask;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.models.comparators.EntryAlphabetComparator;
import com.datonicgroup.narrate.app.models.comparators.EntryNewestDateComparator;
import com.datonicgroup.narrate.app.models.comparators.EntryOldestDateComparator;
import com.datonicgroup.narrate.app.models.comparators.EntryReverseAlphabetComparator;
import com.datonicgroup.narrate.app.models.predicates.EntryBookmarkedPredicate;
import com.datonicgroup.narrate.app.models.predicates.EntryDateRangePredicate;
import com.datonicgroup.narrate.app.models.predicates.EntryPhotoPredicate;
import com.datonicgroup.narrate.app.models.predicates.EntryTagsPredicate;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.ui.calendar.CalendarFragment;
import com.datonicgroup.narrate.app.ui.dialogs.MaterialDialogFragment;
import com.datonicgroup.narrate.app.ui.dialogs.ReviewDialog;
import com.datonicgroup.narrate.app.ui.dialogs.UpgradeDialog;
import com.datonicgroup.narrate.app.ui.entries.EntriesListFragment;
import com.datonicgroup.narrate.app.ui.entries.PhotosGridFragment;
import com.datonicgroup.narrate.app.ui.entryeditor.EditEntryActivity;
import com.datonicgroup.narrate.app.ui.passcode.AppLockManager;
import com.datonicgroup.narrate.app.ui.places.PlacesFragment;
import com.datonicgroup.narrate.app.ui.setup.SetupActivity;
import com.datonicgroup.narrate.app.ui.views.ControllableViewPager;
import com.datonicgroup.narrate.app.ui.views.FragmentPagerAdapter;
import com.datonicgroup.narrate.app.ui.views.TabBarView;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.GraphicsUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.datonicgroup.narrate.app.util.UpgradeUtil;
import com.jmedeisis.draglinearlayout.DragLinearLayout;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MainActivity extends BaseActivity implements View.OnClickListener {

    /**
     * Control
     */
    private final int SETUP_ACTIVITY_REQ = 100;

    private Handler handler = new Handler();
    private long mLastSyncTime;
    private long mLastSwapTime;

    private boolean mUpgrading;
    private boolean mActive;
    private String mCurrentPageTag;
    private int prevPosition;

    public HashSet<ViewPager.OnPageChangeListener> mViewPagerListeners = new HashSet<>();

    public FragmentPagerAdapter mPagerAdapter;

    /**
     * Data
     */
    public MutableArrayList<Entry> entries = new MutableArrayList<>();
    public String[] mViewPagerOrder;
    private int[] mSectionColors;

    /**
     * Views
     */
    public Toolbar mToolbar;
    private TabBarView mTabBar;
    public ControllableViewPager mViewPager;

    private UpgradeDialog mUpgradeDialog;

    /**
     * The setup process for this activity is a bit long and complicated because it is the first
     * activity when the user enters the experience. Here is an overview of what happens
     * and the order in which it happens:
     * <p/>
     * 1. Enabled Flurry analytics and Crashlytics monitoring
     * 2. Check if it is the user's first time opening the app and launch welcome screen if true
     * 3. Check if the user was previously using an older version of the app and some upgrade
     * code needs ot be run
     * 4. If all of the conditions are met, prompt the user to leave a rating on the Google Play
     * store
     * 5. Asynchronously hit Parse.com server to log the user in
     * 6. Setup the main ViewPager for displaying content
     * 7. Register local notifier receiver for easy inter-activity communication
     * 8. Run a task to check if there is an updated version available on the Google Play Store and
     * display a dialog if there is
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActive = true;

        // Check if user needs to complete setup process
        if (SettingsUtil.isFirstRun(this)) {

            Intent intent = new Intent(this, SetupActivity.class);
            startActivityForResult(intent, SETUP_ACTIVITY_REQ);

        }

        if (UpgradeUtil.doesUserNeedUpgrading()) {

            mUpgrading = true;
            mUpgradeDialog = new UpgradeDialog(this, new UpgradeDialog.UpgradeListener() {
                @Override
                public void onUpgradeComplete() {
                    mUpgrading = false;

                }
            });

        } else {

            if (!Settings.getShownReviewDialog() &&
                    Settings.getLoginCount() > 4 &&
                    (Settings.getEntryCount() > 3) &&
                    (DateTime.now().getMillis() - Settings.getJoinDate()) > (3 * DateUtil.DAY_IN_MILLISECONDS)) {

                Settings.setShownReviewDialog(true);
                new ReviewDialog().show(getSupportFragmentManager(), "ReviewDialogFragment");
            }

        }

        /**** SETUP UI HERE ****/

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.tab_bar);
            mTabBar = (TabBarView) getSupportActionBar().getCustomView().findViewById(R.id.tab_bar);

            // add a view behind status bar on KitKat builds
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                int statusBarHeight = 0;
                int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                RelativeLayout root = (RelativeLayout) findViewById(R.id.root);


                mStatusBarBg = new View(this);
                mStatusBarBg.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, statusBarHeight));
                root.addView(mStatusBarBg, 0);
            }

            mTabBar.setOnTabClickedListener(new TabBarView.OnTabClickedListener() {
                @Override
                public void onTabClicked(int index) {
                    mViewPager.setCurrentItem(index);
                }
            });

            mTabBar.setOnViewSwapListener(new DragLinearLayout.OnViewSwapListener() {

                @Override
                public void onSwap(View firstView, int firstPosition, View secondView, int secondPosition) {

                    // update the view pager with the new order
                    String temp = mViewPagerOrder[firstPosition];
                    mViewPagerOrder[firstPosition] = mViewPagerOrder[secondPosition];
                    mViewPagerOrder[secondPosition] = temp;

                    // update the section colors array with the new order
                    int tempColor = mSectionColors[firstPosition];
                    mSectionColors[firstPosition] = mSectionColors[secondPosition];
                    mSectionColors[secondPosition] = tempColor;

                }

                @Override
                public void onViewsSettled() {

                    // notify the view pager to update
                    mPagerAdapter.notifyDataSetChanged();

                    for (int i = 0; i < mViewPagerOrder.length; i++)
                        if (mViewPagerOrder[i].equals(mCurrentPageTag)) {
                            mViewPager.setCurrentItem(i, false);
                        }

                    // save the new arrangement to shared preferences
                    SharedPreferences.Editor mPrefsEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                    mPrefsEditor.putInt("main_toolbar_num_sections", mViewPagerOrder.length);
                    for (int i = 0; i < mViewPagerOrder.length; i++)
                        mPrefsEditor.putString("main_toolbar_order_" + i, mViewPagerOrder[i]);
                    mPrefsEditor.commit();

                }
            });
        }

        // setup viewpager
        mViewPager = (ControllableViewPager) findViewById(R.id.fragment_pager);
        mViewPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.default_gap));
        mViewPager.setOffscreenPageLimit(2);

        // retrieve viewpager order from shared preferences
        updateViewPagerOrder();

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (mTabBar != null) {
                    mTabBar.setOffset(positionOffset);
                    mTabBar.setSelectedTab(position);
                }

                Iterator<ViewPager.OnPageChangeListener> iter = mViewPagerListeners.iterator();
                while (iter.hasNext())
                    iter.next().onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {

                mCurrentPageTag = mViewPagerOrder[position];
                mViewPager.setOnMapFragment(mCurrentPageTag.equals(PlacesFragment.TAG));

                final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), mSectionColors[prevPosition], mSectionColors[position]);
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {

                        if (!mActive) {
                            colorAnimation.cancel();
                            return;
                        }

                        int color = (Integer) valueAnimator.getAnimatedValue();

                        setStatusBarColor(GraphicsUtil.darkenColor(color, 0.85f));
                        getToolbar().setBackgroundColor(color);
                        ( (ImageView) findViewById(R.id.fab_bg) ).getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                colorAnimation.setDuration(300);
                colorAnimation.start();

                prevPosition = position;

                Iterator<ViewPager.OnPageChangeListener> iter = mViewPagerListeners.iterator();
                while (iter.hasNext())
                    iter.next().onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Iterator<ViewPager.OnPageChangeListener> iter = mViewPagerListeners.iterator();
                while (iter.hasNext())
                    iter.next().onPageScrollStateChanged(state);
            }
        });

        // select the first tab as default
        if (mTabBar != null) {
            mTabBar.setSelectedTab(0);
            setStatusBarColor(GraphicsUtil.darkenColor(mSectionColors[0], 0.85f));
            getToolbar().setBackgroundColor(mSectionColors[0]);
        }

        findViewById(R.id.fab).setOnClickListener(this);
        ((ImageView) findViewById(R.id.fab_bg)).getDrawable().mutate().setColorFilter(mSectionColors[0], PorterDuff.Mode.SRC_ATOP);

        // register data observer from entries back-end
        getContentResolver().registerContentObserver(Contract.BASE_CONTENT_URI, true, mObserver);

        // register app passcode lock manager
        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(getApplication());

        /********/

        // register local notifier receiver
        IntentFilter mFilter = new IntentFilter(LocalContract.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onResume() {
        LogUtil.log(MainActivity.class.getSimpleName(), "onResume()");
        super.onResume();

        if (User.isAbleToSync() && ((System.currentTimeMillis() - mLastSyncTime) > DateUtil.MINUTE_IN_MILLISECONDS)) {
            mLastSyncTime = System.currentTimeMillis();
            SyncHelper.requestManualSync(User.getAccount());
        }

        refreshData(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    int placesIndex = 0;
                    for (placesIndex = 0; placesIndex < mViewPagerOrder.length; placesIndex++) {
                        if (mViewPagerOrder[placesIndex].equals(PlacesFragment.TAG)) {
                            break;
                        }
                    }

                    if (placesIndex < getSupportFragmentManager().getFragments().size()) {
                        PlacesFragment frag = (PlacesFragment)getSupportFragmentManager().getFragments().get(placesIndex);
                        frag.onLocationPermissionVerified();
                    }
                }
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // persist filter/sort string between screen rotations, dialogs, etc.
        if (savedInstanceState != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
            SharedPreferences.Editor editor = prefs.edit();
            String filter_sort = savedInstanceState.getString("filter_sort_key", null);

            if (filter_sort != null) {
                editor.putString("filter_sort_key", filter_sort);
                editor.apply();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save filter sort key through configuration changes
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        String key = null;
        if ((key = prefs.getString("filter_sort_key", null)) != null)
            outState.putString("filter_sort_key", key);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.log("MainActivity", "onDestroy()");
        mActive = false;

        handler.removeCallbacksAndMessages(null);
        handler = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        if (!Settings.getSaveSortFilter()) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();
            editor.remove("filter_sort_key");
            editor.apply();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SETUP_ACTIVITY_REQ:
                if (resultCode != RESULT_OK) {
                    finish();
                }
                break;
            default:
                if (mUpgrading)
                    mUpgradeDialog.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void refreshData(final BaseTask.Listener mListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (entries.size() > 0 && mListener != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onFinish(entries);
                        }
                    });
                }

                final MutableArrayList<Entry> data = EntryHelper.getAllEntries();
                Settings.setEntryCount(data.size());

                // process filter/sort
                final List<Predicate<Entry>> predicates = new ArrayList<Predicate<Entry>>();
                int sortId = -1;

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());

                try {
                    JSONObject json = new JSONObject(prefs.getString("filter_sort_key", null));

                    if (json.getBoolean("photo"))
                        predicates.add(new EntryPhotoPredicate());

                    if (json.getBoolean("bookmarked"))
                        predicates.add(new EntryBookmarkedPredicate());

                    if (json.getBoolean("date_range")) {
                        long start = json.getLong("date_start");
                        long end = json.getLong("date_end");

                        predicates.add(new EntryDateRangePredicate(start, end));
                    }

                    if (json.getBoolean("tags")) {
                        JSONArray tagsJson = json.getJSONArray("tags_list");
                        ArrayList<String> tags = new ArrayList<>();
                        for (int i = 0; i < tagsJson.length(); i++)
                            tags.add(tagsJson.getString(i));
                        predicates.add(new EntryTagsPredicate(tags));
                    }

                    sortId = json.getInt("sort");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {

                }

                if (!predicates.isEmpty())
                    data.filter(new Predicate<Entry>() {
                        @Override
                        public boolean apply(Entry entry) {
                            boolean result = true;
                            for (int i = 0; i < predicates.size(); i++) {
                                result = result && predicates.get(i).apply(entry);

                                if (!result)
                                    return false;
                            }

                            return result;
                        }
                    });

                Comparator<Entry> comparator = null;

                switch (sortId) {
                    case R.id.sort_alphabet:
                        comparator = new EntryAlphabetComparator();
                        break;
                    case R.id.sort_alphabet_reverse:
                        comparator = new EntryReverseAlphabetComparator();
                        break;
                    case R.id.sort_oldest:
                        comparator = new EntryOldestDateComparator();
                        break;
                    default:
                        comparator = new EntryNewestDateComparator();
                        break;
                }

                data.sort(comparator);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mActive) {

                            entries.clear();
                            entries.addAll(data);

                            if (mListener != null)
                                mListener.onFinish(entries);

                            Intent i = new Intent(LocalContract.ACTION);
                            i.putExtra(LocalContract.COMMAND, LocalContract.REFRESH_ENTRY_ADAPTERS);
                            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                        }
                    }
                });

            }
        }).start();
    }

    private ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            refreshData(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (Build.VERSION.SDK_INT > 15)
                super.onChange(selfChange, uri);
            refreshData(null);
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra(LocalContract.COMMAND);
            switch (command) {
                case LocalContract.REFRESH_DATA:
                    refreshData(null);
                    break;
                case LocalContract.SYNC_FINISHED:
                    refreshData(null);
                    break;
                case LocalContract.UPDATE_SECTIONS:
                    updateViewPagerOrder();
                    break;
            }
        }
    };

    private void updateViewPagerOrder() {

        mTabBar.removeAllViews();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        int size = prefs.getInt("main_toolbar_num_sections", -1);

        if (size == -1) {
            size = 4;
            editor.putInt("main_toolbar_num_sections", size);
            editor.apply();
        }

        mViewPagerOrder = new String[size];
        mSectionColors = new int[size];

        for (int i = 0; i < size; i++) {
            String def;
            switch (i) {
                case 1:
                    def = PhotosGridFragment.TAG;
                    break;
                case 2:
                    def = CalendarFragment.TAG;
                    break;
                case 3:
                    def = PlacesFragment.TAG;
                    break;
                default:
                    def = EntriesListFragment.TAG;
                    break;
            }

            String tag = prefs.getString("main_toolbar_order_" + i, null);

            if (tag == null) {
                tag = def;
                editor.putString("main_toolbar_order_" + i, tag);
                editor.apply();
            }

            mViewPagerOrder[i] = tag;

            View v = null;
            int color = 0;
            switch (tag) {
                case EntriesListFragment.TAG:
                    color = getResources().getColor(R.color.section_list);
                    v = View.inflate(this, R.layout.tab_view_entries, null);
                    break;
                case PhotosGridFragment.TAG:
                    color = getResources().getColor(R.color.section_photos);
                    v = View.inflate(this, R.layout.tab_view_photos, null);
                    break;
                case CalendarFragment.TAG:
                    color = getResources().getColor(R.color.section_calendar);
                    v = View.inflate(this, R.layout.tab_view_calendar, null);
                    break;
                case PlacesFragment.TAG:
                    color = getResources().getColor(R.color.section_places);
                    v = View.inflate(this, R.layout.tab_view_places, null);
                    break;
            }

            mSectionColors[i] = color;
            mTabBar.addView(v, i);
        }

        prevPosition = 0;
        mCurrentPageTag = mViewPagerOrder[0];

        mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager(), mViewPagerOrder);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setCurrentItem(0, false);

        int color = mSectionColors[0];

        setStatusBarColor(GraphicsUtil.darkenColor(color, 0.85f));
        getToolbar().setBackgroundColor(color);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent i = new Intent(this, EditEntryActivity.class);
                if ( mCurrentPageTag.equals(CalendarFragment.TAG) ) {
//                    Calendar cal = Calendar.getInstance(Locale.getDefault());
//                    cal.set(Calendar.YEAR, mCalendar.get(Calendar.YEAR));
//                    cal.set(Calendar.MONTH, mCalendar.get(Calendar.MONTH));
//                    cal.set(Calendar.DAY_OF_MONTH, mCalendar.get(Calendar.DAY_OF_MONTH));
//                    i.putExtra(EditEntryActivity.EXTRA_DATE, cal.getTimeInMillis());
                }
                startActivity(i);
                break;
        }
    }
}
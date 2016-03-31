package com.datonicgroup.narrate.app.ui.base;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.datonicgroup.narrate.app.ui.MainActivity;
import com.datonicgroup.narrate.app.ui.dialogs.FilterSortDialog;
import com.datonicgroup.narrate.app.ui.settings.MaterialPreferencesActivity;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 12/15/14.
 */
public abstract class BaseEntryFragment extends BaseFragment implements MenuItem.OnMenuItemClickListener {

    private int menuId;
    private String mFilterText;

    private MenuItem mSyncMenuitem;
    private MenuItem mFilterSortMenuItem;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

    public MainActivity mainActivity;

    private FilterSortDialog mFilterSortDialog;

    private View mSyncIndicator;
    private ProgressWheel wheel;
    private ImageView mSyncIndicatorIcon;

    protected View mNoEntriesGraphic;
    protected TextView mNoEntriesTextView;

    private static boolean mSyncing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mainActivity = (MainActivity) getActivity();
        mFilterSortDialog = new FilterSortDialog();

        IntentFilter mFilter = new IntentFilter(LocalContract.ACTION);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        mSyncMenuitem = menu.findItem(R.id.sync_data);
        MenuItem syncIndicator = menu.findItem(R.id.sync_indicator);

        if ( User.isAbleToSync() ) {
            mSyncMenuitem.setVisible(true);
            syncIndicator.setVisible(true);

            String currentlySyncing = getString(R.string.currently_syncing);
            if (SettingsUtil.getSyncStatus().equals(currentlySyncing))
                mSyncMenuitem.setTitle(R.string.cancel_sync);
            else
                mSyncMenuitem.setTitle(getString(R.string.sync_data));
        } else {
            mSyncMenuitem.setVisible(false);
            syncIndicator.setVisible(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        if ( menuId != 0 ) {
            inflater.inflate(menuId, menu);

            // setup searching
            mSearchMenuItem = menu.findItem(R.id.search);
            if ( mSearchMenuItem != null ) {
                mSearchView = (SearchView) mSearchMenuItem.getActionView();

                mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(final String s) {
                        BaseEntryFragment.this.mFilterText = s.trim().toLowerCase(Locale.getDefault());
                        filter(new Predicate<Entry>() {

                            private String entryText;
                            private String title;
                            private List<String> tags;

                            @Override
                            public boolean apply(Entry item) {

                                entryText = item.text != null ? item.text.toLowerCase(Locale.getDefault()).trim() : "";
                                title = item.title != null ? item.title.toLowerCase(Locale.getDefault()).trim() : "";
                                tags = item.tags != null ? item.tags : new ArrayList<String>();

                                if (entryText.contains(mFilterText) || title.contains(mFilterText)) {
                                    return true;
                                } else {
                                    if (tags.size() > 0) {
                                        Iterator<String> iter = tags.iterator();
                                        while (iter.hasNext()) {
                                            if (iter.next().toLowerCase(Locale.getDefault()).contains(mFilterText)) {
                                                return true;
                                            }

                                        }
                                    }
                                }

                                return false;
                            }
                        });
                        onDataUpdated();
                        return true;
                    }
                });
                mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
                    @Override
                    public boolean onClose() {
                        filter(null);
                        onDataUpdated();
                        return false;
                    }
                });
            }

            if ( mFilterSortMenuItem != null ) {
                // setup filtering and sorting
                mFilterSortMenuItem = menu.findItem(R.id.filter_sort);
            }


        }

        inflater.inflate(R.menu.base_entry, menu);

        for (int i = 0; i < menu.size(); i++)
            menu.getItem(i).setOnMenuItemClickListener(this);

        MenuItem sync = menu.findItem(R.id.sync_indicator);

        mSyncIndicator = View.inflate(GlobalApplication.getAppContext(), R.layout.action_bar_sync_indicator, null);
        mSyncIndicatorIcon = (ImageView) mSyncIndicator.findViewById(R.id.icon);
        wheel = (ProgressWheel) mSyncIndicator.findViewById(R.id.wheel);
        sync.setActionView(mSyncIndicator);

        if ( mSyncing )
            wheel.spin();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);

        if ( wheel != null )
            wheel.stopSpinning();
    }

    public void setMenuItems(int resId) {
        this.menuId = resId;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch ( intent.getStringExtra(LocalContract.COMMAND) ) {
                case LocalContract.REFRESH_DATA:
                    showLoader();
                    break;
                case LocalContract.REFRESH_ENTRY_ADAPTERS:
                    onDataUpdated();
                    break;
                case LocalContract.SYNC_START:
                    mSyncing = true;

                    if ( wheel != null && !wheel.isSpinning() )
                        wheel.spin();

                    if ( mSyncMenuitem != null )
                        mSyncMenuitem.setTitle(getString(R.string.cancel_sync));
                    break;
                case LocalContract.SYNC_FINISHED:
                    mSyncing = false;

                    if ( wheel != null )
                        wheel.stopSpinning();

                    if ( mSyncMenuitem != null )
                        mSyncMenuitem.setTitle(getString(R.string.sync_data));
                    break;
            }
        }
    };

    protected abstract void showLoader();

    protected abstract void onDataUpdated();

    public void filter(Predicate<Entry> predicate) {
        mainActivity.entries.filter(predicate);
    }

    public void sort(Comparator<Entry> comparator) {
        mainActivity.entries.sort(comparator);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(getActivity(), MaterialPreferencesActivity.class));
                return true;
            case R.id.filter_sort:
                if ( mFilterSortDialog.isAdded() )
                    mFilterSortDialog.dismiss();

                mFilterSortDialog.show(getFragmentManager(), "FilterDialog");
                return true;
            case R.id.sync_data:
                if ( item.getTitle().equals(getString(R.string.cancel_sync)) ) {
                    SyncHelper.cancelPendingActiveSync(User.getAccount());
                } else {
                    SyncHelper.requestManualSync(User.getAccount());
                }
                return true;
        }
        return false;
    }

    protected void showEmptyGraphic(boolean show) {
        if (show) {
            if (mNoEntriesGraphic.getVisibility() == View.GONE) {
                mNoEntriesGraphic.setScaleX(0);
                mNoEntriesGraphic.setScaleY(0);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(mNoEntriesGraphic, "scaleX", 0f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(mNoEntriesGraphic, "scaleY", 0f, 1f);

                AnimatorSet anim = new AnimatorSet();
                anim.setInterpolator(new OvershootInterpolator(1.5f));
                anim.setStartDelay(250);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mNoEntriesGraphic.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                anim.playTogether(scaleX, scaleY);
                anim.start();
            }
        } else {

            if (mNoEntriesGraphic.getVisibility() == View.VISIBLE) {
                ObjectAnimator alpha = ObjectAnimator.ofFloat(mNoEntriesGraphic, "alpha", 0f);
                alpha.setInterpolator(new DecelerateInterpolator());
                alpha.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mNoEntriesGraphic.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                alpha.start();
            }
        }
    }
}

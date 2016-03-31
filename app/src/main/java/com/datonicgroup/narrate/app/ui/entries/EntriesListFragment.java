package com.datonicgroup.narrate.app.ui.entries;


import android.animation.Animator;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.MainActivity;
import com.datonicgroup.narrate.app.ui.SectionListAdapter;
import com.datonicgroup.narrate.app.ui.base.BaseEntryFragment;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class EntriesListFragment extends BaseEntryFragment implements RecyclerView.OnItemTouchListener, ActionMode.Callback {

    public static final String TAG = "Entries";

    @Override
    public String getIdentifier() {
        return TAG;
    }

    /**
     * Control
     */
    private Handler mHandler = new Handler();
    private boolean mListAnimating;
    private boolean mUpdateDataOnAnimComplete;

    /**
     * Data
     */
    private SectionListAdapter mSectionListAdapter;
    private EntriesRecyclerAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    /**
     * Views
     */
    private RecyclerView mRecyclerView;
    private ProgressWheel mLoadingIndicator;
    private SearchView mSearchView;
    private GestureDetectorCompat mGestureDetector;

    private ActionMode mActionMode;

    public static EntriesListFragment getInstance() {
        return new EntriesListFragment();
    }

    public EntriesListFragment() {
        LogUtil.log(TAG, "New Entries List Fragment!");
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMenuItems(R.menu.entries_list);
        mGestureDetector = new GestureDetectorCompat(getActivity(), new GestureListener());
    }

    @Override
    protected void showLoader() {
        mLoadingIndicator.setAlpha(0f);
        mLoadingIndicator.animate().alpha(1f).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
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
        }).start();
    }

    @Override
    protected void onDataUpdated() {

        if ( !mListAnimating ) {
            if (mLoadingIndicator.getVisibility() == View.VISIBLE) {
                mLoadingIndicator.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoadingIndicator.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).start();
            }

            showHideNoEntriesGraphicIfNeeded();

            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();
        } else {
            mUpdateDataOnAnimComplete = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_entries, container, false);

        mLoadingIndicator = (ProgressWheel) mRoot.findViewById(R.id.loading_indicator);

        mNoEntriesGraphic = findViewById(R.id.no_entries_graphic);
        mNoEntriesTextView = (TextView) findViewById(R.id.no_entries_text);
        mNoEntriesTextView.setText(getString(R.string.no_entries).toLowerCase());

        setupListView();

        return mRoot;
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.log(TAG, "onStart()");
        mAdapter.updateTimeFormat();
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtil.log(TAG, "onPause()");

        if (mActionMode != null)
            mActionMode.finish();

    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.log(TAG, "onStop()");
    }

    private void setupListView() {
        LogUtil.log(TAG, "setupListView()");
        mRecyclerView = (RecyclerView) mRoot.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
//        mRecyclerView.setItemAnimator(new ScaleInOutItemAnimator(mRecyclerView));
        mRecyclerView.addOnItemTouchListener(this);
//        mRecyclerView.getItemAnimator().setSupportsChangeAnimations(true);


        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new EntriesRecyclerAdapter(((MainActivity) getActivity()).entries);
        mSectionListAdapter = new SectionListAdapter(getActivity(), R.layout.entries_month_header, R.id.month_header, mAdapter);
        mRecyclerView.setAdapter(mSectionListAdapter);

        mRecyclerView.setLayoutAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mListAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mListAnimating = false;

                if ( mUpdateDataOnAnimComplete ) {
                    mUpdateDataOnAnimComplete = false;
                    onDataUpdated();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mRecyclerView.setVisibility(View.INVISIBLE);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setVisibility(View.VISIBLE);
                mRecyclerView.scheduleLayoutAnimation();
            }
        }, 250);
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
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        mActionMode.getMenuInflater().inflate(R.menu.entries_contextual, menu);

        menu.findItem(R.id.delete).getIcon().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.SRC_ATOP);

        // disable viewpager swiping for now
        mainActivity.mViewPager.setSwipeEnabled(false);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                List<Integer> mIndices = mAdapter.getSelectedItems();
                final List<Entry> mEntries = new ArrayList<>();

                for (int i = 0; i < mIndices.size(); i++)
                    mEntries.add(mainActivity.entries.get(mIndices.get(i)));

                for (int i = 0; i < mEntries.size(); i++) {
                    mainActivity.entries.remove(mEntries.get(i));
                    mAdapter.notifyItemRemoved(mIndices.get(i));
                }

                mActionMode.finish();
                showHideNoEntriesGraphicIfNeeded();

                if (mEntries.size() > 0) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            DataManager dm = DataManager.getInstance();
                            List<Entry> _items = mEntries;

                            Iterator<Entry> iter = _items.iterator();
                            while (iter.hasNext())
                                dm.delete(iter.next());
                        }
                    }).start();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.mActionMode = null;
        mAdapter.clearSelections();

        // re-enable swiping
        mainActivity.mViewPager.setSwipeEnabled(true);
    }

    private void toggleSelectedItem(int pos) {
        mAdapter.toggleSelection(pos);
        String title = getString(R.string.selected_count, mAdapter.getSelectedItemCount());
        mActionMode.setTitle(title);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

            if (mActionMode != null) {
                toggleSelectedItem(mRecyclerView.getChildPosition(view));
            } else {
                if ( view != null ) {
                    view.playSoundEffect(SoundEffectConstants.CLICK);

                    int pos = mRecyclerView.getChildPosition(view);

                    Intent i = new Intent(getActivity(), ViewEntryActivity.class);
                    Bundle b = new Bundle();
                    b.putParcelable(ViewEntryActivity.ENTRY_KEY, ((MainActivity) getActivity()).entries.get(pos - mAdapter.getSectionOffset(pos)));
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
            }

            return super.onSingleTapUp(e);
        }

        public void onLongPress(MotionEvent e) {
            View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
            if (mActionMode != null) {
                return;
            }
            // Start the CAB using the ActionMode.Callback defined above
            mActionMode = mainActivity.mToolbar.startActionMode(EntriesListFragment.this);
            int idx = mRecyclerView.getChildPosition(view);
            toggleSelectedItem(idx);
            super.onLongPress(e);
        }
    }

    private void showHideNoEntriesGraphicIfNeeded() {
        if ( mainActivity.entries == null || mainActivity.entries.isEmpty() ) {
            showEmptyGraphic(true);
        } else if ( mainActivity.entries != null && !mainActivity.entries.isEmpty() ) {
            showEmptyGraphic(false);
        }
    }

}

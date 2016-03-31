package com.datonicgroup.narrate.app.ui.entries;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.predicates.EntryPhotoPredicate;
import com.datonicgroup.narrate.app.ui.base.BaseEntryFragment;
import com.datonicgroup.narrate.app.ui.views.AutoGridView;
import com.pnikosis.materialishprogress.ProgressWheel;

import it.gmariotti.recyclerview.itemanimator.ScaleInOutItemAnimator;

/**
 * Created by timothymiko on 12/14/14.
 */
public class PhotosGridFragment extends BaseEntryFragment
        implements RecyclerEntriesGridAdapter.OnItemClickListener,
        RecyclerEntriesGridAdapter.OnFavoriteClickedListener,
        Handler.Callback,
        MenuItem.OnMenuItemClickListener {

    public static final String TAG = "Photos";

    public static final short REQUEST_CODE = 25380;

    /**
     * Views
     */
    protected AutoGridView gridView;
    protected ProgressWheel mProgressWheel;

    private final MutableArrayList<Entry> mPhotoEntries = new MutableArrayList<>();
    private final EntryPhotoPredicate mPredicate = new EntryPhotoPredicate();

    /**
     * Misc
     */
    protected int itemSize;
    protected String query;
    protected RecyclerEntriesGridAdapter mAdapter;
    private GridLayoutManager gridLayoutManager;

    private Handler mhandler = new Handler();
    private Handler mBackgroundHandler;


    private boolean mListAnimating;
    private boolean mUpdateDataOnAnimComplete;

    public static PhotosGridFragment newInstance() {
        Log.d(TAG, "newInstance()");
        return new PhotosGridFragment();
    }

    public PhotosGridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMenuItems(R.menu.entries_grid);

        HandlerThread handlerThread = new HandlerThread("EntriesGrid.background");
        handlerThread.start();
        mBackgroundHandler = new Handler(handlerThread.getLooper(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_entries_grid, container, false);

        if (mRoot != null) {
            gridView = (AutoGridView) mRoot.findViewById(R.id.listview);

            gridLayoutManager = new GridLayoutManager(mRoot.getContext(), 2);
            gridView.setLayoutManager(gridLayoutManager);
            gridView.setHasFixedSize(true);
//            gridView.getItemAnimator().setSupportsChangeAnimations(true);
            gridView.setItemAnimator(new ScaleInOutItemAnimator(gridView));

            gridView.setLayoutAnimationListener(new Animation.AnimationListener() {
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

            mProgressWheel = (ProgressWheel) mRoot.findViewById(R.id.loader);
            if (mProgressWheel != null) {
                mProgressWheel.setAlpha(0.0f);
            }

            mNoEntriesGraphic = findViewById(R.id.no_entries_graphic);
            mNoEntriesTextView = (TextView) findViewById(R.id.no_entries_text);
            mNoEntriesTextView.setText(getString(R.string.no_photos).toLowerCase());

            setupAutoSizeGridView();

            mhandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAdapter = new RecyclerEntriesGridAdapter(mPhotoEntries, itemSize);
                    gridView.setAdapter(mAdapter);
                    mAdapter.setOnItemClickListener(PhotosGridFragment.this);
                    mAdapter.setOnFavoriteClickedListener(PhotosGridFragment.this);
                    gridView.scheduleLayoutAnimation();
                }
            }, 250);
        }

        return mRoot;
    }

    @Override
    public void onResume() {
        super.onResume();
        onDataUpdated();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setInsets(gridView, 0, view.getResources().getDimensionPixelSize(R.dimen.gridview_bottom_padding));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && mAdapter != null) {
            gridView.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    protected void onDataUpdated() {

        if (!mListAnimating) {
            hideLoader();

            mPhotoEntries.clear();
            for ( int i = 0; i < mainActivity.entries.size(); i++ ) {
                Entry e = mainActivity.entries.get(i);
                if ( mPredicate.apply(e) ) {
                    mPhotoEntries.add(e);
                }
            }

            if ( mPhotoEntries == null || mPhotoEntries.isEmpty() ) {
                showEmptyGraphic(true);
            } else if ( mPhotoEntries != null && !mPhotoEntries.isEmpty() ) {
                showEmptyGraphic(false);
            }

            if (mAdapter != null)
                mAdapter.notifyDataSetChanged();

        } else {
            mUpdateDataOnAnimComplete = true;
        }
    }

    protected void showLoader() {
        mProgressWheel.animate().alpha(1.0f).setDuration(300).start();
    }

    private void hideLoader() {
        mProgressWheel.animate().alpha(0.0f).setDuration(300).start();
    }

    protected void setupAutoSizeGridView() {
        final ViewTreeObserver vto = gridView.getViewTreeObserver();
        if (vto != null) {
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                int lastWidth = -1;

                @Override
                public void onGlobalLayout() {
                    int width = gridView.getWidth() - gridView.getPaddingLeft() - gridView.getPaddingRight();
                    if (width == lastWidth || width <= 0) {
                        return;
                    }

                    // Compute number of columns
                    int maxItemWidth = gridView.getDefaultCellWidth();
                    int numColumns = 1;
                    while (true) {
                        if (width / numColumns > maxItemWidth) {
                            ++numColumns;
                        } else {
                            break;
                        }
                    }

                    itemSize = width / numColumns;
                    if (mAdapter != null) {
                        mAdapter.setItemSize(itemSize);
                    }
                    gridLayoutManager.setSpanCount(numColumns);

                }
            });
        }
    }

    private void setInsets(View view, int extraHeight, int extraBottom) {
        int otherPadding = view.getResources().getDimensionPixelSize(R.dimen.gridview_other_padding);
        int bottomPadding = otherPadding + extraBottom;
        int statusbarHeight = 0;
        view.setPadding(
                otherPadding,
                statusbarHeight + extraHeight + otherPadding,
                otherPadding,
                bottomPadding
        );
    }

    @Override
    public void onItemClick(View view, int position) {
        Intent i = new Intent(getActivity(), ViewEntryActivity.class);
        i.putExtra(ViewEntryActivity.ENTRY_KEY, mPhotoEntries.get(position));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.buildDrawingCache(true);
            Bitmap drawingCache = view.getDrawingCache(true);
            Bundle bundle = ActivityOptions.makeThumbnailScaleUpAnimation(view, drawingCache, 0, 0).toBundle();
            getActivity().startActivity(i, bundle);
        } else {
            startActivity(i);
        }
    }

    @Override
    public void onFavoriteButtonClicked(final Entry entry, int position) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                entry.starred = !entry.starred;
                DataManager.getInstance().save(entry, false);
            }
        });
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}

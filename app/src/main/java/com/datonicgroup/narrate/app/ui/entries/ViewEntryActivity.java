package com.datonicgroup.narrate.app.ui.entries;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.ui.dialogs.PlaceOnAMapDialog;
import com.datonicgroup.narrate.app.ui.entryeditor.EditEntryActivity;
import com.datonicgroup.narrate.app.ui.views.BookmarkView;
import com.datonicgroup.narrate.app.ui.views.ColorFilterImageView;
import com.datonicgroup.narrate.app.ui.views.ListeningScrollView;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.GraphicsUtil;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import in.uncod.android.bypass.Bypass;

public class ViewEntryActivity extends BaseActivity {

    /**
     * Data
     */
    private Entry mEntry;
    private Bitmap mBitmap;
    private int mStatusColor;

    private int mColorAccent;
    private int mColorPrimary;

    /**
     * Views
     */
    private RelativeLayout mTitleLayout;
    private TextView mTitle;
    private LinearLayout mDateTimeLayout;
    private TextView mDateTimeTextView;
    private LinearLayout mLocationLayout;
    private TextView mLocation;
    private LinearLayout mTagsLayout;
    private TextView mTags;
    private TextView mText;
    private Toolbar mToolbar;
    private ImageView mImage;
    private ListeningScrollView mScrollView;
    private View fab;
    private BookmarkView mBookmarkView;
    private TextView mEditedText;
    private RelativeLayout mImageTitleLayout;
    private TextView mImageTitleView;
    private ColorFilterImageView mDateTimeImageView;
    private ColorFilterImageView mLocationImageView;
    private ColorFilterImageView mTagImageView;
    private View mToolbarButtonShadow;

    /**
     * Other
     */
    public static final String TAG = "ViewEntryActivity";
    public static final String ENTRY_KEY = "EntryKey";
    public static final String ACTION_BAR_COLOR_KEY = "ActionBarColor";
    public static final int REQUEST_EDIT_ENTRY = 1;
    public static final int RESULT_ENTRY_DELETED = -2;
    public static final String PARCELABLE_ENTRY = "ParcelableEntry";

    private final String TITLE_REGEX = "^.*?[\\.!\\?](?:\\s|$)";
    private AsyncTask<Void, Void, Void> mSaveTask;
    private AsyncTask<Void, Void, Entry> mUpdateTask;

    public ViewEntryActivity() {
        // Required empty public constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mColorPrimary = getResources().getColor(R.color.primary);
        mColorAccent = getResources().getColor(R.color.accent);

        if (savedInstanceState != null)
            mEntry = savedInstanceState.getParcelable(PARCELABLE_ENTRY);

        if ( mEntry == null )
            mEntry = getIntent().getParcelableExtra(ENTRY_KEY);

        setContentView(R.layout.activity_view_entry);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.inflateMenu(R.menu.view_entry);
//        mFavoriteOption = mToolbar.getMenu().findItem(R.id.favorite);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.edit:

                        Intent intent = new Intent(ViewEntryActivity.this, EditEntryActivity.class);
                        Bundle b = new Bundle();
                        b.putParcelable(ViewEntryActivity.ENTRY_KEY, mEntry);
                        intent.putExtras(b);
                        startActivityForResult(intent, REQUEST_EDIT_ENTRY);
                        return true;
                    case R.id.map:
                        Intent i = new Intent(ViewEntryActivity.this, PlaceOnAMapDialog.class);
                        i.putExtra("title", mEntry.placeName);
                        i.putExtra("location", new LatLng(mEntry.latitude, mEntry.longitude));
                        startActivity(i);
                        return true;
                    case R.id.share:
                        StringBuilder s = new StringBuilder();
                        s.append(mEntry.title);
                        s.append("\n\n");
                        s.append(getString(R.string.date));
                        s.append(": ");
                        s.append(getDateString());
                        if ( mEntry.hasLocation ) {
                            s.append('\n');
                            s.append(getString(R.string.location));
                            s.append(": ");
                            s.append(getLocationString());
                        }
                        String tags = EntryHelper.getTags(mEntry);
                        if ( mEntry.tags.size() > 0 && tags != null ) {
                            s.append('\n');
                            s.append(getString(R.string.tags));
                            s.append(": ");
                            s.append(tags);
                        }
                        s.append("\n\n");
                        s.append(mEntry.text);

                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, s.toString());
                        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.abc_shareactionprovider_share_with)));
                        return true;
                }
                return false;
            }
        });

        mText.setMovementMethod(LinkMovementMethod.getInstance());

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            int statusBarHeight = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            RelativeLayout root = (RelativeLayout) findViewById(R.id.root);

            mStatusBarBg = new View(this);
            mStatusBarBg.setId(R.id.status_bar_bg);

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, statusBarHeight);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mStatusBarBg.setLayoutParams(lp);

            root.addView(mStatusBarBg);
//            mStatusBarBg.bringToFront();

            lp = (RelativeLayout.LayoutParams) mToolbar.getLayoutParams();
            lp.addRule(RelativeLayout.BELOW, R.id.status_bar_bg);
            mToolbar.setLayoutParams(lp);

            lp = (RelativeLayout.LayoutParams) mToolbarButtonShadow.getLayoutParams();
            lp.height = lp.height + statusBarHeight;
            mToolbarButtonShadow.setLayoutParams(lp);
        }

        findViewById(R.id.fab_selector).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEntry.starred = !mEntry.starred;
                mBookmarkView.setFilled(mEntry.starred);
                saveEntry();
            }
        });

        setupEntryInfo();
    }

    @Override
    protected void assignViews() {
        super.assignViews();

        mTitleLayout = (RelativeLayout) findViewById(R.id.title_layout);
        mTitle = (TextView) findViewById(R.id.title);
        mDateTimeLayout = (LinearLayout) findViewById(R.id.time);
        mDateTimeTextView = (TextView) findViewById(R.id.time_text);
        mLocationLayout = (LinearLayout) findViewById(R.id.location);
        mLocation = (TextView) findViewById(R.id.location_text);
        mTagsLayout = (LinearLayout) findViewById(R.id.tags_layout);
        mTags = (TextView) findViewById(R.id.tags_text);
        mText = (TextView) findViewById(R.id.text);
        mImage = (ImageView) findViewById(R.id.image);
        mScrollView = (ListeningScrollView) findViewById(R.id.scrollView);
        fab = findViewById(R.id.fab);
        mBookmarkView = (BookmarkView) findViewById(R.id.bookmark);
        mEditedText = (TextView) findViewById(R.id.edited_text);
        mImageTitleLayout = (RelativeLayout) findViewById(R.id.image_title_layout);
        mImageTitleView = (TextView) findViewById(R.id.image_title);
        mDateTimeImageView = (ColorFilterImageView) findViewById(R.id.date_time_image);
        mLocationImageView = (ColorFilterImageView) findViewById(R.id.location_icon);
        mTagImageView = (ColorFilterImageView) findViewById(R.id.tags_icon);
        mToolbarButtonShadow = findViewById(R.id.toolbar_button_shadow);

        mScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                mScrollView.setScrollY(0);
                mScrollView.getViewTreeObserver().removeOnScrollChangedListener(this);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(PARCELABLE_ENTRY, mEntry);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBitmap != null)
            mBitmap.recycle();
    }

    private String getDateString() {
        SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
        SimpleDateFormat tf = new SimpleDateFormat(DateUtil.getTimeFormatString(Settings.getTwentyFourHourTime()), Locale.getDefault());

        return df.format(mEntry.creationDate.getTime()) + getString(R.string.at) + tf.format(mEntry.creationDate.getTime());
    }

    private String getLocationString() {
        if (mEntry.placeName != null && mEntry.placeName.length() > 0)
            return mEntry.placeName;
        else
            return mEntry.latitude + ", " + mEntry.longitude;
    }

    private void setupEntryInfo() {
        mDateTimeTextView.setText(getDateString());

        SimpleDateFormat mEditedDateFormat = new SimpleDateFormat(" MMM d");
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(mEntry.modifiedDate);
        mEditedText.setText(getString(R.string.edited) + mEditedDateFormat.format(cal.getTime()));

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;

        // check if the entry has a photo and if the file exists
        if (mEntry.photos.size() > 0 && (mBitmap = BitmapFactory.decodeFile(mEntry.photos.get(0).path, options)) != null ) {

            if ( mBitmap.getWidth() > 4096 || mBitmap.getHeight() > 4096 ) {
                float ratio = (float) mBitmap.getHeight() / (float) mBitmap.getWidth();
                int width = mBitmap.getWidth();
                int height = 0;

                if ( width > 4096 ) {
                    width = 4096;
                    height = Math.round(width * ratio);
                } else {
                    height = 4096;
                    width = Math.round(height / ratio);
                }

                mBitmap = Bitmap.createScaledBitmap(mBitmap, width, height, false);
            }

            mImage.setImageBitmap(mBitmap);
            mImage.setVisibility(View.VISIBLE);
            mImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ViewEntryActivity.this, ViewPhotoActivity.class);
                    intent.putExtra("photo", mEntry.photos.get(0));
                    startActivity(intent);
                }
            });

            mTitleLayout.setVisibility(View.INVISIBLE);
            mImageTitleLayout.setVisibility(View.VISIBLE);
            mImageTitleLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (mImageTitleLayout.getBottom() > 0 &&
                            mImageTitleLayout.getHeight() > 0 &&
                            fab.getHeight() > 0) {

                        int y = mImageTitleLayout.getBottom() - fab.getHeight() / 2;
                        fab.setY(y);

                        mImageTitleLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
            ( (ImageView) findViewById(R.id.fab_bg) ).getDrawable().mutate().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);

            mImageTitleView.setText(mEntry.title);



            View detailsLayout = findViewById(R.id.details);
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) detailsLayout.getLayoutParams();
            rlp.addRule(RelativeLayout.BELOW, R.id.image_title_layout);
            detailsLayout.setLayoutParams(rlp);

            findViewById(R.id.shadow1).setVisibility(View.VISIBLE);
            mToolbarButtonShadow.setVisibility(View.VISIBLE);

            mDateTimeImageView.setColor(mColorPrimary);
            mLocationImageView.setColor(mColorPrimary);
            mTagImageView.setColor(mColorPrimary);

        } else {

            mTitleLayout.setVisibility(View.VISIBLE);
            mImageTitleLayout.setVisibility(View.INVISIBLE);
            mTitleLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (mTitleLayout.getBottom() > 0 &&
                            mTitleLayout.getHeight() > 0 &&
                            fab.getHeight() > 0) {

                        int y = mTitleLayout.getBottom() - fab.getHeight() / 2;
                        fab.setY(y);

                        mTitleLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
            ( (ImageView) findViewById(R.id.fab_bg) ).getDrawable().mutate().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.MULTIPLY);

            mTitle.setText(mEntry.title);

            mImage.setImageBitmap(null);
            if (mBitmap != null && !mBitmap.isRecycled())
                mBitmap.recycle();

            mImage.setVisibility(View.GONE);

            View detailsLayout = findViewById(R.id.details);
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) detailsLayout.getLayoutParams();
            rlp.addRule(RelativeLayout.BELOW, R.id.title_layout);
            detailsLayout.setLayoutParams(rlp);

            findViewById(R.id.shadow1).setVisibility(View.GONE);
            mToolbarButtonShadow.setVisibility(View.GONE);

            mDateTimeImageView.setColor(mColorAccent);
            mLocationImageView.setColor(mColorAccent);
            mTagImageView.setColor(mColorAccent);
        }

        final boolean hasPhotos = mEntry.photos.size() > 0;

        int transparent = getResources().getColor(R.color.transparent);
        mToolbar.setBackgroundColor(transparent);

        mStatusColor = GraphicsUtil.darkenColor(getResources().getColor(R.color.primary), 0.85f);
        setStatusBarColor(hasPhotos ? transparent : mStatusColor);

        mScrollView.setOnScrollListener(new ListeningScrollView.OnScrollListener() {
            @Override
            public void onScrollChanged(int dx, int dy) {

                float ratio = 0;
                if (hasPhotos)
                    ratio = Math.min((float) mScrollView.getScrollY() / (float) mImage.getHeight(), 1.0f);
                else
                    ratio = Math.min((float) mScrollView.getScrollY() / (float) (mTitleLayout.getHeight() - mToolbar.getHeight()), 1.0f);

                int alpha = Math.round(ratio * 255);
                int baseColor = getResources().getColor(R.color.primary);
                int color = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
                mToolbar.setBackgroundColor(color);

                if (hasPhotos) {
                    color = Color.argb(alpha, Color.red(mStatusColor), Color.green(mStatusColor), Color.blue(mStatusColor));
                    setStatusBarColor(color);

                    mImage.setScrollY(-Math.round(ratio / 2 * mImage.getHeight()));
                }
            }
        });

        List<View> mDetailViews = new ArrayList<>();
        mDetailViews.add(mDateTimeLayout);

        mToolbar.getMenu().findItem(R.id.map).setVisible(mEntry.hasLocation);
        if (mEntry.hasLocation) {
            mLocation.setText(getLocationString());
            mLocationLayout.setVisibility(View.VISIBLE);
            mDetailViews.add(mLocationLayout);
        } else {
            mLocationLayout.setVisibility(View.GONE);
        }

        String mTagString = EntryHelper.getTags(mEntry);

        if (mTagString != null) {
            mTags.setText(mTagString);
            mTagsLayout.setVisibility(View.VISIBLE);
            mDetailViews.add(mTagsLayout);
        } else {
            mTagsLayout.setVisibility(View.GONE);
        }

        mBookmarkView.setFilled(mEntry.starred);

        if (hasPhotos) {
            if ( mDetailViews.isEmpty() ) {
                mTitleLayout.setPadding(mTitleLayout.getPaddingLeft(),
                        mTitleLayout.getPaddingTop(),
                        mTitleLayout.getPaddingRight(),
                        0);
            } else {
                mTitleLayout.setPadding(mTitleLayout.getPaddingLeft(),
                        mTitleLayout.getPaddingTop(),
                        mTitleLayout.getPaddingRight(),
                        getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin));
            }
        }

        mText.setText(mEntry.text);
        new AsyncTask<Void, Void, CharSequence>() {

            @Override
            protected CharSequence doInBackground(Void... params) {
                Bypass bypass = new Bypass();
                return bypass.markdownToSpannable(mEntry.text);
            }

            @Override
            protected void onPostExecute(CharSequence charSequence) {
                super.onPostExecute(charSequence);
                mText.setText(charSequence);
            }
        }.execute();

        mText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void saveEntry() {
        if (mSaveTask != null)
            mSaveTask.cancel(true);

        mSaveTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                DataManager.getInstance().save(mEntry, false);
                return null;
            }
        };
        mSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateEntry() {
        if (mUpdateTask == null || mUpdateTask.getStatus() == AsyncTask.Status.FINISHED) {
            mUpdateTask = new AsyncTask<Void, Void, Entry>() {
                @Override
                protected Entry doInBackground(Void... params) {
                    return EntryHelper.getEntry(mEntry.uuid);
                }

                @Override
                protected void onPostExecute(Entry entry) {
                    super.onPostExecute(entry);
                    mEntry = entry;
                    setupEntryInfo();
                }
            };
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_ENTRY:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mEntry = data.getExtras().getParcelable(PARCELABLE_ENTRY);
                        setupEntryInfo();
                        break;
                    case RESULT_ENTRY_DELETED:
                        finish();
                        break;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}

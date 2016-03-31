package com.datonicgroup.narrate.app.ui.entryeditor;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileMetadata;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.sync.GoogleDriveSyncService;
import com.datonicgroup.narrate.app.dataprovider.tasks.RetrievePlacesTask;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.datonicgroup.narrate.app.ui.RoundedCornerTransformation;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.ui.dialogs.DeleteConfirmationDialog;
import com.datonicgroup.narrate.app.ui.dialogs.LocationNameDialog;
import com.datonicgroup.narrate.app.ui.dialogs.LocationPickerDialog;
import com.datonicgroup.narrate.app.ui.dialogs.MaterialDialogFragment;
import com.datonicgroup.narrate.app.ui.dialogs.NearbyPlacesDialog;
import com.datonicgroup.narrate.app.ui.dialogs.OptionsDialog;
import com.datonicgroup.narrate.app.ui.dialogs.TagsPickerDialog;
import com.datonicgroup.narrate.app.ui.entries.ViewEntryActivity;
import com.datonicgroup.narrate.app.ui.entries.ViewPhotoActivity;
import com.datonicgroup.narrate.app.ui.passcode.AppLockManager;
import com.datonicgroup.narrate.app.ui.views.ColorFilterImageView;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.GraphicsUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.PermissionsUtil;
import com.google.android.gms.maps.model.LatLng;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

/**
 * Created by timothymiko on 1/5/15.
 */
public class EditEntryActivity extends BaseActivity implements View.OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener, TagsPickerDialog.TagsChangeListener, NearbyPlacesDialog.PlacesListener {

    /**
     * Control
     */
    private final String PARCELABLE_ENTRY_KEY = "EntryKey";
    public static final String EXTRA_DATE = "InitialDateExtra";

    private final int REQUEST_LOCATION_CODE = 100;
    private final int REQUEST_SELECT_PHOTO = 101;
    private final int REQUEST_TAKE_PHOTO = 102;

    private final int IMAGE_MAX_SIZE = 2560;
    private final Handler mHandler = new Handler();

    private boolean mIsEditing;
    private long lastSaveTime;
    private boolean mIsNewEntry = true;
    private boolean mNewEntrySaved = false;
    private boolean mSavingEntry = false;
    private boolean mUpdatedPhoto = false;

    /**
     * Data
     */
    private Entry entry;

    private final List<String> mPhotoOptionsList = new ArrayList<>();
    private final List<Integer> mPhotoIconsList = new ArrayList<>();

    private final List<String> mLocationOptionsList = new ArrayList<>();
    private final List<Integer> mLocationIconsList = new ArrayList<>();

    /**
     * Views
     */
    private EditText mTitleView;
    private EditText mTextView;
    private ImageView mTimeView;
    private ImageView mDateView;
    private ImageView mPhotoView;
    private ColorFilterImageView mLocationView;
    private ColorFilterImageView mTagView;

    private TimePickerDialog mTimePickerDialog;
    private DatePickerDialog mDatePickerDialog;
    private TagsPickerDialog mTagsPickerDialog;
    private OptionsDialog mPhotoOptionsDialog;
    private OptionsDialog mLocationOptionsDialog;
    private LocationNameDialog mNameDialog;
    private NearbyPlacesDialog mNearbyPlacesDialog;

    private ValueAnimator mLocationLoaderAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_entry);
        setTitle("");

        if (getIntent().getBooleanExtra("from_widget", false)) {
            AppLockManager.getInstance().enableDefaultAppLockIfAvailable(getApplication());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColor(GraphicsUtil.darkenColor(getResources().getColor(R.color.primary), 0.85f));
        }

        entry = new Entry();
        entry.creationDate = Calendar.getInstance();
        entry.modifiedDate = entry.creationDate.getTimeInMillis();
        entry.title = "";
        entry.text = "";
        entry.photos = new ArrayList<>();
        entry.tags = new ArrayList<>();

        Bundle b = getIntent().getExtras();
        if (b == null)
            b = savedInstanceState;

        String action = getIntent().getAction();
        String type = getIntent().getType();

        if (action != null && action.equals(Entry.ACTION_NEW_ENTRY)) {

            String data = b.getString(Entry.EXTRA_DATA, null);

            if (data != null) {

                try {
                    JSONObject json = new JSONObject(data);

                    String title = json.has(Entry.EXTRA_TITLE) ? json.getString(Entry.EXTRA_TITLE) : null;
                    String text = json.has(Entry.EXTRA_TEXT) ? json.getString(Entry.EXTRA_TEXT) : null;
                    long date = json.has(Entry.EXTRA_DATE_TIME) ? json.getLong(Entry.EXTRA_DATE_TIME) : 0;
                    String photo = json.has(Entry.EXTRA_PHOTO) ? json.getString(Entry.EXTRA_PHOTO) : null;
                    double latitude = json.has(Entry.EXTRA_LATITUDE) ? json.getDouble(Entry.EXTRA_LATITUDE) : 0;
                    double longitude = json.has(Entry.EXTRA_LONGITUDE) ? json.getDouble(Entry.EXTRA_LONGITUDE) : 0;
                    String placeName = json.has(Entry.EXTRA_PLACE_NAME) ? json.getString(Entry.EXTRA_PLACE_NAME) : null;
                    JSONArray tags = json.has(Entry.EXTRA_TAGS) ? json.getJSONArray(Entry.EXTRA_TAGS) : null;
                    boolean bookmark = json.has(Entry.EXTRA_BOOKMARK) ? json.getBoolean(Entry.EXTRA_BOOKMARK) : false;

                    if (title != null)
                        entry.title = title;

                    if (text != null)
                        entry.text = text;

                    if (date > 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(date * DateUtil.SECOND_IN_MILLISECONDS);
                        entry.creationDate = cal;
                    }

                    if (photo != null) {
                        // todo
                    }

                    if (latitude != 0 && longitude != 0) {
                        entry.latitude = latitude;
                        entry.longitude = longitude;

                        if (placeName != null) {
                            entry.placeName = placeName;
                        }
                    }

                    if (tags != null) {
                        try {
                            ArrayList<String> tagsArray = new ArrayList<>();
                            for (int i = 0; i < tags.length(); i++)
                                tagsArray.add(tags.getString(i));

                            entry.tags = tagsArray;
                        } catch (Exception e) {
                            e.printStackTrace();
                            entry.tags = new ArrayList<>();
                        }
                    }

                    entry.starred = bookmark;

                    mIsEditing = true;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (type != null && (type.equals("text/plain") || type.startsWith("image/"))) {

            // handle text shared with Narrate
            String sharedText = b.getString(Intent.EXTRA_TEXT);

            if (sharedText != null && type.equals("text/plain")) {
                entry.text = sharedText;
                mTextView.setText(entry.text);
                mIsEditing = true;
            }

            // handle a photo shared with Narrate
            if (type.startsWith("image/")) {
                Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {

                    // Update UI to reflect image being shared
                    saveImageUri(imageUri);

                    String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                    Cursor cur = new CursorLoader(this, imageUri, orientationColumn, null, null, null).loadInBackground();
                    int orientation = -1;
                    if (cur != null && cur.moveToFirst()) {
                        orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                    }
                    Log.d("", "Orientation: " + orientation);


                    try {
                        final File f = new File(entry.photos.get(0).path);
                        BitmapFactory.Options o2 = new BitmapFactory.Options();
                        FileInputStream fis = new FileInputStream(f);
                        Bitmap image = BitmapFactory.decodeStream(fis, null, o2);
                        fis.close();

                        image = rotateBitmap(image, orientation);

                        FileOutputStream fos = new FileOutputStream(f);
                        image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        image.recycle();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    mIsEditing = true;
                }
            }

        } else {
            if (b != null) {
                if (b.getParcelable(PARCELABLE_ENTRY_KEY) != null) {
                    entry = b.getParcelable(PARCELABLE_ENTRY_KEY);
                    mIsNewEntry = false;
                    mIsEditing = true;
                } else {
                    long date = b.getLong(EXTRA_DATE, -1);
                    if (date > 0) {
                        entry.creationDate.setTimeInMillis(date);
                    }
                }
            }
        }

        setupOptions();

        if (Settings.getAutomaticLocation() && !mIsEditing) {
            autoRetrieveLocation();
        }
    }

    @Override
    protected void assignViews() {
        super.assignViews();

        mTitleView = (EditText) findViewById(R.id.title);
        mTextView = (EditText) findViewById(R.id.text);
        mTimeView = (ImageView) findViewById(R.id.time);
        mDateView = (ImageView) findViewById(R.id.date);
        mPhotoView = (ImageView) findViewById(R.id.photo);
        mLocationView = (ColorFilterImageView) findViewById(R.id.location);
        mTagView = (ColorFilterImageView) findViewById(R.id.tag);

        mTimeView.setOnClickListener(this);
        mDateView.setOnClickListener(this);
        mPhotoView.setOnClickListener(this);
        mLocationView.setOnClickListener(this);
        mTagView.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsEditing && !mSavingEntry)
            save();
    }

    @Override
    public void onBackPressed() {
        if (mIsEditing)
            onSaveClick();
        else
            finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PARCELABLE_ENTRY_KEY, entry);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void setupOptions() {

        // title & entry text
        mTitleView.setText(entry.title);
        mTextView.setText(entry.text);

        mTitleView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
//                if ((DateTime.now().getMillis() - lastSaveTime) > (DateUtil.SECOND_IN_MILLISECONDS * 20)) {
//                    save();
//                }
            }
        });

        mTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
//                if ((DateTime.now().getMillis() - lastSaveTime) > (DateUtil.SECOND_IN_MILLISECONDS * 20)) {
//                    save();
//                }
            }
        });

        // time & date
        Calendar date = entry.creationDate;

        mTimePickerDialog = TimePickerDialog.
                newInstance(this, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE),
                        Settings.getTwentyFourHourTime()
                );

        mDatePickerDialog = DatePickerDialog.newInstance(this, date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DAY_OF_MONTH)
        );

        // photo
        mPhotoOptionsList.clear();
        mPhotoOptionsList.add(getString(R.string.take_photo));
        mPhotoOptionsList.add(getString(R.string.select_gallery));
        mPhotoIconsList.clear();
        mPhotoIconsList.add(R.drawable.take_photo_icon);
        mPhotoIconsList.add(R.drawable.select_from_gallery);
        mPhotoOptionsDialog = OptionsDialog.newInstance(mPhotoOptionsList, mPhotoIconsList, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (entry.photos.size() > 0) {
                    switch (position) {
                        case 0:
                            viewPhoto();
                            break;
                        case 1:
                            takePhotoUsingCamera();
                            break;
                        case 2:
                            selectPhotoFromGallery();
                            break;
                        case 3:
                            mPhotoView.setImageResource(R.drawable.ic_photo);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    DataManager.getInstance().delete(entry.photos.get(0), entry.googleDrivePhotoFileId);
                                    entry.photos.clear();
                                    entry.googleDrivePhotoFileId = null;
                                }
                            }).start();
                            updatePhotoOptionsDialog(false);
                            break;
                    }
                } else {
                    if (position == 0)
                        takePhotoUsingCamera();
                    else
                        selectPhotoFromGallery();
                }
            }
        });
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (entry.photos.size() > 0) {
                    File file = new File(entry.photos.get(0).path);
                    Glide.with(GlobalApplication.getAppContext())
                            .load(file)
                            .transform(new RoundedCornerTransformation(EditEntryActivity.this, R.dimen.entry_photo_rnd_corner))
                            .placeholder(R.drawable.ic_photo)
                            .signature(new StringSignature(String.valueOf(file.lastModified())))
                            .into(mPhotoView);

                    updatePhotoOptionsDialog(true);
                }
            }
        }, 250);

        // location
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LocalContract.ACTION));
        mLocationOptionsList.clear();
        mLocationOptionsList.add(getString(R.string.select_location));
        mLocationOptionsList.add(getString(R.string.show_nearby_places));
        mLocationOptionsList.add(getString(R.string.name_this_location));
        mLocationOptionsList.add(getString(R.string.remove_location));
        mLocationIconsList.clear();
        mLocationIconsList.add(R.drawable.select_location);
        mLocationIconsList.add(R.drawable.nearby_places);
        mLocationIconsList.add(R.drawable.ic_pencil);
        mLocationIconsList.add(R.drawable.ic_menu_delete);
        mLocationOptionsDialog = OptionsDialog.newInstance(mLocationOptionsList, mLocationIconsList, new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        showLocationPickerDialog();
                        break;
                    case 1:
                        mNearbyPlacesDialog.show(getSupportFragmentManager(), "NearbyPlacesDialog");
                        break;
                    case 2:
                        mNameDialog.show(getSupportFragmentManager(), "LocationNameDialog");
                        break;
                    case 3:
                        entry.hasLocation = false;
                        entry.latitude = 0;
                        entry.longitude = 0;
                        entry.placeName = null;
                        updateLocationIcon();
                        break;
                }
            }
        });

        mNameDialog = new LocationNameDialog();
        mNameDialog.setSaveListener(new LocationNameDialog.SaveListener() {
            @Override
            public void onSave(String text) {
                entry.placeName = text;
                mNameDialog.setName(entry.placeName);
            }
        });
        mNameDialog.setName(entry.placeName);

        mNearbyPlacesDialog = new NearbyPlacesDialog();
        mNearbyPlacesDialog.setPlacesListener(this);

        LatLng location = null;

        if (entry.hasLocation) {
            location = new LatLng(entry.latitude, entry.longitude);
        } else {
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            if (PermissionsUtil.checkAndRequest(this, Manifest.permission.ACCESS_FINE_LOCATION, 1, R.string.permission_explanation_location, null)) {
                LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);

                    if (lastKnownLocation != null)
                        location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                } catch (SecurityException e) {

                }
            }
        }

        if (location != null) {
            mNearbyPlacesDialog.setLatLng(location);
        }
        updateLocationIcon();

        // tags
        mTagsPickerDialog = TagsPickerDialog.newInstance(this, entry.tags);
        updateTagsIcon();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                onSaveClick();
                return true;
            case android.R.id.home:
                if (mIsEditing)
                    onSaveClick();
                else
                    finish();
                return true;
            case R.id.delete:
                onDeleteClick();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    final Uri imageUri = data.getData();
                    saveImageUri(imageUri);

//                    mIsEditing = true;
//                    save();

                    if (entry.photos.size() > 0) {
                        File file = new File(entry.photos.get(0).path);
                        Glide.with(GlobalApplication.getAppContext())
                                .load(file)
                                .transform(new RoundedCornerTransformation(this, R.dimen.entry_photo_rnd_corner))
                                .placeholder(R.drawable.ic_photo)
                                .signature(new StringSignature(String.valueOf(file.lastModified())))
                                .into(mPhotoView);

                        updatePhotoOptionsDialog(true);
                    }
                }
                break;
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    LogUtil.log(EditEntryActivity.class.getSimpleName(), "Received image from camera!");
                    entry.photos.clear();
                    entry.photos.addAll(PhotosDao.getPhotosForEntry(entry));

//                    mIsEditing = true;
//                    save();

                    if (entry.photos.size() > 0) {
                        File file = new File(entry.photos.get(0).path);
                        Glide.with(GlobalApplication.getAppContext())
                                .load(file)
                                .transform(new RoundedCornerTransformation(this, R.dimen.entry_photo_rnd_corner))
                                .placeholder(R.drawable.ic_photo)
                                .signature(new StringSignature(String.valueOf(file.lastModified())))
                                .into(mPhotoView);

                        updatePhotoOptionsDialog(true);

                        mUpdatedPhoto = true;
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                final File f = new File(entry.photos.get(0).path);

                                Bitmap image;

                                BitmapFactory.Options o = new BitmapFactory.Options();
                                o.inJustDecodeBounds = true;

                                FileInputStream fis = new FileInputStream(f);
                                BitmapFactory.decodeStream(fis, null, o);
                                fis.close();

                                int scale = 1;
                                if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                                    scale = (int) Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                                            (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                                }

                                //Decode with inSampleSize
                                BitmapFactory.Options o2 = new BitmapFactory.Options();
                                o2.inSampleSize = scale;
                                fis = new FileInputStream(f);
                                image = BitmapFactory.decodeStream(fis, null, o2);
                                fis.close();

                                ExifInterface ei = new ExifInterface(entry.photos.get(0).path);
                                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                                switch (orientation) {
                                    case ExifInterface.ORIENTATION_ROTATE_90:
                                        image = rotateBitmap(image, 90);
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_180:
                                        image = rotateBitmap(image, 180);
                                        break;
                                    case ExifInterface.ORIENTATION_ROTATE_270:
                                        image = rotateBitmap(image, 270);
                                        break;
                                }

                                FileOutputStream fos = new FileOutputStream(f);
                                image.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                                if ( Settings.getSavePhotos() ) {
                                    MediaStore.Images.Media.insertImage(getContentResolver(), image, null, null);
                                }

                                image.recycle();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mUpdatedPhoto = true;
                                    save();
                                }
                            });
                        }
                    }).start();
                }
                break;
            case 1004:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.time:
                mTimePickerDialog.show(getFragmentManager(), "TimePickerDialog");
                break;
            case R.id.date:
                mDatePickerDialog.show(getFragmentManager(), "DatePickerDialog");
                break;
            case R.id.photo:
                mPhotoOptionsDialog.show(getSupportFragmentManager(), "PhotoOptionsDialog");
                break;
            case R.id.location:
                if (entry.hasLocation)
                    mLocationOptionsDialog.show(getSupportFragmentManager(), "LocationOptionsDialog");
                else
                    showLocationPickerDialog();
                break;
            case R.id.tag:
                mTagsPickerDialog.show(getSupportFragmentManager(), "TagPickerDialog");
                break;
        }
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        entry.creationDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        entry.creationDate.set(Calendar.MINUTE, minute);
//        save();
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        entry.creationDate.set(Calendar.YEAR, year);
        entry.creationDate.set(Calendar.MONTH, monthOfYear);
        entry.creationDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
//        save();
    }

    @Override
    public void onTagsChanged(List<String> tags) {
        entry.tags = tags;
        updateTagsIcon();
//        save();
    }

    @Override
    public void onPlaceSelected(String place) {
        entry.placeName = place;
        mNameDialog.setName(place);
//        save();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    LatLng location = null;

                    try {
                        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (lastKnownLocation != null)
                            location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                        if (location != null) {
                            mNearbyPlacesDialog.setLatLng(location);
                        }

                    } catch (SecurityException e) {

                    }

                } else {
                    // permission denied
                    // YOUR CANCEL CODE
                }

                updateLocationIcon();

                break;
            }
            case 2: {
                break;
            }
            case 3: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    autoRetrieveLocation();
                } else {
                    final ImageView mLocationLoader = (ImageView) findViewById(R.id.location_loader);
                    mLocationLoaderAnim.cancel();
                    mLocationLoader.setVisibility(View.GONE);
                }
                break;
            }
            case 4: {
                takePhotoUsingCamera();
                break;
            }
            case 5: {
                selectPhotoFromGallery();
                break;
            }

        }
    }

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(LocalContract.COMMAND).equals(LocalContract.UPDATE_ENTRY_LOCATION)) {
                LatLng location = intent.getParcelableExtra("location");

                if (location != null) {
                    entry.hasLocation = true;
                    entry.latitude = location.latitude;
                    entry.longitude = location.longitude;
                    entry.placeName = null;

                    updateLocationIcon();
                    mNearbyPlacesDialog.setLatLng(new LatLng(entry.latitude, entry.longitude));

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!mLocationOptionsDialog.isAdded()) {
                                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                    transaction.add(mLocationOptionsDialog, "LocationOptionsDialog");
                                    transaction.commitAllowingStateLoss();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

//                    mIsEditing = true;
//                    save();
                }
            }
        }
    };

    private void selectPhotoFromGallery() {
        if (PermissionsUtil.checkAndRequest(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 5, R.string.permission_explanation_write_storage, null)) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO);
        }
    }

    private void takePhotoUsingCamera() {

        if (PermissionsUtil.checkAndRequest(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 4, R.string.permission_explanation_write_storage, null)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = PhotosDao.getFileForPhoto(entry.uuid);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // Error occurred while creating the File
                }

                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
            }
        }
    }

    private void viewPhoto() {
        Intent intent = new Intent(EditEntryActivity.this, ViewPhotoActivity.class);
        intent.putExtra("photo", entry.photos.get(0));
        startActivity(intent);
    }

    private void showLocationPickerDialog() {

        Intent i = new Intent(EditEntryActivity.this, LocationPickerDialog.class);

        if (entry.hasLocation) {
            LatLng location = new LatLng(entry.latitude, entry.longitude);
            i.putExtra("location", location);
        } else {
            if (PermissionsUtil.checkAndRequest(this, Manifest.permission.ACCESS_FINE_LOCATION, 2, R.string.permission_explanation_location, null)) {
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                try {
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);

                    if (lastKnownLocation != null) {
                        LatLng location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        i.putExtra("location", location);
                    }
                } catch (SecurityException e) {

                }
            }
        }

        startActivityForResult(i, REQUEST_LOCATION_CODE);
    }

    private Bitmap rotateBitmap(Bitmap bmp, float degrees) {
        Bitmap target = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bmp.getWidth() / 2, bmp.getHeight() / 2);
        canvas.drawBitmap(bmp, matrix, new Paint());
        return target;
    }

    private void saveImageUri(Uri imageUri) {
        if (imageUri != null) {
            try {

                // We must resize the image to make sure it is under 2048px in both dimensions
                // Otherwise, we will have trouble uploading and displaying the image on lower
                // end devices.

                //Decode image size
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;

                InputStream is = getContentResolver().openInputStream(imageUri);
                BitmapFactory.decodeStream(is, null, o);
                is.close();

                int scale = 1;
                if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                    scale = (int) Math.pow(2, (int) Math.ceil(Math.log(IMAGE_MAX_SIZE /
                            (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                }

                //Decode with inSampleSize
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = scale;
                is = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(is, null, o2);
                is.close();

                try {
                    final File file = PhotosDao.getFileForPhoto(entry.uuid);

                    if (file.exists()) {
                        file.delete();
                        file.createNewFile();
                    }

                    FileOutputStream fos = new FileOutputStream(file);
                    selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                    Photo photo = new Photo();
                    photo.name = file.getName();
                    photo.path = file.getAbsolutePath();
                    photo.uuid = entry.uuid;
                    entry.photos.clear();
                    entry.photos.add(photo);
                    mUpdatedPhoto = true;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            save();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LogUtil.log(EditEntryActivity.class.getSimpleName(), "Received image shared with Narrate!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updatePhotoOptionsDialog(boolean hasPhoto) {
        if (hasPhoto) {
            mPhotoOptionsList.clear();
            mPhotoOptionsList.add(getString(R.string.view_photo));
            mPhotoOptionsList.add(getString(R.string.take_photo));
            mPhotoOptionsList.add(getString(R.string.select_gallery));
            mPhotoOptionsList.add(getString(R.string.remove_photo));
            mPhotoIconsList.clear();
            mPhotoIconsList.add(R.drawable.ic_photo);
            mPhotoIconsList.add(R.drawable.take_photo_icon);
            mPhotoIconsList.add(R.drawable.select_from_gallery);
            mPhotoIconsList.add(R.drawable.ic_menu_delete);
        } else {
            mPhotoOptionsList.clear();
            mPhotoOptionsList.add(getString(R.string.take_photo));
            mPhotoOptionsList.add(getString(R.string.select_gallery));
            mPhotoIconsList.clear();
            mPhotoIconsList.add(R.drawable.take_photo_icon);
            mPhotoIconsList.add(R.drawable.select_from_gallery);
        }

        mPhotoOptionsDialog.notifyDataSetChanged();
    }

    private void updateLocationIcon() {
        if (entry.hasLocation) {
            int pumpkin = getResources().getColor(R.color.pumpkin);
            if (mLocationView.getColor() != pumpkin)
                mLocationView.setColor(pumpkin);
        } else {
            int white = getResources().getColor(R.color.off_white);
            if (mLocationView.getColor() != white)
                mLocationView.setColor(white);
        }
    }

    public void updateTagsIcon() {
        if (entry.tags.size() > 0) {
            int blue = getResources().getColor(R.color.belize_hole);
            if (mTagView.getColor() != blue)
                mTagView.setColor(blue);
        } else {
            int white = getResources().getColor(R.color.off_white);
            if (mTagView.getColor() != white)
                mTagView.setColor(white);
        }
    }

    private void onSaveClick() {
        save();

        if (!mIsNewEntry) {
            Intent data = new Intent();
            data.putExtra(ViewEntryActivity.PARCELABLE_ENTRY, entry);
            setResult(Activity.RESULT_OK, data);
        }

        finish();
    }

    private void onDeleteClick() {

        if (mIsEditing) {
            final DialogInterface.OnClickListener mDeleteListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mIsEditing) {
                        setResult(ViewEntryActivity.RESULT_ENTRY_DELETED);
                        delete();
                    }

                    finish();
                }
            };

            final MaterialDialogFragment dialog = DeleteConfirmationDialog.newInstance(mDeleteListener);
            dialog.show(getSupportFragmentManager(), "DeleteConfirmationDialog");
        } else
            finish();
    }

    private void save() {

        mSavingEntry = true;
        lastSaveTime = DateTime.now().getMillis();

        entry.title = mTitleView.getText().toString();
        entry.text = mTextView.getText().toString();

        entry.modifiedDate = Calendar.getInstance(Locale.getDefault()).getTimeInMillis();

        final boolean createNewEntry = mIsNewEntry && !mNewEntrySaved;
        new Thread(new Runnable() {
            @Override
            public void run() {

                Entry _entry = entry;

                Entry dbEntry = EntryHelper.getEntry(_entry.uuid);
                if (dbEntry != null) {
                    if (dbEntry.googleDriveFileId != null) {
                        entry.googleDriveFileId = dbEntry.googleDriveFileId;
                        _entry.googleDriveFileId = dbEntry.googleDriveFileId;
                    }

                    if (dbEntry.googleDrivePhotoFileId != null) {
                        entry.googleDrivePhotoFileId = dbEntry.googleDrivePhotoFileId;
                        _entry.googleDrivePhotoFileId = dbEntry.googleDrivePhotoFileId;
                    }
                }

                DataManager.getInstance().save(_entry, entry.googleDriveFileId == null);
                mSavingEntry = false;

                if (entry.photos != null && entry.photos.size() > 0 && mUpdatedPhoto) {
                    DataManager.getInstance().save(entry.photos.get(0), entry.googleDrivePhotoFileId);
                    mUpdatedPhoto = false;
                }

            }
        }).start();

        if (!mIsEditing)
            mIsEditing = true;
    }

    private void delete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Entry _entry = entry;
                DataManager.getInstance().delete(_entry);
            }
        }).start();
    }

    private void autoRetrieveLocation() {
        final ImageView mLocationLoader = (ImageView) findViewById(R.id.location_loader);
        mLocationLoader.setAlpha(0f);
        mLocationLoader.setVisibility(View.VISIBLE);
        mLocationLoader.animate().alpha(1f).start();

        mLocationLoaderAnim = ValueAnimator.ofInt(0, 360);
        mLocationLoaderAnim.setDuration(1000);
        mLocationLoaderAnim.setInterpolator(new LinearInterpolator());
        mLocationLoaderAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLocationLoader.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mLocationLoader.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mLocationLoaderAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLocationLoader.setRotation((Integer) animation.getAnimatedValue());
            }
        });
        mLocationLoaderAnim.setRepeatCount(ValueAnimator.INFINITE);
        mLocationLoaderAnim.start();

        if (PermissionsUtil.checkAndRequest(this, Manifest.permission.ACCESS_FINE_LOCATION, 3, R.string.permission_explanation_location, null)) {
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            try {
                Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);

                final Runnable mFadeOutRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mLocationLoader.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mLocationLoaderAnim.cancel();
                                mLocationLoader.setVisibility(View.GONE);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();
                    }
                };

                if (lastKnownLocation != null) {
                    LatLng location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                    entry.hasLocation = true;
                    entry.latitude = location.latitude;
                    entry.longitude = location.longitude;

                    new RetrievePlacesTask(false, null) {
                        @Override
                        protected void onPostExecute(MutableArrayList<Pair<String, Double>> pairs) {
                            super.onPostExecute(pairs);

                            String place = null;

                            // check to see if the first custom place is within 1.5km of where we are at
                            if (localPlaces != null && localPlaces.size() > 0) {
                                Pair<String, Double> firstCustomPlace = localPlaces.get(0);

                                if (firstCustomPlace.second < 2)
                                    place = firstCustomPlace.first;
                            }

                            if (place == null && pairs != null && pairs.size() > 0) {
                                place = pairs.get(0).first;
                            }

                            entry.placeName = place;
                            mNameDialog.setName(entry.placeName);
                            mFadeOutRunnable.run();
                            updateLocationIcon();

//                            if ( mIsEditing )
//                                save();
                        }
                    }.execute(location);

                } else {
                    Toast.makeText(this, getString(R.string.error_location), Toast.LENGTH_SHORT).show();
                    mFadeOutRunnable.run();
                }

            } catch (SecurityException e) {

            }
        }
    }
}

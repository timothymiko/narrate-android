package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 12/4/14.
 */
public class FilterSortDialog extends MaterialDialogFragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, TagsPickerDialog.TagsChangeListener, DialogInterface.OnClickListener {

    /**
     * Filter selection
     */
    private boolean mHasPhoto;
    private boolean mBookmarked;
    private boolean mDateRange;

    private boolean mTags;

    private Calendar mStartDate;
    private Calendar mEndDate;
    private ArrayList<String> mTagsList;

    /**
     * Save Instance State
     */
    private final String SAVE_HAS_PHOTO = "mHasPhoto";
    private final String SAVE_BOOKMARKED = "mBookmarked";
    private final String SAVE_DATE_RANGE = "mDateRange";
    private final String SAVE_DATE_START = "mStartDate";
    private final String SAVE_DATE_END = "mEndDate";
    private final String SAVE_TAGS = "mTags";
    private final String SAVE_TAGS_LIST = "mTagsList";

    /**
     * Views
     */
    private CheckBox mPhotoCheckbox;
    private CheckBox mBookmarkCheckbox;
    private CheckBox mDateCheckbox;
    private CheckBox mTagsCheckbox;

    private TextView mDateStartText;
    private TextView mDateEndText;
    private TextView mTagsText;

    private RadioGroup mSortRadioGroup;

    private TagsPickerDialog mTagsDialog;
    private DatePickerDialog mDateStartDialog;
    private DatePickerDialog mDateEndDialog;

    /**
     * Misc
     */
    private SimpleDateFormat mFormatter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mFormatter = new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdyy"));
        else
            mFormatter = new SimpleDateFormat("MMM d, yy");

        mTagsDialog = TagsPickerDialog.newInstance(this, mTagsList);

        Calendar date = Calendar.getInstance();
        mDateStartDialog = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthOfYear);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        mStartDate = cal;

                        mDateRange = mStartDate != null && mEndDate != null;
                        mDateCheckbox.setChecked(true);

                        updateDateText(getDialog());
                    }
                },
                date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)
        );

        mDateEndDialog = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthOfYear);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        mEndDate = cal;

                        mDateRange = mStartDate != null && mEndDate != null;
                        mDateCheckbox.setChecked(true);

                        updateDateText(getDialog());
                    }
                },
                date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)
        );

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_HAS_PHOTO, mHasPhoto);
        outState.putBoolean(SAVE_DATE_RANGE, mDateRange);
        outState.putBoolean(SAVE_TAGS, mTags);

        if ( mDateRange ) {
            outState.putLong(SAVE_DATE_START, mStartDate.getTimeInMillis());
            outState.putLong(SAVE_DATE_END, mEndDate.getTimeInMillis());
        }

        if ( mTags ) {
            outState.putStringArrayList(SAVE_TAGS_LIST, mTagsList);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {

            setContentView(R.layout.dialog_filter_sort);
            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mPhotoCheckbox = (CheckBox) dialog.findViewById(R.id.filter_photo);
            mDateCheckbox = (CheckBox) dialog.findViewById(R.id.filter_date);
            mTagsCheckbox = (CheckBox) dialog.findViewById(R.id.filter_tags);
            mBookmarkCheckbox = (CheckBox) dialog.findViewById(R.id.filter_bookmark);

            mDateStartText = (TextView) dialog.findViewById(R.id.filter_date_start);
            mDateEndText = (TextView) dialog.findViewById(R.id.filter_date_end);
            mTagsText = (TextView) dialog.findViewById(R.id.filter_tags_text);

            mSortRadioGroup = (RadioGroup) dialog.findViewById(R.id.radio_group);
            mSortRadioGroup.check(R.id.sort_newest);

            mDateCheckbox.setOnCheckedChangeListener(this);
            mPhotoCheckbox.setOnCheckedChangeListener(this);
            mTagsCheckbox.setOnCheckedChangeListener(this);
            mBookmarkCheckbox.setOnCheckedChangeListener(this);

            mDateStartText.setOnClickListener(this);
            mDateEndText.setOnClickListener(this);
            mTagsText.setOnClickListener(this);

            mDateCheckbox.setText(getString(R.string.date) + ":");
            mTagsCheckbox.setText(getString(R.string.tags) + ":");

            mTagsText.setText(getString(R.string.none));

            setPositiveButton(R.string.okay, this);

            if ( savedInstanceState != null ) {

                mHasPhoto = savedInstanceState.getBoolean(SAVE_HAS_PHOTO, false);
                mBookmarked = savedInstanceState.getBoolean(SAVE_BOOKMARKED, false);
                mDateRange = savedInstanceState.getBoolean(SAVE_DATE_RANGE, false);
                mTags = savedInstanceState.getBoolean(SAVE_TAGS, false);

                if ( mHasPhoto )
                    mPhotoCheckbox.setChecked(true);

                if ( mBookmarked )
                    mBookmarkCheckbox.setChecked(true);

                if ( mDateRange ) {
                    mStartDate = Calendar.getInstance();
                    mStartDate.setTimeInMillis(savedInstanceState.getLong(SAVE_DATE_START));

                    mEndDate = Calendar.getInstance();
                    mEndDate.setTimeInMillis(savedInstanceState.getLong(SAVE_DATE_END));

                    updateDateText(dialog);

                    mDateCheckbox.setChecked(true);
                }

                if ( mTags ) {
                    mTagsList = savedInstanceState.getStringArrayList(SAVE_TAGS_LIST);
                    onTagsChanged(mTagsList);
                    mTagsCheckbox.setChecked(true);
                }

            } else {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
                try {
                    JSONObject json = new JSONObject(prefs.getString("filter_sort_key", null));

                    if (json.getBoolean("photo")) {
                        mHasPhoto = true;
                        mPhotoCheckbox.setChecked(true);
                    }

                    if (json.getBoolean("bookmarked")) {
                        mBookmarked = true;
                        mBookmarkCheckbox.setChecked(true);
                    }

                    if (json.getBoolean("date_range")) {
                        long start = json.getLong("date_start");
                        long end = json.getLong("date_end");

                        mDateRange = true;

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(start);
                        mStartDate = cal;

                        Calendar cal2 = Calendar.getInstance();
                        cal2.setTimeInMillis(end);
                        mEndDate = cal2;

                        mDateCheckbox.setChecked(true);
                        updateDateText(dialog);
                    }

                    if (json.getBoolean("tags")) {
                        JSONArray tagsJson = json.getJSONArray("tags_list");
                        ArrayList<String> tags = new ArrayList<>();
                        for (int i = 0; i < tagsJson.length(); i++)
                            tags.add(tagsJson.getString(i));

                        onTagsChanged(tags);
                    }

                    if ( json.has("sort") )
                        mSortRadioGroup.check(json.getInt("sort"));

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {

                }
            }

            return dialog;

        } else
            return null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.filter_photo:
                mHasPhoto = isChecked;
                break;
            case R.id.filter_bookmark:
                mBookmarked = isChecked;
                break;
            case R.id.filter_date:
                mDateRange = isChecked;
                if (!mDateRange) {
                    mStartDate = null;
                    mEndDate = null;
                    updateDateText(getDialog());
                }
                break;
            case R.id.filter_tags:
                mTags = isChecked;

                if ( !mTags ) {
                    mTagsList.clear();
                    onTagsChanged(mTagsList);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.filter_date_start) {
            mDateStartDialog.show(getActivity().getFragmentManager(), "mDateStartDialog");
        } else if (v.getId() == R.id.filter_date_end) {
            mDateEndDialog.show(getActivity().getFragmentManager(), "mDateEndDialog");
        } else if (v.getId() == R.id.filter_tags_text) {
            mTagsDialog.show(getFragmentManager(), "TagsDialog");
        }
    }

    @Override
    public void onTagsChanged(List<String> tags) {
        this.mTags = !tags.isEmpty();
        this.mTagsList = new ArrayList<>(tags);
        mTagsCheckbox.setChecked(this.mTags);

        if (this.mTags) {

            StringBuilder tagsBuilder = new StringBuilder();
            for (int i = 0; i < tags.size(); i++) {
                tagsBuilder.append(tags.get(i));

                if (i < tags.size() - 1)
                    tagsBuilder.append(", ");
            }

            mTagsText.setText(tagsBuilder.toString());
        } else {
            mTagsText.setText(getString(R.string.none));
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0: // positive
                SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();

                try {
                    JSONObject json = new JSONObject();

                    json.put("photo", mHasPhoto);
                    json.put("bookmarked", mBookmarked);
                    json.put("date_range", mDateRange);
                    json.put("tags", mTags);

                    if (mDateRange) {
                        json.put("date_start", mStartDate.getTimeInMillis());
                        json.put("date_end", mEndDate.getTimeInMillis());
                    }

                    if (mTags)
                        json.put("tags_list", new JSONArray(mTagsList));

                    json.put("sort", mSortRadioGroup.getCheckedRadioButtonId());

                    prefs.putString("filter_sort_key", json.toString());
                    prefs.commit();

                    Intent i = new Intent(LocalContract.ACTION);
                    i.putExtra(LocalContract.COMMAND, LocalContract.REFRESH_DATA);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
                } catch (JSONException e) {
                    if (!BuildConfig.DEBUG) Crashlytics.logException(e);
                }
                break;
            case 1: // negative
                break;
        }
    }

    private void updateDateText(Dialog dialog) {
        mDateStartText.setText(mStartDate == null ? getString(R.string.start) : mFormatter.format(mStartDate.getTime()));
        mDateEndText.setText(mEndDate == null ? getString(R.string.end) : mFormatter.format(mEndDate.getTime()));

        final LinearLayout filterDateLayout = (LinearLayout) dialog.findViewById(R.id.filter_date_layout);
        final RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) filterDateLayout.getLayoutParams();

        if (mStartDate != null && mEndDate != null) {
            lp.addRule(RelativeLayout.BELOW, R.id.filter_date);
            lp.addRule(RelativeLayout.RIGHT_OF, 0);
            lp.leftMargin = Math.round(getActivity().getResources().getDimensionPixelOffset(R.dimen.checkbox_text_offset));
        } else {
            lp.addRule(RelativeLayout.BELOW, 0);
            lp.addRule(RelativeLayout.RIGHT_OF, R.id.filter_date);
            lp.leftMargin = 0;
        }
        filterDateLayout.setLayoutParams(lp);
    }
}

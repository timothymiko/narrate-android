package com.datonicgroup.narrate.app.ui.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.dataprovider.providers.TagsDao;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncInfoManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.dialogs.MaterialDialogFragment;
import com.datonicgroup.narrate.app.ui.dialogs.RestoreDeletedEntriesDialog;

import java.util.List;

/**
 * Created by timothymiko on 12/30/14.
 */
public class GeneralSettingsCard extends PreferenceCard implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private SwitchPreference mSaveSortFilter;
    private SwitchPreference mSavePhotos;
    private SwitchPreference mAutoLocation;
    private SwitchPreference mTwentyFourHourTime;
    private ButtonPreference mRestoreDeleted;

    private FragmentActivity activity;

    public GeneralSettingsCard(FragmentActivity activity) {
        super(activity);
        this.activity = activity;
    }

    public GeneralSettingsCard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GeneralSettingsCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.general);

        mSaveSortFilter = new SwitchPreference(getContext());
        mSavePhotos = new SwitchPreference(getContext());
        mAutoLocation = new SwitchPreference(getContext());
        mTwentyFourHourTime = new SwitchPreference(getContext());
        mRestoreDeleted = new ButtonPreference(getContext());

        mSaveSortFilter.setTitle(R.string.general_save_sort_filter);
        mSavePhotos.setTitle(R.string.general_save_photos);
        mAutoLocation.setTitle(R.string.general_auto_location);
        mTwentyFourHourTime.setTitle(R.string.general_twentyfour_hour_format);
        mRestoreDeleted.setTitle(R.string.restore_deleted_entries);
        mRestoreDeleted.setButtonText(getResources().getString(R.string.restore));

        mSaveSortFilter.setTag(0);
        mSavePhotos.setTag(1);
        mAutoLocation.setTag(2);
        mTwentyFourHourTime.setTag(3);
        mRestoreDeleted.setTag(4);

        mSaveSortFilter.setOnCheckedChangedListener(this);
        mSavePhotos.setOnCheckedChangedListener(this);
        mAutoLocation.setOnCheckedChangedListener(this);
        mTwentyFourHourTime.setOnCheckedChangedListener(this);
        mRestoreDeleted.setOnClickListener(this);

        mSaveSortFilter.setChecked(Settings.getSaveSortFilter());
        mSavePhotos.setChecked(Settings.getSavePhotos());
        mAutoLocation.setChecked(Settings.getAutomaticLocation());
        mTwentyFourHourTime.setChecked(Settings.getTwentyFourHourTime());

        addView(mSaveSortFilter);
        addView(mSavePhotos);
        addView(mAutoLocation);
        addView(mTwentyFourHourTime);
        addView(mRestoreDeleted);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Integer key = (Integer)buttonView.getTag();
        switch (key) {
            case 0:
                Settings.setSaveSortFilter(isChecked);
                break;
            case 1:
                Settings.setSavePhotos(isChecked);
                break;
            case 2:
                Settings.setAutomaticLocation(isChecked);
                break;
            case 3:
                Settings.setTwentyFourHourTime(isChecked);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        RestoreDeletedEntriesDialog.newInstance().show(activity.getSupportFragmentManager(), "ConfirmationDialog");
    }
}

package com.datonicgroup.narrate.app.ui.settings;

import android.Manifest;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.LocalBackupManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.dialogs.BackupFrequencyDialog;
import com.datonicgroup.narrate.app.ui.dialogs.BackupRestoreDialog;
import com.datonicgroup.narrate.app.ui.dialogs.EditTextDialog;
import com.datonicgroup.narrate.app.util.PermissionsUtil;

/**
 * Created by timothymiko on 1/7/15.
 */
public class LocalBackupCard extends PreferenceCard {

    private ButtonPreference mBackupFrequencyPref;
    private ButtonPreference mBackupsToKeepPref;
    private ButtonPreference mStartBackupPref;
    private ButtonPreference mRestoreBackupPref;

    private BackupFrequencyDialog mFrequencyDialog;
    private BackupRestoreDialog mRestoreDialog;
    private EditTextDialog mBackupsToKeepDialog;

    private FragmentActivity mActivity;

    public LocalBackupCard(FragmentActivity activity) {
        super(activity);

        this.mActivity = activity;

        mFrequencyDialog = new BackupFrequencyDialog();
        mFrequencyDialog.setListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateFrequencyText();
            }
        });

        mRestoreDialog = new BackupRestoreDialog();

        mBackupsToKeepDialog = new EditTextDialog();
        mBackupsToKeepDialog.setTitle(R.string.local_backup_max_to_keep);
        mBackupsToKeepDialog.setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int num = Integer.valueOf(mBackupsToKeepDialog.getEditText().getText().toString());
                Settings.setLocalBackupsToKeep(num);
                mBackupsToKeepPref.setButtonText(String.valueOf(num));
                mBackupsToKeepDialog.setText(String.valueOf(num));
            }
        });
        mBackupsToKeepDialog.setNegativeButton(R.string.cancel_uc, null);
        mBackupsToKeepDialog.setText(String.valueOf(Settings.getLocalBackupsToKeep()));
        mBackupsToKeepDialog.setInputType(InputType.TYPE_CLASS_NUMBER);

        setTitle(R.string.local_backup);
        setSwitchEnabled(true);

        mBackupFrequencyPref = new ButtonPreference(getContext());
        mBackupsToKeepPref = new ButtonPreference(getContext());
        mStartBackupPref = new ButtonPreference(getContext());
        mRestoreBackupPref = new ButtonPreference(getContext());

        mBackupFrequencyPref.setTitle(R.string.local_backup_frequency);
        mBackupsToKeepPref.setTitle(R.string.local_backup_max);
        mStartBackupPref.setTitle(R.string.local_backup_start);
        mRestoreBackupPref.setTitle(R.string.local_backup_restore);

        updateFrequencyText();

        mBackupsToKeepPref.setButtonText(String.valueOf(Settings.getLocalBackupsToKeep()));


        mStartBackupPref.setButtonText(R.string.start);
        mRestoreBackupPref.setButtonText(R.string.restore);

        mBackupFrequencyPref.setTag(0);
        mBackupsToKeepPref.setTag(1);
        mStartBackupPref.setTag(2);
        mRestoreBackupPref.setTag(3);

        mBackupFrequencyPref.setOnClickListener(this);
        mBackupsToKeepPref.setOnClickListener(this);
        mStartBackupPref.setOnClickListener(this);
        mRestoreBackupPref.setOnClickListener(this);

        addView(mBackupFrequencyPref);
        addView(mBackupsToKeepPref);
        addView(mStartBackupPref);
        addView(mRestoreBackupPref);

        mTitle.setChecked(Settings.getLocalBackupsEnabled());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_title:
                if (PermissionsUtil.checkAndRequest(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE, 100, R.string.permission_explanation_write_storage, null)) {
                    super.onCheckedChanged(buttonView, isChecked);

                    Settings.setLocalBackupsEnabled(isChecked);

                    if (Settings.getLocalBackupFrequency() == -1)
                        Settings.setLocalBackupFrequency(0);

                    LocalBackupManager.setEnabled(isChecked);
                } else {
                    mTitle.setChecked(false);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch ((Integer)v.getTag()) {
            case 0:
                mFrequencyDialog.show(mActivity.getSupportFragmentManager(), "BackupFrequencyDialog");
                break;
            case 1:
                mBackupsToKeepDialog.show(mActivity.getSupportFragmentManager(), "BackupsToKeepDialog");
                break;
            case 2:
                Toast.makeText(getContext(), R.string.local_backup_starting, Toast.LENGTH_SHORT).show();
                LocalBackupManager.backup();
                break;
            case 3:
                mRestoreDialog.show(mActivity.getSupportFragmentManager(), "BackupRestoreDialog");
                break;
        }
    }

    private void updateFrequencyText() {
        int frequency = Settings.getLocalBackupFrequency();

        switch (frequency) {
            case -1:
                mBackupFrequencyPref.setButtonText(R.string.none);
                break;
            case 0:
                mBackupFrequencyPref.setButtonText(R.string.daily);
                break;
            case 1:
                mBackupFrequencyPref.setButtonText(R.string.weekly);
                break;
            case 2:
                mBackupFrequencyPref.setButtonText(R.string.monthly);
                break;
        }
    }
}

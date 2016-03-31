package com.datonicgroup.narrate.app.ui.settings;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.GoogleAccountsService;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract;
import com.datonicgroup.narrate.app.dataprovider.sync.GoogleDriveSyncService;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.models.SyncFolderType;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.dialogs.AutoSyncIntervalDialog;
import com.datonicgroup.narrate.app.ui.dialogs.SyncFolderSettingsDialog;
import com.datonicgroup.narrate.app.ui.dialogs.WarningDialog;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.PermissionsUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.drive.DriveScopes;

import java.io.IOException;

import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by timothymiko on 1/6/15.
 */
public class SyncCard extends PreferenceCard implements View.OnClickListener, SyncFolderSettingsDialog.Callbacks {

    public static final int GDRIVE_REQUEST_AUTHORIZATION = 100;

    private SwitchPreference mGoogleDrivePref;
    private SwitchPreference mDropboxPref;
    private ButtonPreference mDropboxPathPref;
    private SwitchPreference mWifiSyncPref;
    private ButtonPreference mAutoSyncInterval;
    private ButtonPreference mGoogleDriveTest;

    private FragmentActivity mActivity;

    private boolean mIsEnablingDropboxSync;
    public boolean mAuthenticatingDropbox;
    private DropboxAPI<AndroidAuthSession> mDBApi;

    private SyncFolderSettingsDialog mSyncFolderDialog;
    private AutoSyncIntervalDialog mIntervalDialog;

    public SyncCard(FragmentActivity activity) {
        super(activity);
        this.mActivity = activity;
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.sync);
        setSwitchEnabled(true);

        mSyncFolderDialog = new SyncFolderSettingsDialog();
        mSyncFolderDialog.setCallback(this);

        mIntervalDialog = new AutoSyncIntervalDialog();
        mIntervalDialog.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ( Settings.getAutoSyncInterval() == -1 )
                    mAutoSyncInterval.setButtonText(R.string.none);
                else {
                    mAutoSyncInterval.setButtonText(Settings.getAutoSyncInterval()/ DateUtil.HOUR_IN_SECONDS + " " + getResources().getString(R.string.hours));
                }
            }
        });

        mGoogleDrivePref = new SwitchPreference(getContext());
        mDropboxPref = new SwitchPreference(getContext());
        mDropboxPathPref = new ButtonPreference(getContext());
        mWifiSyncPref = new SwitchPreference(getContext());
        mAutoSyncInterval = new ButtonPreference(getContext());

        mGoogleDrivePref.setTitle(R.string.google_drive_title);
        mDropboxPref.setTitle(R.string.use_dropbox);
        mDropboxPathPref.setTitle(R.string.dropbox_sync_folder);
        mWifiSyncPref.setTitle(R.string.sync_data_network_descrip);
        mAutoSyncInterval.setTitle(R.string.sync_interval);

        mGoogleDrivePref.setTag(0);
        mDropboxPref.setTag(1);
        mWifiSyncPref.setTag(2);
        mDropboxPathPref.setTag(3);
        mAutoSyncInterval.setTag(4);

        mGoogleDrivePref.setOnCheckedChangedListener(this);
        mDropboxPref.setOnCheckedChangedListener(this);
        mWifiSyncPref.setOnCheckedChangedListener(this);

        mGoogleDrivePref.setOnCheckedChangedListener(this);
        mDropboxPref.setOnCheckedChangedListener(this);
        mWifiSyncPref.setOnCheckedChangedListener(this);

        String folder = Settings.getDropboxSyncFolder();
        if ( folder != null )
            mDropboxPathPref.setButtonText(folder);
        else
            mDropboxPathPref.setButtonText(R.string.none);

        if ( Settings.getAutoSyncInterval() == -1 )
            mAutoSyncInterval.setButtonText(R.string.none);
        else {
            mAutoSyncInterval.setButtonText(Settings.getAutoSyncInterval()/ DateUtil.HOUR_IN_SECONDS + " " + getResources().getString(R.string.hours));
        }

        mDropboxPathPref.setOnClickListener(this);
        mAutoSyncInterval.setOnClickListener(this);

        addView(mGoogleDrivePref);
        addView(mDropboxPref);
        addView(mDropboxPathPref);
        addView(mWifiSyncPref);
        addView(mAutoSyncInterval);

        mTitle.setChecked(Settings.getSyncEnabled());
        mGoogleDrivePref.setChecked(Settings.getGoogleDriveSyncEnabled());
        mDropboxPref.setChecked(Settings.getDropboxSyncEnabled());
        mWifiSyncPref.setChecked(Settings.getSyncOnMobileData());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_title:
                if (PermissionsUtil.checkAndRequest(mActivity, Manifest.permission.GET_ACCOUNTS, 100, R.string.permission_explanation_get_accounts, null)) {
                    if (PermissionsUtil.checkAndRequest(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE, 100, R.string.permission_explanation_write_storage, null)) {
                        super.onCheckedChanged(buttonView, isChecked);
                        Settings.setSyncEnabled(isChecked);
                        if (isChecked) {
                            if (Settings.getDropboxSyncEnabled() || Settings.getGoogleDriveSyncEnabled()) {
                                enableSync();
                            }
                        } else {
                            cancelSync();
                        }
                    } else {
                        mTitle.setChecked(false);
                    }
                } else {
                    mTitle.setChecked(false);
                }
                return;
        }

        super.onCheckedChanged(buttonView, isChecked);
        switch ((Integer) buttonView.getTag()) {
            case 0:
                if (PermissionsUtil.checkAndRequest(mActivity, Manifest.permission.GET_ACCOUNTS, 100, R.string.permission_explanation_get_accounts, null)) {
                    onGoogleDriveChanged(isChecked);
                } else {
                    mGoogleDrivePref.setChecked(false);
                }
                break;
            case 1:
                if (PermissionsUtil.checkAndRequest(mActivity, Manifest.permission.GET_ACCOUNTS, 100, R.string.permission_explanation_get_accounts, null)) {
                    onDropboxChanged(isChecked);
                } else {
                    mDropboxPref.setChecked(false);
                }
                break;
            case 2:
                Settings.setSyncOnMobileData(isChecked);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch ((Integer)v.getTag()) {
            case 3:
                mSyncFolderDialog.setCancelable(true);
                mSyncFolderDialog.show(mActivity.getSupportFragmentManager(), "SyncFolderDialog");
                break;
            case 4:
                mIntervalDialog.show(mActivity.getSupportFragmentManager(), "SyncIntervalDialog");
                break;
        }
    }

    private void onGoogleDriveChanged(boolean enabled) {
        if (enabled) {
            if (!Settings.getGoogleDriveSyncEnabled()) {
                if (Settings.getDropboxSyncEnabled()) {
                    DialogInterface.OnClickListener pos = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Thread(mEnableDriveSyncRunnable).start();
                        }
                    };
                    DialogInterface.OnClickListener neg = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.setGoogleDriveSyncEnabled(false);
                            mGoogleDrivePref.setChecked(false);
                        }
                    };
                    WarningDialog dialog = new WarningDialog();
                    dialog.setPositiveListener(pos);
                    dialog.setNegativeListener(neg);
                    dialog.show(mActivity.getSupportFragmentManager(), "WarningDialog");
                } else {
                    new Thread(mEnableDriveSyncRunnable).start();
                }
            }
        } else {
            if (Settings.getGoogleDriveSyncEnabled()) {
                Settings.setGoogleDriveSyncEnabled(false);
                cancelSync();
            }
        }
    }

    private Runnable mEnableDriveSyncRunnable = new Runnable() {
        @Override
        public void run() {
            try {

                String email = Settings.getEmail();
                Settings.setGoogleAccountName(email);

                GoogleAccountsService.getAuthToken("https://www.googleapis.com/auth/drive.appfolder");

                GoogleDriveSyncService.shared().setup(new GoogleDriveSyncService.GoogleDriveSyncSetupInterface() {
                    @Override
                    public void onSetupComplete() {

                        Settings.setGoogleDriveSyncEnabled(true);

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                enableSync();
                            }
                        });
                    }

                    @Override
                    public void onSetupFailure() {

                        Settings.setGoogleDriveSyncEnabled(false);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mGoogleDrivePref.setChecked(false);
                            }
                        });
                    }
                });

            } catch (final UserRecoverableAuthException e) {
                e.printStackTrace();
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mGoogleDrivePref.setChecked(false);
                            mActivity.startActivityForResult(e.getIntent(), GDRIVE_REQUEST_AUTHORIZATION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void onDropboxChanged(boolean enabled) {
        if (enabled) {
            if ( !Settings.getDropboxSyncEnabled() ) {

                if ( Settings.getGoogleDriveSyncEnabled() ) {
                    DialogInterface.OnClickListener pos = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if ( Settings.getDropboxSyncToken() == null ) {
                                mAuthenticatingDropbox = true;
                                AppKeyPair appKeys = new AppKeyPair(BuildConfig.DROPBOX_API_KEY, BuildConfig.DROPBOX_API_SECRET);
                                AndroidAuthSession session = new AndroidAuthSession(appKeys, Session.AccessType.DROPBOX);
                                mDBApi = new DropboxAPI<AndroidAuthSession>(session);
                                mDBApi.getSession().startOAuth2Authentication(mActivity);
                            } else {
                                // show sync folder dialog
                                mIsEnablingDropboxSync = true;
                                mSyncFolderDialog.setCancelable(false);
                                mSyncFolderDialog.show(mActivity.getSupportFragmentManager(), "SyncFolderDialog");
                            }
                        }
                    };
                    DialogInterface.OnClickListener neg = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.setDropboxSyncEnabled(false);
                            mDropboxPref.setChecked(false);
                        }
                    };
                    WarningDialog dialog = new WarningDialog();
                    dialog.setPositiveListener(pos);
                    dialog.setNegativeListener(neg);
                    dialog.show(mActivity.getSupportFragmentManager(), "WarningDialog");
                } else {
                    if ( Settings.getDropboxSyncToken() == null ) {
                        mAuthenticatingDropbox = true;
                        AppKeyPair appKeys = new AppKeyPair(BuildConfig.DROPBOX_API_KEY, BuildConfig.DROPBOX_API_SECRET);
                        AndroidAuthSession session = new AndroidAuthSession(appKeys, Session.AccessType.DROPBOX);
                        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
                        mDBApi.getSession().startOAuth2Authentication(mActivity);
                    } else {
                        // show sync folder dialog
                        mIsEnablingDropboxSync = true;
                        mSyncFolderDialog.setCancelable(false);
                        mSyncFolderDialog.show(mActivity.getSupportFragmentManager(), "SyncFolderDialog");
                    }
                }
            }
        } else {
            if ( Settings.getDropboxSyncEnabled() ) {
                Settings.setDropboxSyncEnabled(false);
            }
        }
    }

    public void onResume() {
        if ( mAuthenticatingDropbox ) {
            mAuthenticatingDropbox = false;

            if (mDBApi != null && mDBApi.getSession().authenticationSuccessful()) {
                try {
                    // Required to complete auth, sets the access token on the session
                    mDBApi.getSession().finishAuthentication();

                    String dropboxToken = mDBApi.getSession().getOAuth2AccessToken();

                    Settings.setDropboxSyncToken(dropboxToken);
                    Settings.setDropboxSyncEnabled(true);

                    mIsEnablingDropboxSync = true;
                    mSyncFolderDialog.setCancelable(false);
                    mSyncFolderDialog.show(mActivity.getSupportFragmentManager(), "SyncFolderDialog");

                } catch (IllegalStateException e) {
                    Log.i("DbAuthLog", "Error authenticating", e);
                }
            } else {
                Settings.setDropboxSyncEnabled(false);
                mDropboxPref.setChecked(false);
            }
        }
    }

    public void onActivityResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            mGoogleDrivePref.setChecked(true);
            Settings.setGoogleDriveSyncEnabled(true);
            enableSync();
        } else {
            mGoogleDrivePref.setChecked(false);
        }
    }

    private void enableSync() {
        Account acc = User.getAccount();


        ContentResolver.setIsSyncable(acc, Contract.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(acc, Contract.AUTHORITY, true);
        ContentResolver.removePeriodicSync(acc, Contract.AUTHORITY, Bundle.EMPTY);

        long interval = Settings.getAutoSyncInterval();

        if (interval > 0) {
            ContentResolver.addPeriodicSync(acc, Contract.AUTHORITY, Bundle.EMPTY, interval);
        }

        Bundle b = new Bundle();
        b.putBoolean("resync_files", true);

        SyncHelper.requestManualSync(acc, b);

        Toast.makeText(GlobalApplication.getAppContext(), GlobalApplication.getAppContext().getString(R.string.data_resyncing), Toast.LENGTH_SHORT).show();
    }

    private void cancelSync() {

        Account acc = User.getAccount();

        ContentResolver.setIsSyncable(acc, Contract.AUTHORITY, 0);
        ContentResolver.setSyncAutomatically(acc, Contract.AUTHORITY, false);

        SettingsUtil.setSyncStatus("N/A");
    }

    @Override
    public void onCancel() {
        if ( mIsEnablingDropboxSync ) {
            Settings.setDropboxSyncEnabled(false);
            mDropboxPref.setChecked(false);
        }
    }

    @Override
    public void onPathSaved(SyncFolderType type, String path) {
        Settings.setDropboxSyncFolder(path);
        mDropboxPathPref.setButtonText(path);

        if ( mIsEnablingDropboxSync || Settings.getDropboxSyncEnabled() ) {
            Settings.setDropboxSyncEnabled(true);
            enableSync();
        }
    }
}

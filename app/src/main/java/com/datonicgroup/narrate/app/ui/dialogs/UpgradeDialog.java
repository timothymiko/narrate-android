package com.datonicgroup.narrate.app.ui.dialogs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.dataprovider.providers.SyncInfoDao;
import com.datonicgroup.narrate.app.dataprovider.sync.DriveSyncService;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.SyncFolderType;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.entries.PhotosGridFragment;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.datonicgroup.narrate.app.models.User.ACCOUNT_TYPE;

public class UpgradeDialog {

    public interface UpgradeListener {
        void onUpgradeComplete();
    }

    private Thread thread;
    private TextView mText;
    private final int REQUEST_AUTHORIZATION = 100;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private UpgradeListener mListener;

    private Activity mActivity;
    private AlertDialog mDialog;

    public UpgradeDialog(Activity activity, UpgradeListener listener) {
        this.mActivity = activity;
        this.mListener = listener;

        View v = View.inflate(mActivity, R.layout.activity_app_upgrade, null);
        mDialog = new AlertDialog.Builder(mActivity)
                .setView(v)
                .setCancelable(false)
                .create();
        mDialog.show();

        startUpgrade();
    }

    public void startUpgrade() {
        if ( thread != null ) {
            thread.interrupt();
            thread = null;
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtil.log("AppUpgradeActivity", "Upgrading user()");

                upgradeUser();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onUpgradeComplete();
                        mDialog.dismiss();
                    }
                });
            }
        });
        thread.start();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == REQUEST_AUTHORIZATION && resultCode == Activity.RESULT_OK ) {
            LogUtil.log("ProPreferences", "REQUEST_AUTHORIZATION - DRIVE API");
            Settings.setGoogleDriveSyncEnabled(true);
            startUpgrade();
        }
    }

    private void upgradeUser() {
        switch (Settings.getAppVersion()) {
            case 24:
                LogUtil.log("AppUpgradeActivity", "Upgrading sync info.");

                if ( Settings.getGoogleDriveSyncEnabled() ) {
                    try {

                        String token = GoogleAuthUtil.getToken(GlobalApplication.getAppContext(), Settings.getEmail(), "oauth2:" + DriveScopes.DRIVE_APPDATA);
                        GoogleAuthUtil.clearToken(GlobalApplication.getAppContext(), token);
                        token = GoogleAuthUtil.getToken(GlobalApplication.getAppContext(), Settings.getEmail(), "oauth2:" + DriveScopes.DRIVE_APPDATA);
                        SettingsUtil.refreshGDriveToken();

                    } catch (final UserRecoverableAuthException e) {
                        e.printStackTrace();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                                thread.interrupt();
                                thread = null;
                            }
                        });
                        return;
                    } catch (GoogleAuthException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    try {
                        new DriveSyncService().deleteEverything();
                    } catch (Exception e) {

                    }
                }

                List<Entry> entries = EntryHelper.getAllEntries();
                for (Entry e : entries) {
                    if (e.revisionKey != null) {
                        SyncInfo info = new SyncInfo(e.uuid);
                        info.setRevision(e.revisionKey);
                        info.setModifiedDate(e.modifiedDate);
                        info.setSyncStatus(e.dropboxSyncStatus);
                        info.setSyncService(SyncService.Dropbox.ordinal());
                        SyncInfoDao.saveData(info);
                    }
                }

                if (Settings.getDropboxSyncEnabled() || Settings.getGoogleDriveSyncEnabled())
                    DataManager.getInstance().sync();
            case 29:
                AccountManager accountManager = AccountManager.get(GlobalApplication.getAppContext());
                Account[] accs = accountManager.getAccountsByType(ACCOUNT_TYPE);

                String email = Settings.getEmail();
                Account acc;

                if ( accs != null && accs.length > 0 )
                    acc = accs[0];
                else {
                    acc = new Account(email, ACCOUNT_TYPE);
                    accountManager.addAccountExplicitly(acc, null, null);
                }

                if (Settings.getDropboxSyncEnabled() || Settings.getGoogleDriveSyncEnabled()) {
                    ContentResolver.setSyncAutomatically(acc, Contract.AUTHORITY, true);

                    Context context = GlobalApplication.getAppContext();

                    long interval = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("key_sync_interval", "0"));

                    if (acc != null && interval == 0) {
                        ContentResolver.removePeriodicSync(acc, Contract.AUTHORITY, Bundle.EMPTY);

                        interval = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("key_sync_interval", "0"));
                    }

                    ContentResolver.addPeriodicSync(acc, Contract.AUTHORITY, Bundle.EMPTY, interval);
                }

            case 32:
                if (Settings.getDropboxSyncEnabled() || Settings.getGoogleDriveSyncEnabled())
                    ContentResolver.setSyncAutomatically(User.getAccount(), Contract.AUTHORITY, true);
            case 37:
                // update settings
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
                SharedPreferences.Editor editor = prefs.edit();

                boolean syncDropbox = prefs.getBoolean("useDropbox", false);
                String dropboxToken = prefs.getString("dropboxToken", null);
                boolean integrateDayOne = prefs.getBoolean("integrateDayOne", false);
                String syncFolder = prefs.getString("syncFolder", null);
                SyncFolderType syncFolderType = SyncFolderType.values()[prefs.getInt("syncFolderType", 0)];
                editor.remove("useDropbox");
                editor.remove("dropboxToken");
                editor.remove("integrateDayOne");
                editor.remove("syncFolder");
                editor.remove("syncFolderType");

                Settings.setDropboxSyncEnabled(syncDropbox);
                Settings.setDropboxSyncDayOne(integrateDayOne);
                if ( syncDropbox && dropboxToken != null ) {
                    Settings.setDropboxSyncToken(dropboxToken);

                    if ( syncFolder != null ) {
                        Settings.setDropboxSyncFolder(syncFolder);
                        Settings.setDropboxSyncFolderType(syncFolderType);
                    }
                }

                boolean syncGoogleDrive = prefs.getBoolean("proGoogleDriveSync", false);
                Settings.setGoogleDriveSyncEnabled(syncGoogleDrive);
                editor.remove("proGoogleDriveSync");

                if ( syncGoogleDrive || syncDropbox ) {
                    Settings.setSyncEnabled(true);
                    String autoSyncInterval = prefs.getString("key_sync_interval", null);

                    if ( autoSyncInterval != null ) {
                        String[] values = {
                                String.valueOf(-1),
                                String.valueOf(1 * DateUtil.HOUR_IN_SECONDS),
                                String.valueOf(3 * DateUtil.HOUR_IN_SECONDS),
                                String.valueOf(6 * DateUtil.HOUR_IN_SECONDS),
                                String.valueOf(12 * DateUtil.HOUR_IN_SECONDS),
                        };

                        switch (autoSyncInterval) {
                            case "-1":
                                Settings.setAutoSyncInterval(-1);
                                break;
                            case "3600":
                                Settings.setAutoSyncInterval(DateUtil.HOUR_IN_SECONDS);
                                break;
                            case "10800":
                                Settings.setAutoSyncInterval(3 * DateUtil.HOUR_IN_SECONDS);
                                break;
                            case "21600":
                                Settings.setAutoSyncInterval(6 * DateUtil.HOUR_IN_SECONDS);
                                break;
                            case "43200":
                                Settings.setAutoSyncInterval(12 * DateUtil.HOUR_IN_SECONDS);
                                break;
                        }
                    }
                }
                editor.remove("key_sync_interval");

                boolean automaticLocation = prefs.getBoolean("automaticLocation", false);
                editor.remove("automaticLocation");
                Settings.setAutomaticLocation(automaticLocation);

                boolean syncOnMobileData = prefs.getBoolean("sync_on_data_network", false);
                editor.remove("sync_on_data_network");
                Settings.setSyncOnMobileData(syncOnMobileData);

                boolean useMilitaryTime = prefs.getBoolean("use_military_time", false);
                Settings.setTwentyFourHourTime(useMilitaryTime);
                editor.remove("use_military_time");

                boolean useLocalBackup = prefs.getBoolean("key_local_backup", false);
                int interval = Integer.parseInt(prefs.getString("key_backup_frequency", "0"));
                Settings.setLocalBackupsEnabled(useLocalBackup);
                if ( useLocalBackup ) {
                    Settings.setLocalBackupFrequency(interval);
                }
                editor.remove("key_local_backup");
                editor.remove("key_backup_frequency");

                editor.apply();

                if (RemindersDao.getAllReminders().size() > 0)
                    Settings.setRemindersEnabled(true);
            case 41:

                prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
                editor = prefs.edit();

                editor.putInt("main_toolbar_num_sections", 4);
                editor.putString("main_toolbar_order_" + 3, prefs.getString("main_toolbar_order_" + 2, null));
                editor.putString("main_toolbar_order_" + 2, prefs.getString("main_toolbar_order_" + 1, null));
                editor.putString("main_toolbar_order_" + 1, PhotosGridFragment.TAG);
                editor.remove("entries_list_toggle");

                editor.apply();

                break;
        }

        Settings.setAppVersion(BuildConfig.VERSION_CODE);
    }
}

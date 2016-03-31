package com.datonicgroup.narrate.app.dataprovider.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by timothymiko on 11/28/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Pattern sSanitizeAccountNamePattern = Pattern.compile("(.).*?(.?)@");
    private static DropboxSyncService mDropboxService;
    private static GoogleDriveSyncService mGoogleDriveService;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        if (!BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    LogUtil.e("Narrate", "Uncaught sync exception, suppressing UI in release build.",
                            throwable);
                }
            });
        }
    }

    @Override
    public void onPerformSync(final Account account, Bundle extras, String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {

        if (Settings.getDropboxSyncEnabled() && mDropboxService == null) {
            mDropboxService = new DropboxSyncService();
        }

        if (Settings.getGoogleDriveSyncEnabled() && mGoogleDriveService == null) {
            mGoogleDriveService = new GoogleDriveSyncService();
        }

        final boolean uploadOnly = extras.getBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, false);
        final boolean manualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final boolean initialize = extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE, false);

        final String logSanitizedAccountName = sSanitizeAccountNamePattern
                .matcher(account.name).replaceAll("$1...$2@");

        if (uploadOnly) {
            return;
        }

        LogUtil.log("Narrate", "Beginning sync for account " + logSanitizedAccountName + "," +
                " uploadOnly=" + uploadOnly +
                " manualSync=" + manualSync +
                " initialize=" + initialize);

        if ( !User.isAbleToSync() ) {
            LogUtil.log("NarrateSync", "Cancelling sync. User is not able to perform sync.");
            return;
        }

        Intent i = new Intent(LocalContract.ACTION);
        i.putExtra(LocalContract.COMMAND, LocalContract.SYNC_START);
        LocalBroadcastManager.getInstance(GlobalApplication.getAppContext()).sendBroadcast(i);

        SettingsUtil.setSyncStatus(GlobalApplication.getAppContext().getString(R.string.currently_syncing));

        if ( extras != null && extras.getBoolean("resync_files") ) {

            LogUtil.log("NarrateSync", "Resync all files!");

            if (Settings.getDropboxSyncEnabled()) {
                mDropboxService.resyncFiles();
            }

//            if (Settings.getGoogleDriveSyncEnabled()) {
//                mGoogleDriveService.manualSync();
//            }

        } else {

            if (Settings.getDropboxSyncEnabled()) {
                mDropboxService.sync();
            }

            if (Settings.getGoogleDriveSyncEnabled()) {
                try {
                    if (Settings.getGoogleDriveManualSyncPending()) {
                        mGoogleDriveService.manualSync();
                    }
                    mGoogleDriveService.sync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        SettingsUtil.setSyncStatus(System.currentTimeMillis());

        Intent i2 = new Intent(LocalContract.ACTION);
        i2.putExtra(LocalContract.COMMAND, LocalContract.SYNC_FINISHED);
        LocalBroadcastManager.getInstance(GlobalApplication.getAppContext()).sendBroadcast(i2);
    }
}

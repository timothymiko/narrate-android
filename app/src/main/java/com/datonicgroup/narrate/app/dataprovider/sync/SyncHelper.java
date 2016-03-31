package com.datonicgroup.narrate.app.dataprovider.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.models.events.RemoteSyncFinishedEvent;
import com.datonicgroup.narrate.app.models.events.RemoteSyncStartEvent;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.EventBusBuilder;

/**
 * Created by timothymiko on 11/28/14.
 */
public class SyncHelper {

    private static final String TAG = SyncHelper.class.getSimpleName();

    private Context mContext;

    public SyncHelper(Context context) {
        this.mContext = context;
    }

    public static void requestManualSync(Account mChosenAccount) {
        requestManualSync(mChosenAccount, Bundle.EMPTY);
    }

    public static void requestManualSync(Account mChosenAccount, Bundle extras) {
        if (mChosenAccount != null &&
                ContentResolver.getSyncAutomatically(User.getAccount(), Contract.AUTHORITY)) {

            LogUtil.log(TAG, "Requesting manual sync for account " + mChosenAccount.name);

            Bundle b = new Bundle(extras);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

            cancelPendingActiveSync(mChosenAccount);

            LogUtil.log(TAG, "Requesting sync now.");
            ContentResolver.requestSync(mChosenAccount, Contract.AUTHORITY, b);
        } else {
            LogUtil.log(TAG, "Can't request manual sync -- no chosen account.");
        }
    }

    public static boolean cancelPendingActiveSync(Account mChosenAccount) {
        boolean pending = ContentResolver.isSyncPending(mChosenAccount, Contract.AUTHORITY);
        if (pending) {
            LogUtil.log(TAG, "Warning: sync is PENDING. Will cancel.");
        }
        boolean active = ContentResolver.isSyncActive(mChosenAccount, Contract.AUTHORITY);
        if (active) {
            LogUtil.log(TAG, "Warning: sync is ACTIVE. Will cancel.");
        }

        if (pending || active) {
            LogUtil.log(TAG, "Cancelling previously pending/active sync.");
            ContentResolver.cancelSync(mChosenAccount, Contract.AUTHORITY);
            return true;
        }

        return false;
    }

    public static List<AbsSyncService> getSyncServices() {

        List<AbsSyncService> syncServices = new ArrayList<>();

        if (Settings.getDropboxSyncEnabled())
            syncServices.add(new DropboxSyncService());

        if (Settings.getGoogleDriveSyncEnabled()) {
            try {
                DriveSyncService syncService = new DriveSyncService();
                syncServices.add(syncService);
            } catch (UserRecoverableAuthException e) {
                e.printStackTrace();

                Settings.setGoogleDriveSyncEnabled(false);
                Toast.makeText(GlobalApplication.getAppContext(), "Google Drive Error. Disabling Drive sync for now.", Toast.LENGTH_LONG).show();
            }
        }

        return syncServices;
    }
}

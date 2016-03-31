package com.datonicgroup.narrate.app.models;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.NetworkUtil;

/**
 * Created by timothymiko on 6/1/14.
 */
public abstract class User {

    public static final String ACCOUNT_TYPE = "com.datonicgroup.narrate.acc";

    public static boolean isAbleToSync() {

        if ( Settings.getSyncEnabled() ) {
            if (Settings.getGoogleDriveSyncEnabled() || Settings.getDropboxSyncEnabled()) {
                return Settings.getSyncOnMobileData() || NetworkUtil.isOnWifi();
            } else
                return false;
        } else {
            return false;
        }
    }

    public static Account getAccount() {
        AccountManager accountManager = AccountManager.get(GlobalApplication.getAppContext());
        Account[] accs = accountManager.getAccountsByType(ACCOUNT_TYPE);
        if ( accs != null && accs.length > 0 )
            return accs[0];
        else {
            // recreate the user account if it is null
            Account acc = new Account(Settings.getEmail(), ACCOUNT_TYPE);
            accountManager.addAccountExplicitly(acc, null, null);
            return acc;
        }
    }
}

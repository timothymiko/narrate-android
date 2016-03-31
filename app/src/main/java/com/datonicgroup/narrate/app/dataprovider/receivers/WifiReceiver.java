package com.datonicgroup.narrate.app.dataprovider.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.models.User;

/**
 * Created by timothymiko on 12/28/14.
 */
public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!User.isAbleToSync() && Settings.getEmail() != null)
            SyncHelper.cancelPendingActiveSync(User.getAccount());
    }
}

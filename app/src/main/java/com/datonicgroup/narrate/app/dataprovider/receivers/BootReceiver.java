package com.datonicgroup.narrate.app.dataprovider.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.datonicgroup.narrate.app.dataprovider.LocalBackupManager;
import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.List;

/**
 * Created by timothymiko on 9/24/14.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        LogUtil.log("BootReceiver", "onReceive()");
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {

                    if ( PreferenceManager.getDefaultSharedPreferences(context).getBoolean("key_local_backup", false) )
                        LocalBackupManager.setEnabled(true);

                    List<Reminder> reminders = RemindersDao.getAllReminders();

                    for (Reminder r :reminders) {
                        RemindersDao.scheduleReminder(r);
                    }
                    return null;
                }

            }.execute();
        }
    }
}

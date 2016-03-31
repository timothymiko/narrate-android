package com.datonicgroup.narrate.app.dataprovider.tasks;

import android.os.AsyncTask;

import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.util.LogUtil;

/**
 * Created by timothymiko on 1/8/15.
 */
public class SaveReminderTask extends AsyncTask<Reminder, Void, Boolean> {

    private Reminder reminder;

    @Override
    protected Boolean doInBackground(Reminder... reminders) {
        this.reminder = reminders[0];
        boolean newReminder = RemindersDao.doesReminderExist(reminder.uuid);
        boolean result = RemindersDao.saveReminder(reminder);

        if ( result ) {
            if ( !newReminder )
                RemindersDao.unscheduleReminder(reminder);

            RemindersDao.scheduleReminder(reminder);
        }

        LogUtil.log("SaveReminderTask", "Reminder Save Result: " + result);
        return result;
    }
}

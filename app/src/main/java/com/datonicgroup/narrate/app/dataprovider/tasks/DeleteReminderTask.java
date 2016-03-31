package com.datonicgroup.narrate.app.dataprovider.tasks;

import android.os.AsyncTask;

import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.models.Reminder;

/**
 * Created by timothymiko on 1/8/15.
 */
public class DeleteReminderTask extends AsyncTask<Reminder, Void, Boolean> {

    private Reminder reminder;

    @Override
    protected Boolean doInBackground(Reminder... reminders) {
        this.reminder = reminders[0];
        boolean result = RemindersDao.unscheduleReminder(reminder);
        result = result && RemindersDao.deleteReminder(reminders[0]);
        return result;
    }
}

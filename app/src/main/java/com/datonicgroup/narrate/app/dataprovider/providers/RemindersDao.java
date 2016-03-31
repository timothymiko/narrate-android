package com.datonicgroup.narrate.app.dataprovider.providers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.datonicgroup.narrate.app.dataprovider.receivers.AlarmReceiver;
import com.datonicgroup.narrate.app.models.Recurrence;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.LogUtil;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders.DATE;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders.DESCRIPTION;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders.RECURRENCE;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders.RECURRING;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders.UUID;
import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.REMINDERS;

/**
 * Created by timothymiko on 9/22/14.
 */
public class RemindersDao {

    /**
     * Database methods
     */

    public static boolean saveReminder(Reminder reminder) {
        if ( doesReminderExist(reminder.uuid) )
            return updateReminder(reminder);
        else
            return addReminder(reminder);
    }

    public static boolean deleteReminder(Reminder reminder) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {
            result = db.delete(REMINDERS, UUID + "=?", new String[]{reminder.uuid});

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
            LogUtil.e("RemindersDao", "Error deleteReminder() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    public static Reminder getReminder(String uuid) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        Reminder reminder = null;
        Cursor cursor = null;

        try {

            cursor = db.query(REMINDERS, null, UUID + "=?", new String[] { uuid }, null, null, null);

            if ((cursor != null) && (cursor.getCount() > 0)) {
                cursor.moveToFirst();
                reminder = fromCursor(cursor);
            }

        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return reminder;
    }

    public static List<Reminder> getAllReminders() {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<Reminder> values = new ArrayList<Reminder>();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("select * from " + REMINDERS, null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    values.add(fromCursor(cursor));
                    cursor.moveToNext();
                }

            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return values;
    }

    public static boolean doesReminderExist(String uuid) {
        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        // Query the database
        Cursor cursor = db.rawQuery("select 1 from " + REMINDERS + " where " + UUID + " ='" + uuid.toUpperCase() + "' LIMIT 1", null);

        // If the query returned anything, return true, else return false
        boolean exists = false;

        if ( cursor != null ) {
            exists = (cursor.getCount() > 0);
            cursor.close();
        }

        // return the result
        return exists;
    }

    private static boolean addReminder(Reminder reminder) {
        LogUtil.log("RemindersDao", "addReminder()");
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        long result = 0;
        try {

            ContentValues args = toValues(reminder);

            result = db.insert(REMINDERS, null, args);

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.e("RemindersDao", "Error addReminder() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    private static boolean updateReminder(Reminder reminder) {
        LogUtil.log("RemindersDao", "updateReminder()");
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {

            ContentValues values = toValues(reminder);

            result = db.update(REMINDERS, values, UUID + "=?", new String[]{reminder.uuid});

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.e("RemindersDao", "Error updateReminder() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    private static Reminder fromCursor(Cursor cursor) {

        Reminder r = new Reminder();

        r.uuid = cursor.getString(1);
        r.description = cursor.getString(2);
        r.recurring = cursor.getInt(3) == 1;
        r.date = new DateTime(cursor.getLong(4));
        r.recurrence = Recurrence.lookup(cursor.getInt(5));
        r.occurrenceString = Reminder.getOccurence(GlobalApplication.getAppContext().getResources(), r);

        return r;
    }

    private static ContentValues toValues(Reminder reminder) {
        ContentValues values = new ContentValues();

        values.put(UUID, reminder.uuid);
        values.put(DESCRIPTION, reminder.description);
        values.put(RECURRING, reminder.recurring ? 1 : 0);

        if ( reminder.date != null )
            values.put(DATE, reminder.date.getMillis());

        values.put(RECURRENCE, reminder.recurrence.getInternalValue());

        return values;
    }

    /**
     * Alarm Manager Methods
     */

    public static boolean scheduleReminder(Reminder reminder) {

        if ( (System.currentTimeMillis() - reminder.date.getMillis()) > 0 )
            return false;

        Intent intent = new Intent(reminder.uuid);
        intent.setClass(GlobalApplication.getAppContext(), AlarmReceiver.class);
        intent.putExtra("description", reminder.description);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(GlobalApplication.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) GlobalApplication.getAppContext().getSystemService(GlobalApplication.getAppContext().ALARM_SERVICE);

        if ( reminder.recurring ) {

            long interval = 0;

            switch ( reminder.recurrence ) {
                case Daily:
                    interval = DateUtil.DAY_IN_MILLISECONDS;
                    am.setRepeating(AlarmManager.RTC_WAKEUP, reminder.date.getMillis(), interval, pendingIntent);
                    break;
                case Weekly:
                    interval = DateUtil.WEEK_IN_MILLISECONDS;
                    am.setRepeating(AlarmManager.RTC_WAKEUP, reminder.date.getMillis(), interval, pendingIntent);
                    break;
                case Monthly:
                    am.set(AlarmManager.RTC_WAKEUP, reminder.date.plusMonths(1).getMillis(), pendingIntent);
                    break;
                case Yearly:
                    am.set(AlarmManager.RTC_WAKEUP, reminder.date.plusYears(1).getMillis(), pendingIntent);
                    break;
            }

        } else {
            am.set(AlarmManager.RTC_WAKEUP, reminder.date.getMillis(), pendingIntent);
        }
        return true;
    }

    public static boolean unscheduleReminder(Reminder reminder) {
        AlarmManager am = (AlarmManager) GlobalApplication.getAppContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(reminder.uuid);
        intent.setClass(GlobalApplication.getAppContext(), AlarmReceiver.class);
        intent.putExtra("description", reminder.description);

        PendingIntent reminderNotificationIntent = PendingIntent.getBroadcast(GlobalApplication.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        am.cancel(reminderNotificationIntent);

        return true;
    }
}

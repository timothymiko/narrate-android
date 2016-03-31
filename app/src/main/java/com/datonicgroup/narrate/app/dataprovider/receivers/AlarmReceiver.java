package com.datonicgroup.narrate.app.dataprovider.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.LocalBackupManager;
import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.models.Recurrence;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.ui.entryeditor.EditEntryActivity;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.util.LogUtil;

/**
 * Created by timothymiko on 9/24/14.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    private static final int GROUP = 1209853254;

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.log(TAG, "onReceive()");
        LogUtil.log(TAG, "intent: " + intent.toString());

        if (intent.getAction().equals("NARRATE_BACKUP")) {
            if ( Settings.getLocalBackupsEnabled() ) {
                LocalBackupManager.backup();
            }
        } else {

            String description = intent.getStringExtra("description");
            String uuid = intent.getAction();
            final Reminder reminder = RemindersDao.getReminder(uuid);

            if (Settings.getRemindersEnabled()) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

                Intent launcherIntent = new Intent(context, EditEntryActivity.class);
                launcherIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

                Notification.Builder notificationBuilder = new Notification.Builder(context)
                        .setContentTitle("Narrate")
                        .setContentText(description)
                        .setSmallIcon(R.drawable.reminder_one_time)
                        .setLargeIcon(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 5, bitmap.getHeight() / 5, false))
                        .setContentIntent(PendingIntent.getActivity(context, 0, launcherIntent, 0))
                        .setAutoCancel(true);

                bitmap.recycle();

                Notification notification;
                if (Build.VERSION.SDK_INT >= 16) {
                    notification = notificationBuilder.build();
                } else {
                    notification = notificationBuilder.getNotification();
                }

                notification.defaults |= Notification.DEFAULT_SOUND;
                notification.defaults |= Notification.DEFAULT_VIBRATE;

                notificationManager.notify(GROUP, notification);
            }

            new AsyncTask<Reminder, Void, Void>() {

                @Override
                protected Void doInBackground(Reminder... params) {

                    Reminder reminder = params[0];

                    if (reminder.recurring) {
                        if (reminder.recurrence == Recurrence.Monthly) {
                            reminder.date = reminder.date.plusMonths(1);
                            RemindersDao.saveReminder(reminder);
                            RemindersDao.scheduleReminder(reminder);
                        } else if (reminder.recurrence == Recurrence.Yearly) {
                            reminder.date = reminder.date.plusYears(1);
                            RemindersDao.saveReminder(reminder);
                            RemindersDao.scheduleReminder(reminder);
                        }
                    } else
                        RemindersDao.deleteReminder(reminder);
                    return null;
                }
            }.execute(reminder);

        }
    }
}

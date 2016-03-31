package com.datonicgroup.narrate.app.dataprovider;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.receivers.AlarmReceiver;
import com.datonicgroup.narrate.app.dataprovider.sync.AbsSyncService;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncInfoManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.FileUtil;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 12/12/14.
 */
public class LocalBackupManager {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm");

    public static void setEnabled(boolean enabled) {
        if (enabled)
            enableBackup();
        else
            disableBackup();
    }

    private static void enableBackup() {
        Intent intent = new Intent("NARRATE_BACKUP");
        intent.setClass(GlobalApplication.getAppContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(GlobalApplication.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) GlobalApplication.getAppContext().getSystemService(GlobalApplication.getAppContext().ALARM_SERVICE);

        int interval = Settings.getLocalBackupFrequency();

        if ( interval > -1 ) {
            long millis = 0;

            switch (interval) {
                case 0:
                    millis = DateUtil.DAY_IN_MILLISECONDS;
                    break;
                case 1:
                    millis = DateUtil.WEEK_IN_MILLISECONDS;
                    break;
                case 2:
                    millis = DateUtil.WEEK_IN_MILLISECONDS * 4;
                    break;
            }

            // delay things by a second
            am.setRepeating(AlarmManager.RTC, Calendar.getInstance().getTimeInMillis() + (30 * DateUtil.SECOND_IN_MILLISECONDS), millis, pendingIntent);
        }
    }

    private static void disableBackup() {
        AlarmManager am = (AlarmManager) GlobalApplication.getAppContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("NARRATE_BACKUP");
        intent.setClass(GlobalApplication.getAppContext(), AlarmReceiver.class);

        PendingIntent reminderNotificationIntent = PendingIntent.getBroadcast(GlobalApplication.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        am.cancel(reminderNotificationIntent);
    }

    public static void backup() {

        new Thread() {
            @Override
            public void run() {
                super.run();

                LogUtil.log("NarrateBackups", "Beginning local backup of Narrate's data.");

                File sdcard = Environment.getExternalStorageDirectory();

                // create 'Narrate Backups' folder
                File backup = new File(sdcard, "/Narrate/");

                // create .pendingbackup folder
                try {

                    // don't keep more than 5 backups
                    File[] backups = backup.listFiles();
                    if ( backups != null && backups.length > 0 ) {
                        List<File> backupList = new ArrayList<File>();
                        for (int i = 0; i < backups.length; i++)
                            backupList.add(backups[i]);

                        // sort oldest to newest
                        Collections.sort(backupList, new Comparator<File>() {
                            @Override
                            public int compare(File lhs, File rhs) {
                                return lhs.getName().compareTo(rhs.getName());
                            }
                        });

                        // delete the oldest backups
                        for (int i = 0; i <= backupList.size()-Settings.getLocalBackupsToKeep(); i++)
                            backupList.get(i).delete();
                    }

                    File pendingFolder = new File(backup, "pendingbackup");

                    // create entries folder
                    File entries = new File(pendingFolder, "entries");
                    entries.mkdirs();

                    // create photos folder
                    File photos = new File(pendingFolder, "photos");
                    photos.mkdirs();

                    // save all entries
                    List<Entry> entryData = EntryHelper.getAllEntries();
                    for (int i = 0; i < entryData.size(); i++) {
                        Entry e = entryData.get(i);
                        String data = EntryHelper.toJson(e);

                        File file = new File(entries, e.uuid);
                        FileOutputStream os = new FileOutputStream(file);

                        PrintWriter writer = new PrintWriter(os);
                        writer.write(data);

                        writer.close();
                        writer.flush();
                        os.close();
                    }

                    // copy all photos
                    File[] photoData = PhotosDao.getPhotosFolder().listFiles();
                    for (int i = 0; i < photoData.length; i++) {

                        File destFile = new File(photos, photoData[i].getName());

                        FileChannel source = null;
                        FileChannel destination = null;
                        try {
                            source = new FileInputStream(photoData[i]).getChannel();
                            destination = new FileOutputStream(destFile).getChannel();
                            destination.transferFrom(source, 0, source.size());
                        } finally {
                            if (source != null)
                                source.close();
                            if (destination != null)
                                destination.close();
                        }
                    }

                    // compress and rename
                    String name = "backup-" + DATE_FORMAT.format(Calendar.getInstance(Locale.getDefault()).getTime());

                    File result = new File(backup, name);
                    FileUtil.zipFileAtPath(pendingFolder, result);

                    // we are finished, delete the .pendingbackup folder
                    FileUtil.deleteDirectory(pendingFolder);

                    LogUtil.log("NarrateBackups", "Narrate local backup complete!");

                } catch (Exception e) {
                    LogUtil.e("NarrateBackups", "Narrate local backup failed.");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public static void restore(File file) {
        LogUtil.log("NarrateBackups", "Beginning restore from local backup.");

        // stop any active sync processes
        SyncHelper.cancelPendingActiveSync(User.getAccount());

        // disable syncing for now
        ContentResolver.setSyncAutomatically(User.getAccount(), Contract.AUTHORITY, false);

        // delete all data on remote clients
        List<AbsSyncService> syncServices;
        if ((syncServices = SyncHelper.getSyncServices()).size() > 0) {
            Iterator<AbsSyncService> it = syncServices.iterator();
            while (it.hasNext()) {
                it.next().deleteEverything();
            }
        }

        File sdcard = Environment.getExternalStorageDirectory();

        // create 'Narrate Backups' folder
        File backup = new File(sdcard, "/Narrate/");

        File dest = new File(backup, "pendingRestore");
        FileUtil.unzip(file, dest.getAbsolutePath());

        File pendingRestore = new File(dest, "pendingBackup");

        // delete old photos
        File photosFolder = PhotosDao.getPhotosFolder();
        File[] existing = photosFolder.listFiles();
        if ( existing != null )
            for (int i = 0; i < existing.length; i++)
                existing[i].delete();

        // copy new photos
        File[] photos = new File(pendingRestore, "photos").listFiles();
        if ( photos != null ) {
            for (int i = 0; i < photos.length; i++) {
                File destFile = new File(photosFolder, photos[i].getName());

                FileChannel source = null;
                FileChannel destination = null;
                try {
                    source = new FileInputStream(photos[i]).getChannel();
                    destination = new FileOutputStream(destFile).getChannel();
                    destination.transferFrom(source, 0, source.size());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (source != null)
                        try {
                            source.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    if (destination != null)
                        try {
                            destination.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }

                Photo photo = new Photo();
                photo.path = photos[i].getAbsolutePath();
                photo.name = photos[i].getName();
                photo.uuid = EntryHelper.getUUIDFromString(photo.name);

                SyncInfoManager.setStatus(photo, SyncStatus.UPLOAD);
            }
        }

        // delete old entries
        EntryHelper.deleteAllEntries();

        // create new entries
        File[] entries = new File(pendingRestore, "entries").listFiles();
        if ( entries != null ) {
            for (int i = 0; i < entries.length; i++) {
                try {
                    FileInputStream is = new FileInputStream(entries[i]);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();
                    String text = new String(buffer);

                    Entry e = EntryHelper.fromJson(text);
                    DataManager.getInstance().save(e, true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // delete the pending restore folder
        FileUtil.deleteDirectory(dest);

        // re-enable syncing
        if (Settings.getDropboxSyncEnabled() || Settings.getGoogleDriveSyncEnabled()) {

            Account acc = User.getAccount();

            ContentResolver.setSyncAutomatically(acc, Contract.AUTHORITY, true);

            long interval = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext())
                    .getString("key_sync_interval", "0"));

            ContentResolver.addPeriodicSync(acc, Contract.AUTHORITY, Bundle.EMPTY, interval);

            Bundle b = new Bundle();
            b.putBoolean("resync_files", true);

            SyncHelper.requestManualSync(acc, b);
        }

        LogUtil.log("NarrateBackups", "Local backup restore complete.");
    }

    public static File[] getBackups() {
        File sdcard = Environment.getExternalStorageDirectory();

        // get 'Narrate Backups' folder
        File backup = new File(sdcard, "/Narrate/");

        return backup.listFiles();
    }
}

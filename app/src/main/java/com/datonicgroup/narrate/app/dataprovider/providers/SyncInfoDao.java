package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.datonicgroup.narrate.app.dataprovider.SelectionBuilder;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo.MODIFIED_DATE;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo.REVISION;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo.SERVICE;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo.STATUS;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo.UUID;
import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.SYNC_INFO;

/**
 * Created by timothymiko on 10/30/14.
 */
public class SyncInfoDao {

    public static List<SyncInfo> getDataForService(SyncService mService) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<SyncInfo> values = new ArrayList<>();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("select * from " + SYNC_INFO + " where " + SERVICE + "='" + mService.ordinal() + "' ORDER BY " + UUID + " DESC", null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                SyncInfo info;
                while (!cursor.isAfterLast()) {
                    info = new SyncInfo(cursor.getString(1));
                    info.setSyncService(cursor.getInt(2));
                    info.setRevision(cursor.getString(3));
                    info.setSyncStatus(cursor.getInt(4));
                    info.setModifiedDate(cursor.getLong(5));

                    values.add(info);
                    cursor.moveToNext();
                }

            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return values;
    }

    public static void saveData(SyncInfo info) {
        LogUtil.log(SyncInfoDao.class.getSimpleName(), "saveData()");
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        long result = 0;
        try {

            // first check if there is an existing sync record for this entry & service in the database
            // Query the database
            Cursor cursor = db.rawQuery("select 1 from " + SYNC_INFO + " where " + UUID + " ='" + info.getTitle() + "' AND " + SERVICE + "='" + info.getSyncService() + "' LIMIT 1", null);
            boolean exists = false;

            if (cursor != null) {
                exists = (cursor.getCount() > 0);
                cursor.close();
            }

            // next insert/update record in database
            ContentValues args = new ContentValues();
            args.put(UUID, info.getTitle());
            args.put(SERVICE, info.getSyncService());
            args.put(REVISION, info.getRevision());
            args.put(STATUS, info.getSyncStatus());
            args.put(MODIFIED_DATE, info.getModifiedDate());

            if (exists)
                result = db.update(SYNC_INFO, args, UUID + "=? AND " + SERVICE + "=?", new String[]{info.getTitle(), String.valueOf(info.getSyncService())});
            else
                result = db.insert(SYNC_INFO, null, args);

            if (result > 0)
                db.setTransactionSuccessful();

        } catch (Exception e) {
            LogUtil.log("SyncInfoDao", "Error saveData() - " + e);
        } finally {
            db.endTransaction();
        }
    }

    public static void deleteAllData() {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();
        final SelectionBuilder builder = new SelectionBuilder();
        builder.table(DatabaseHelper.Tables.SYNC_INFO).delete(db);
    }

    public static SyncInfo getInfo(String uuid, SyncService service) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        Cursor cursor = null;
        SyncInfo info = null;

        try {

            cursor = db.query(SYNC_INFO, null, SERVICE + "=?" + " AND " + UUID + "=?", new String[]{String.valueOf(service.ordinal()), uuid}, null, null, null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                info = new SyncInfo(cursor.getString(1));
                info.setSyncService(cursor.getInt(2));
                info.setRevision(cursor.getString(3));
                info.setSyncStatus(cursor.getInt(4));
                info.setModifiedDate(cursor.getLong(5));

            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return info;
    }

    public static List<SyncInfo> getInfo(String uuid) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<SyncInfo> values = new ArrayList<>();
        Cursor cursor = null;
        SyncInfo info = null;

        try {

            cursor = db.query(SYNC_INFO, null, UUID + "=?", new String[]{uuid}, null, null, null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while ( cursor.moveToNext() ) {
                    info = new SyncInfo(cursor.getString(1));
                    info.setSyncService(cursor.getInt(2));
                    info.setRevision(cursor.getString(3));
                    info.setSyncStatus(cursor.getInt(4));
                    info.setModifiedDate(cursor.getLong(5));
                    values.add(info);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return values;
    }
}

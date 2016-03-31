package com.datonicgroup.narrate.app.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.PendingSyncCall.*;
import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.PENDING_SYNC_CALLS;
import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.PLACES;

/**
 * Created by timothymiko on 1/15/16.
 */
public class PendingSyncCall {

    public enum Operation {
        CREATE(0),
        UPDATE(1),
        DELETE(2);

        private int internalValue;

        private Operation(int internalValue) {
            this.internalValue = internalValue;
        }

        public static Operation lookup(int value) {
            for (Operation status : values()) {
                if (status.internalValue == value) {
                    return status;
                }
            }

            return null;
        }

        public int getInternalValue() {
            return internalValue;
        }
    }

    public SyncService service;
    public Operation operation;
    public String serviceFileId;
    public String name;
    public Date time;

    public static boolean save(PendingSyncCall call) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        long result = 0;
        try {

            ContentValues args = new ContentValues();
            args.put(SERVICE, call.service.getInternalValue());
            args.put(OPERATION, call.operation.getInternalValue());
            args.put(NAME, call.name);
            args.put(TIME, call.time.getTime());
            args.put(SERVICE_FILE_ID, call.serviceFileId);

            result = db.insert(PENDING_SYNC_CALLS, null, args);

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    public static HashMap<String, MutableArrayList<PendingSyncCall>> retrieve(SyncService service, boolean retrievePhotos) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        HashMap<String, MutableArrayList<PendingSyncCall>> results = new HashMap<>();
        Cursor cursor = null;

        try {

            String fileType = retrievePhotos ? ".jpg" : ".json";
            cursor = db.rawQuery("select * from " + PENDING_SYNC_CALLS + " where " + SERVICE + "=? AND " + NAME + " LIKE '%" + fileType + "'", new String[] { String.valueOf(service.ordinal()) });

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    PendingSyncCall call = new PendingSyncCall();
                    call.service = SyncService.lookup(cursor.getInt(1));
                    call.operation = Operation.lookup(cursor.getInt(2));
                    call.name = cursor.getString(3);
                    call.time = new Date(cursor.getLong(4));
                    call.serviceFileId = cursor.getString(5);

                    MutableArrayList<PendingSyncCall> calls;
                    if (results.containsKey(call.name)) {
                        calls = results.get(call.name);
                    } else {
                        calls = new MutableArrayList<>();
                    }

                    calls.add(call);
                    results.put(call.name, calls);

                    cursor.moveToNext();
                }

            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return results;
    }

    public static boolean delete(SyncService service, String fileId) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {
            result = db.delete(PENDING_SYNC_CALLS, SERVICE + "=? AND " + SERVICE + "=?", new String[]{String.valueOf(service.ordinal()), fileId});

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    public static boolean delete(String name) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {
            result = db.delete(PENDING_SYNC_CALLS, NAME + "=?", new String[]{name});

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    public static boolean deleteAll(SyncService service, String name) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {
            result = db.delete(PENDING_SYNC_CALLS, SERVICE + "=? AND " + NAME + "=?", new String[]{ String.valueOf(service.ordinal()), name });

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }
}

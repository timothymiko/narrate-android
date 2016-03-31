package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.TAGS;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Tags.*;

/**
 * Created by timothymiko on 9/27/14.
 */
public class TagsDao {

    public static void storeTag(String tag) {
        if ( doesTagExist(tag) )
            updateTag(tag);
        else
            addTag(tag);
    }

    public static List<String> getTags() {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<String> values = new ArrayList<>();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("select * from " + TAGS, null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    values.add(cursor.getString(1));

                    cursor.moveToNext();
                }

            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return values;
    }

    public static List<String> getTags(String startsWith) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<String> values = new ArrayList<>();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("select * from " + TAGS + " where " + TAG + " like '?%'", new String[] { startsWith });

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    values.add(cursor.getString(1));

                    cursor.moveToNext();
                }

            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return values;
    }

    private static boolean addTag(String tag) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        long result = 0;
        try {

            ContentValues args = new ContentValues();
            args.put(TAG, tag);

            result = db.insert(TAGS, null, args);

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.log("EntriesDao", "Error addEntry() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    private static boolean updateTag(String tag) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {

            ContentValues args = new ContentValues();
            args.put(TAG, tag);

            result = db.update(TAGS, args, TAG + "=?", new String[] { tag });

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.e("EntriesDao", "Error updateEntry() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    private static boolean doesTagExist(String tag) {

        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        // Query the database
        Cursor cursor = db.rawQuery("select * from " + TAGS + " where " + TAG + "=?", new String[] { tag });

        // If the query returned anything, return true, else return false
        boolean exists = false;

        if ( cursor != null ) {
            exists = (cursor.getCount() > 0);
            cursor.close();
        }

        // return the result
        return exists;

    }
}

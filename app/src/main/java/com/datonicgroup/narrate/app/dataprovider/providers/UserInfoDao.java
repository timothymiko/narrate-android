package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.Calendar;

import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.USER_INFO;
import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.UserInfo.*;

/**
 * Created by timothymiko on 8/5/14.
 */
public class UserInfoDao {

    public static boolean isProUser() {
        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        // Query the database
        Cursor cursor = db.rawQuery("select " + PRO_USER + " from " + USER_INFO, null);

        // If the query returned anything, return true, else return false
        boolean isProUser = false;
        if ( (cursor != null) && (cursor.getCount() > 0) ) {
            cursor.moveToFirst();
            isProUser = cursor.getInt(0) == 1;
            cursor.close();
        }

        // return the result
        return isProUser;
    }

    public static void updateUserInfo(boolean proUser, Calendar queryDate) {

        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();

        Cursor cursor = db.rawQuery("select * from " + USER_INFO, null);

        ContentValues args = new ContentValues();
        args.put(PRO_USER, proUser ? 1 : 0);
        args.put(QUERY_DATE, queryDate.getTimeInMillis());

        db.beginTransaction();
        try {
            if ( (cursor == null) || (cursor.getCount() == 0) ) {
                db.insert(USER_INFO, null, args);
            } else {
                cursor.moveToFirst();
                db.update(USER_INFO, args, "_id=?", new String[] { String.valueOf(cursor.getInt(0)) });
            }
            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.e("UserInfoDao", "Error updateUserInfo() - " + e);
        } finally {
            db.endTransaction();
        }

    }

    public static long getLastQueryDate() {
        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        // Query the database
        Cursor cursor = db.rawQuery("select * from " + USER_INFO, null);

        // If the query returned anything, return true, else return false
        long date = 0;

        if ( (cursor != null) && (cursor.getCount() > 0) ) {
            cursor.moveToFirst();
            date = cursor.getLong(2);
            cursor.close();
            LogUtil.log("UserInfoDao", "Last Query: " + date);
        }

        return date;
    }
}

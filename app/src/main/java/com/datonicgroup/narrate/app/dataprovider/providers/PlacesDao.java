package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LocationUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static com.datonicgroup.narrate.app.dataprovider.providers.Contract.Places.*;
import static com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables.PLACES;

/**
 * Created by timothymiko on 9/27/14.
 */
public class PlacesDao {

    public static void storePlace(String place, double lat, double lng) {
        if ( doesPlaceExist(place) )
            updatePlace(place, lat, lng);
        else
            addPlace(place, lat, lng);
    }

    public static List<Pair<String, LatLng>> getPlaces(double lat, double lng, double radius) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        ArrayList<Pair<String, LatLng>> values = new ArrayList<>();
        Cursor cursor = null;

        try {

            cursor = db.rawQuery("select * from " + PLACES, null);

            // Check to make sure that the query returned anything
            if ((cursor != null) && (cursor.getCount() > 0)) {

                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {

                    LatLng n = new LatLng(cursor.getDouble(2), cursor.getDouble(3));
                    if (LocationUtil.distanceBetweenLocations(lat, lng, n.latitude, n.longitude) < radius )
                        values.add(new Pair<>(cursor.getString(1), n));

                    cursor.moveToNext();
                }

            }
        } finally {
            if ( cursor != null )
                cursor.close();
        }

        return values;
    }

    private static boolean addPlace(String name, double lat, double lng) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        long result = 0;
        try {

            ContentValues args = new ContentValues();
            args.put(NAME, name);
            args.put(LATITUDE, lat);
            args.put(LONGITUDE, lng);

            result = db.insert(PLACES, null, args);

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.log("EntriesDao", "Error addEntry() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    private static boolean updatePlace(String name, double lat, double lng) {
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getWritableDatabase();
        db.beginTransaction();
        int result = 0;
        try {

            ContentValues args = new ContentValues();
            args.put(NAME, name);
            args.put(LATITUDE, lat);
            args.put(LONGITUDE, lng);

            result = db.update(PLACES, args, NAME + "=?", new String[] { name });

            db.setTransactionSuccessful();
        } catch ( Exception e ) {
            LogUtil.e("EntriesDao", "Error updateEntry() - " + e);
        } finally {
            db.endTransaction();
        }

        return result > 0;
    }

    public static boolean doesPlaceExist(String place) {

        // Open the database
        SQLiteDatabase db = DatabaseHelper.getInstance(GlobalApplication.getAppContext()).getReadableDatabase();

        // Query the database
        Cursor cursor = db.rawQuery("select * from " + PLACES + " where " + NAME + "=?", new String[] { place });

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

package com.datonicgroup.narrate.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.ui.GlobalApplication;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by timothymiko on 7/3/14.
 */
public class SettingsUtil {

    /**
     * Name of preference file containing a single boolean indicating the user has opened the app
     * for the first time and completed the setup process.
     */
    private static final String firstRunPrefs = "FirstRunPrefs";

    private static final String gDrivePrefs = "GoogleDrivePrefs";

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM d, yy", Locale.getDefault());
    private static final SimpleDateFormat mTimeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    /**
     * This method should be called when the user completes the setup process. It writes out a
     * boolean to SharedPrefences indicating the process has been compeleted. Call
     * {@link com.datonicgroup.narrate.app.util.SettingsUtil#isFirstRun(android.content.Context)}
     * to retrieve this value in code.
     *
     * @param mContext
     */
    public static void setupCompleted(Context mContext) {
        final SharedPreferences preferences = mContext.getSharedPreferences(firstRunPrefs, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = preferences.edit();
        prefsEditor.putBoolean("isFirstRun", false);
        prefsEditor.apply();
    }

    /**
     * Determines if the app is being opened for the first time.
     *
     * @param mContext
     *
     * @return true if the app is being run for the first time, false otherwise
     */
    public static boolean isFirstRun(Context mContext) {
        final SharedPreferences preferences = mContext.getSharedPreferences(firstRunPrefs, Context.MODE_PRIVATE);
        return preferences.getBoolean("isFirstRun", true);
    }

    public static boolean shouldRefreshGDriveToken() {
        final SharedPreferences preferences = GlobalApplication.getAppContext().getSharedPreferences(gDrivePrefs, Context.MODE_PRIVATE);
        long time = preferences.getLong("lastRefreshTime", -1);

        if ( time == -1 )
            return true;

        if ( (System.currentTimeMillis() - time) > (DateUtil.MINUTE_IN_MILLISECONDS * 50) )
            return true;

        return false;
    }

    public static void refreshGDriveToken() {
        final SharedPreferences preferences = GlobalApplication.getAppContext().getSharedPreferences(gDrivePrefs, Context.MODE_PRIVATE);
        final SharedPreferences.Editor prefsEditor = preferences.edit();
        prefsEditor.putLong("lastRefreshTime", System.currentTimeMillis());
        prefsEditor.apply();
    }

    public static void setSyncStatus(String status) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        final SharedPreferences.Editor prefsEditor = preferences.edit();
        prefsEditor.putString("key_sync_status", status);
        prefsEditor.apply();
    }

    public static void setSyncStatus(long lastSyncTime) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(lastSyncTime);

        StringBuilder sb = new StringBuilder();
        sb.append(GlobalApplication.getAppContext().getString(R.string.last_sync));
        sb.append(" ");
        sb.append(mDateFormat.format(date.getTime()));
        sb.append(GlobalApplication.getAppContext().getString(R.string.at));
        sb.append(mTimeFormat.format(date.getTime()));

        setSyncStatus(sb.toString());
    }

    public static String getSyncStatus() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        return preferences.getString("key_sync_status", "N/A");
    }
}

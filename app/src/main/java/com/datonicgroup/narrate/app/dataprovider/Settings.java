package com.datonicgroup.narrate.app.dataprovider;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.models.SyncFolderType;
import com.datonicgroup.narrate.app.ui.GlobalApplication;

/**
 * Created by timothymiko on 12/30/14.
 */
public abstract class Settings {

    private static void setKeyValue(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getValue(String key, boolean def) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        return prefs.getBoolean(key, def);
    }

    private static void setKeyValue(String key, int value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static int getValue(String key, int def) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        return prefs.getInt(key, def);
    }

    private static void setKeyValue(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static long getValue(String key, long def) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        return prefs.getLong(key, def);
    }

    private static void setKeyValue(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext()).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getValue(String key, String def) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GlobalApplication.getAppContext());
        return prefs.getString(key, def);
    }

    public static void setSaveSortFilter(boolean save) {
        setKeyValue("setting_persist_sort_filter", save);
    }

    public static boolean getSaveSortFilter() {
        return getValue("setting_persist_sort_filter", true);
    }

    public static void setSavePhotos(boolean save) {
        setKeyValue("setting_save_photos_camera_roll", save);
    }

    public static boolean getSavePhotos() {
        return getValue("setting_save_photos_camera_roll", true);
    }

    public static void setAutomaticLocation(boolean enabled) {
        setKeyValue("setting_automatic_location", enabled);
    }

    public static boolean getAutomaticLocation() {
        return getValue("setting_automatic_location", true);
    }

    public static void setTwentyFourHourTime(boolean enabled) {
        setKeyValue("setting_twentyfour_hour_time", enabled);
    }

    public static boolean getTwentyFourHourTime() {
        return getValue("setting_twentyfour_hour_time", false);
    }

    public static void setRemindersEnabled(boolean enabled) {
        setKeyValue("setting_reminders_enabled", enabled);
    }

    public static boolean getRemindersEnabled() {
        return getValue("setting_reminders_enabled", false);
    }

    public static void setSyncEnabled(boolean enabled) {
        setKeyValue("setting_sync_enabled", enabled);
    }

    public static boolean getSyncEnabled() {
        return getValue("setting_sync_enabled", false);
    }

    public static void setLocalBackupsEnabled(boolean enabled) {
        setKeyValue("setting_local_backups_enabled", enabled);
    }

    public static boolean getLocalBackupsEnabled() {
        return getValue("setting_local_backups_enabled", false);
    }

    public static void setGoogleDriveSyncEnabled(boolean enabled) {
        setKeyValue("setting_sync_drive_enabled", enabled);
    }

    public static boolean getGoogleDriveSyncEnabled() {
        return getValue("setting_sync_drive_enabled", false);
    }

    public static void setGoogleDriveSyncPageToken(String token) {
        setKeyValue("setting_sync_drive_pagetoken", token);
    }

    public static String getGoogleDriveSyncPageToken() {
        return getValue("setting_sync_drive_pagetoken", null);
    }

    public static void setGoogleDriveSyncEntriesFolderId(String id) {
        setKeyValue("setting_sync_drive_entriesfolderid", id);
    }

    public static String getGoogleDriveSyncEntriesFolderId() {
        return getValue("setting_sync_drive_entriesfolderid", null);
    }

    public static void setGoogleDriveSyncPhotosFolderId(String id) {
        setKeyValue("setting_sync_drive_photosfolderid", id);
    }

    public static String getGoogleDriveSyncPhotosFolderId() {
        return getValue("setting_sync_drive_photosfolderid", null);
    }

    public static void setGoogleDriveManualSyncPending(boolean pending) {
        setKeyValue("setting_sync_drive_pendingmanualsync", pending);
    }

    public static boolean getGoogleDriveManualSyncPending() {
        return getValue("setting_sync_drive_pendingmanualsync", false);
    }

    public static void setDropboxSyncEnabled(boolean enabled) {
        setKeyValue("setting_sync_dropbox_enabled", enabled);
    }

    public static boolean getDropboxSyncEnabled() {
        return getValue("setting_sync_dropbox_enabled", false);
    }

    public static void setDropboxSyncFolder(String folder) {
        setKeyValue("setting_sync_dropbox_folder", folder);
    }

    public static String getDropboxSyncFolder() {
        return getValue("setting_sync_dropbox_folder", null);
    }

    public static void setDropboxSyncFolderType(SyncFolderType type) {
        setKeyValue("setting_sync_dropbox_folder_type", type.ordinal());
    }

    public static int getDropboxSyncFolderType() {
        return getValue("setting_sync_dropbox_folder_type", 0);
    }

    public static void setDropboxSyncDayOne(boolean enabled) {
        setKeyValue("setting_sync_dropbox_dayone", enabled);
    }

    public static boolean getDropboxSyncDayOne() {
        return getValue("setting_sync_dropbox_dayone", false);
    }

    public static void setDropboxSyncToken(String token) {
        setKeyValue("setting_sync_dropbox_token", token);
    }

    public static String getDropboxSyncToken() {
        return getValue("setting_sync_dropbox_token", null);
    }

    public static void setSyncOnMobileData(boolean sync) {
        setKeyValue("setting_sync_mobile_data", sync);
    }

    public static boolean getSyncOnMobileData() {
        return getValue("setting_sync_mobile_data", false);
    }

    public static void setAutoSyncInterval(int interval) {
        setKeyValue("setting_sync_auto_interval", interval);
    }

    public static int getAutoSyncInterval() {
        return getValue("setting_sync_auto_interval", -1);
    }

    public static void setLocalBackupFrequency(int frequency) {
        setKeyValue("setting_local_backup_frequency", frequency);
    }

    public static int getLocalBackupFrequency() {
        return getValue("setting_local_backup_frequency", -1);
    }

    public static void setLocalBackupsToKeep(int maxBackups) {
        setKeyValue("setting_local_backups_max", maxBackups);
    }

    public static int getLocalBackupsToKeep() {
        return getValue("setting_local_backups_max", 5);
    }

    public static void setPasscodeLockEnabled(boolean enabled) {
        setKeyValue("setting_passcode_enabled", enabled);
    }

    public static boolean getPasscodeLockEnabled() {
        return getValue("setting_passcode_enabled", false);
    }

    public static void setPasscodeLockTimeout(int timeout) {
        setKeyValue("setting_passcode_timeout", timeout);
    }

    public static int getPasscodeLockTimeout() {
        return getValue("setting_passcode_timeout", 5000);
    }

    public static void setEmail(String email) {
        setKeyValue("email", email);
    }

    public static String getEmail() {
        return getValue("email", null);
    }

    public static void setGoogleAccountName(String name) {
        setKeyValue("google_account_name", name);
    }

    public static String getGoogleAccountName() {
        return getValue("google_account_name", null);
    }

    public static void setDeveloperModeEnabled(boolean enabled) {
        setKeyValue("developerModeEnabled", enabled);
    }

    public static boolean getDeveloperModeEnabled() {
        return getValue("developerModeEnabled", false);
    }

    public static void setLoggingEnabled(boolean enabled) {
        setKeyValue("loggingEnabled", enabled);
    }

    public static boolean getLoggingEnabled() {
        return getValue("loggingEnabled", false);
    }

    public static void setShownReviewDialog(boolean shown) {
        setKeyValue("reviewDialogDisplayed", shown);
    }

    public static boolean getShownReviewDialog() {
        return getValue("reviewDialogDisplayed", false);
    }

    public static void setLoginCount(int count) {
        setKeyValue("numOfLogins", count);
    }

    public static int getLoginCount() {
        return getValue("numOfLogins", 0);
    }

    public static void setAppVersion(int version) {
        setKeyValue("versionCode", version);
    }

    public static int getAppVersion() {
        return getValue("versionCode", BuildConfig.VERSION_CODE);
    }

    public static void setJoinDate(long joinDate) {
        setKeyValue("joinDate", joinDate);
    }

    public static long getJoinDate() {
        return getValue("joinDate", 0l);
    }

    public static void setEntryCount(int count) {
        setKeyValue("key_num_entries", count);
    }

    public static int getEntryCount() {
        return getValue("key_num_entries", BuildConfig.VERSION_CODE);
    }

    public static void setUserId(String id) {
        setKeyValue("narrate_parse_user_id", id);
    }

    public static String getUserId() {
        return getValue("narrate_parse_user_id", null);
    }
}

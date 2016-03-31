package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Entries;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.PendingSyncCall;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Places;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Reminders;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.SyncInfo;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Tags;
import com.datonicgroup.narrate.app.dataprovider.providers.Contract.UserInfo;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.models.User;

/**
 * Created by timothymiko on 11/26/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "data.db";

    private static final int CUR_DATABASE_VERSION = 8;

    private final Context mContext;

    public interface Tables {
        String ENTRIES = "Entries";
        String USER_INFO = "UserInfo";
        String REMINDERS = "Reminders";
        String TAGS = "Tags";
        String PLACES = "Places";
        String SYNC_INFO = "SyncInfo";
        String PENDING_SYNC_CALLS = "PendingSyncCalls";
    }

    private static DatabaseHelper sInstance;

    public static DatabaseHelper getInstance(Context context) {
        if ( sInstance == null )
            sInstance = new DatabaseHelper(context);

        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, CUR_DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.ENTRIES + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Entries.UUID + " TEXT NOT NULL,"
                + Entries.CREATION_DATE + " INTEGER,"
                + Entries.MODIFIED_DATE + " INTEGER, "
                + Entries.TITLE + " TEXT DEFAULT '',"
                + Entries.TEXT + " TEXT DEFAULT '',"
                + Entries.PLACE_NAME + " TEXT,"
                + Entries.LATITUDE + " REAL DEFAULT 0,"
                + Entries.LONGITUDE + " REAL DEFAULT 0,"
                + Entries.STARRED + " INTEGER DEFAULT 0,"
                + Entries.TAGS_LIST + " TEXT,"
                + Entries.DELETED + " INTEGER,"
                + Entries.DELETION_DATE + " INTEGER,"
                + Entries.GOOGLE_DRIVE_FILE_ID + " TEXT,"
                + Entries.GOOGLE_DRIVE_PHOTO_FILE_ID + " TEXT,"
                + "UNIQUE (" + Entries.UUID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.USER_INFO + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + UserInfo.PRO_USER + " INTEGER,"
                + UserInfo.QUERY_DATE + " TEXT NOT NULL"
                + ");");

        db.execSQL("CREATE TABLE " + Tables.REMINDERS + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Reminders.UUID + " TEXT NOT NULL,"
                + Reminders.DESCRIPTION + " TEXT,"
                + Reminders.RECURRING + " INTEGER,"
                + Reminders.DATE + " INTEGER,"
                + Reminders.RECURRENCE + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE " + Tables.TAGS + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Tags.TAG + " TEXT NOT NULL,"
                + Tags.COUNT + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE " + Tables.PLACES + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + Places.NAME + " TEXT NOT NULL,"
                + Places.LATITUDE + " INTEGER,"
                + Places.LONGITUDE + " INTEGER"
                +");");

        db.execSQL("CREATE TABLE " + Tables.SYNC_INFO + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + SyncInfo.UUID + " TEXT NOT NULL COLLATE NOCASE,"
                + SyncInfo.SERVICE + " INTEGER,"
                + SyncInfo.REVISION + " TEXT,"
                + SyncInfo.STATUS + " INTEGER,"
                + SyncInfo.MODIFIED_DATE + " INTEGER"
                + ");");

        db.execSQL("CREATE TABLE " + Tables.PENDING_SYNC_CALLS + "("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + PendingSyncCall.SERVICE + " INTEGER,"
                + PendingSyncCall.OPERATION + " INTEGER,"
                + PendingSyncCall.NAME + " TEXT NOT NULL,"
                + PendingSyncCall.TIME + " INTEGER,"
                + PendingSyncCall.SERVICE_FILE_ID + " TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("Database", "onUpgrade() from " + oldVersion + " to " + newVersion);

        // this might fail depending on if the user has syncing enabled
        try {
            SyncHelper.cancelPendingActiveSync(User.getAccount());
        } catch ( Exception e ) {

        }

        switch (oldVersion) {
            case 1:
                db.execSQL("CREATE TABLE UserInfo(isProUser INTEGER, lastQueryDate TEXT NOT NULL);");
            case 2:
                // create temporary backup table
                db.execSQL("create temporary table entries_backup ("
                        + "_id integer primary key autoincrement, "
                        + "uuid text not null, "
                        + "revisionKey text, "
                        + "creationDate integer, "
                        + "modifiedDate integer, "
                        + "text text, "
                        + "hasLocation integer, "
                        + "latitude real, "
                        + "longitude real, "
                        + "isStarred integer, "
                        + "tagsList text, "
                        + "deleted integer, "
                        + "deletionDate integer, "
                        + "dropboxSyncStatus integer);");

                // copy over the data
                db.execSQL("INSERT INTO entries_backup SELECT "
                        + "_id, uuid, revisionKey,  creationDate, modifiedDate, text, hasLocation, latitude, "
                        + "longitude, isStarred, tagsList, deleted, deletionDate, dropboxSyncStatus"
                        + " FROM EntriesTable");

                // drop the main table
                db.execSQL("DROP TABLE EntriesTable");

                // create new, updated main table
                db.execSQL("CREATE TABLE EntriesTable ("
                        + "_id integer primary key autoincrement, "
                        + "uuid text not null, "
                        + "revisionKey text, "
                        + "creationDate integer, "
                        + "modifiedDate integer, "
                        + "title text, "
                        + "text text, "
                        + "hasLocation integer, "
                        + "latitude real, "
                        + "longitude real, "
                        + "isStarred integer, "
                        + "tagsList text, "
                        + "deleted integer, "
                        + "deletionDate integer, "
                        + "dropboxSyncStatus integer);");

                // copy data back in
                db.execSQL("INSERT INTO EntriesTable SELECT "
                        + "_id, uuid, revisionKey,  creationDate, modifiedDate, null, text, hasLocation, latitude, "
                        + "longitude, isStarred, tagsList, deleted, deletionDate, dropboxSyncStatus"
                        + " FROM entries_backup");

                // drop the backup table
                db.execSQL("DROP TABLE entries_backup");
            case 3:
                db.execSQL("CREATE TABLE Reminders("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uuid TEXT NOT NULL,"
                        + "description TEXT,"
                        + "recurring INTEGER,"
                        + "date INTEGER,"
                        + "recurrence INTEGER"
                        + ");");

                db.execSQL("CREATE TABLE Tags("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "tag TEXT NOT NULL,"
                        + "count INTEGER"
                        + ");");
            case 4:
                db.execSQL("CREATE TABLE IF NOT EXISTS Places("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "placeName TEXT NOT NULL,"
                        + "latitude INTEGER,"
                        + "longitude INTEGER"
                        +");");

                // create temporary backup table
                db.execSQL("create temporary table entries_backup ("
                        + "_id integer primary key autoincrement, "
                        + "uuid text not null, "
                        + "revisionKey text, "
                        + "creationDate integer, "
                        + "modifiedDate integer, "
                        + "title text, "
                        + "text text, "
                        + "hasLocation integer, "
                        + "latitude real, "
                        + "longitude real, "
                        + "isStarred integer, "
                        + "tagsList text, "
                        + "deleted integer, "
                        + "deletionDate integer, "
                        + "dropboxSyncStatus integer);");

                // copy over the data
                db.execSQL("INSERT INTO entries_backup SELECT _id, uuid, revisionKey,  creationDate, modifiedDate, title, text, hasLocation, latitude, "
                        + "longitude, isStarred, tagsList, deleted, deletionDate, dropboxSyncStatus"
                        + " FROM EntriesTable");

                // drop the main table
                db.execSQL("DROP TABLE EntriesTable");

                // create new, updated main table
                db.execSQL("create table EntriesTable ("
                        + "_id integer primary key autoincrement, "
                        + "uuid text not null, "
                        + "revisionKey text, "
                        + "creationDate integer, "
                        + "modifiedDate integer, "
                        + "title text, "
                        + "text text, "
                        + "hasLocation integer, "
                        + "placeName text, "
                        + "latitude real, "
                        + "longitude real, "
                        + "isStarred integer, "
                        + "tagsList text, "
                        + "deleted integer, "
                        + "deletionDate integer, "
                        + "dropboxSyncStatus integer);");

                // copy data back in
                db.execSQL("INSERT INTO EntriesTable SELECT _id, uuid, revisionKey,  creationDate, modifiedDate, "
                        + "title, text, hasLocation, null, latitude, longitude, isStarred, tagsList, "
                        + "deleted, deletionDate, dropboxSyncStatus"
                        + " FROM entries_backup");

                // drop the backup table
                db.execSQL("DROP TABLE entries_backup");

                db.execSQL("CREATE TABLE SyncInfo("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uuid TEXT NOT NULL,"
                        + "syncService INTEGER,"
                        + "revisionKey TEXT,"
                        + "syncStatus INTEGER,"
                        + "modifiedDate INTEGER);");
            case 5:
                // create the new entries table
                db.execSQL("CREATE TABLE Entries("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uuid TEXT NOT NULL,"
                        + "creationDate INTEGER,"
                        + "modifiedDate INTEGER, "
                        + "title TEXT DEFAULT '',"
                        + "text TEXT DEFAULT '',"
                        + "placeName TEXT,"
                        + "latitude REAL DEFAULT 0,"
                        + "longitude REAL DEFAULT 0,"
                        + "isStarred INTEGER DEFAULT 0,"
                        + "tagsList TEXT,"
                        + "deleted INTEGER,"
                        + "deletionDate INTEGER,"
                        + "UNIQUE (uuid) ON CONFLICT REPLACE)");

                // create a backup table
                db.execSQL("INSERT INTO Entries SELECT _id, uuid, creationDate, modifiedDate, "
                        + "title, text, placeName, latitude, longitude, isStarred, tagsList, "
                        + "deleted, deletionDate"
                        + " FROM EntriesTable");

                // drop the old table
                db.execSQL("DROP TABLE EntriesTable");
            case 6:
                // create backup sync info table
                db.execSQL("CREATE TABLE sync_info_backup("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uuid TEXT NOT NULL,"
                        + "syncService INTEGER,"
                        + "revisionKey TEXT,"
                        + "syncStatus INTEGER,"
                        + "modifiedDate INTEGER"
                        + ");");

                // copy existing data into backup table
                db.execSQL("INSERT INTO sync_info_backup SELECT " +
                        "_id, uuid, syncService, revisionKey, syncStatus, modifiedDate " +
                        "FROM SyncInfo");

                // drop existing table
                db.execSQL("DROP TABLE SyncInfo");

                // create new table
                db.execSQL("CREATE TABLE SyncInfo("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uuid TEXT NOT NULL COLLATE NOCASE,"
                        + "syncService INTEGER,"
                        + "revisionKey TEXT,"
                        + "syncStatus INTEGER,"
                        + "modifiedDate INTEGER"
                        + ");");

                // copy data into new table
                db.execSQL("INSERT INTO SyncInfo SELECT " +
                        "_id, uuid, syncService, revisionKey, syncStatus, modifiedDate " +
                        "FROM sync_info_backup");

                // drop old table
                db.execSQL("DROP TABLE sync_info_backup");
            case 7:
                db.execSQL("ALTER TABLE Entries ADD COLUMN GOOGLE_DRIVE_FILE_ID TEXT;");
                db.execSQL("ALTER TABLE Entries ADD COLUMN GOOGLE_DRIVE_PHOTO_FILE_ID TEXT;");

                db.execSQL("CREATE TABLE PendingSyncCalls("
                        + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "syncService INTEGER,"
                        + "operation INTEGER,"
                        + "name TEXT NOT NULL,"
                        + "time INTEGER,"
                        + "serviceFileId TEXT"
                        + ");");
        }
    }

    public static void deleteDatabase(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }
}

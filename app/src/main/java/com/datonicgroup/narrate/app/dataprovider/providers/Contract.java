package com.datonicgroup.narrate.app.dataprovider.providers;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.datonicgroup.narrate.app.models.SyncService;

import org.joda.time.DateTime;

import java.util.HashMap;

/**
 * Created by timothymiko on 11/26/14.
 */
public class Contract {

    public static final String AUTHORITY = "com.datonicgroup.narrate.app.provider";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final String PATH_ENTRIES = "entries";

    public static final String[] TOP_LEVEL_PATHS = {
            PATH_ENTRIES
    };

    interface EntryColumns {

        String UUID = "uuid";
        String CREATION_DATE = "creationDate";
        String MODIFIED_DATE = "modifiedDate";
        String TITLE = "title";
        String TEXT = "text";
        String PLACE_NAME = "placeName";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String STARRED = "isStarred";
        String TAGS_LIST = "tagsList";
        String DELETED = "deleted";
        String DELETION_DATE = "deletionDate";
        String GOOGLE_DRIVE_FILE_ID = "GOOGLE_DRIVE_FILE_ID";
        String GOOGLE_DRIVE_PHOTO_FILE_ID = "GOOGLE_DRIVE_PHOTO_FILE_ID";

    }

    public static class Entries implements EntryColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_ENTRIES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.entries";

        public static HashMap<String, String> PROJ_ALL = new HashMap<String, String>() {{
            put(_ID, _ID);
            put(UUID, UUID);
            put(CREATION_DATE, CREATION_DATE);
            put(MODIFIED_DATE, MODIFIED_DATE);
            put(TITLE, TITLE);
            put(TEXT, TEXT);
            put(PLACE_NAME, PLACE_NAME);
            put(LATITUDE, LATITUDE);
            put(LONGITUDE, LONGITUDE);
            put(STARRED, STARRED);
            put(TAGS_LIST, TAGS_LIST);
            put(DELETED, DELETED);
            put(DELETION_DATE, DELETION_DATE);
            put(GOOGLE_DRIVE_FILE_ID, GOOGLE_DRIVE_FILE_ID);
            put(GOOGLE_DRIVE_PHOTO_FILE_ID, GOOGLE_DRIVE_PHOTO_FILE_ID);
        }};

        public static String[] PROJ_ALL_ARRAY = {
                _ID,
                UUID,
                CREATION_DATE,
                MODIFIED_DATE,
                TITLE,
                TEXT,
                PLACE_NAME,
                LATITUDE,
                LONGITUDE,
                STARRED,
                TAGS_LIST,
                DELETED,
                DELETION_DATE,
                GOOGLE_DRIVE_FILE_ID,
                GOOGLE_DRIVE_PHOTO_FILE_ID
        };

        public static Uri buildEntryUri(String uuid) {
            return CONTENT_URI.buildUpon().appendPath(uuid).build();
        }
    }

    interface UserInfoColumns {
        String PRO_USER = "isProUser";
        String QUERY_DATE = "lastQueryDate";
    }

    public static class UserInfo implements UserInfoColumns, BaseColumns {
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.user-info";
    }

    interface ReminderColumns {
        String UUID = "uuid";
        String DESCRIPTION = "description";
        String RECURRING = "recurring";
        String DATE = "date";
        String RECURRENCE = "recurrence";
    }

    public static class Reminders implements ReminderColumns, BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.reminders";
    }

    interface TagsColumns {
        String TAG = "tag";
        String COUNT = "count";
    }

    public static class Tags implements TagsColumns, BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.tags";
    }

    interface PlaceColumns {
        String NAME = "placeName";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
    }

    public static class Places implements PlaceColumns, BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.places";
    }

    interface SyncInfoColumns {
        String UUID = "uuid";
        String SERVICE = "syncService";
        String REVISION = "revisionKey";
        String STATUS = "syncStatus";
        String MODIFIED_DATE = "modifiedDate";
    }

    public static class SyncInfo implements SyncInfoColumns, BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.syncinfo";
    }

    interface PendingSyncCallColumns {
        String SERVICE = "syncService";
        String OPERATION = "operation";
        String SERVICE_FILE_ID = "serviceFileId";
        String NAME = "name";
        String TIME = "time";
    }

    public static class PendingSyncCall implements PendingSyncCallColumns, BaseColumns {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.narrate.filechange";
    }

    public static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    public static boolean hasCallerIsSyncAdapterParameter(Uri uri) {
        return TextUtils.equals("true",
                uri.getQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER));
    }

    private Contract() { }
}

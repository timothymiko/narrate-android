package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Entries;
import com.datonicgroup.narrate.app.dataprovider.providers.DatabaseHelper.Tables;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.dataprovider.SelectionBuilder;

import java.util.ArrayList;

/**
 * Created by timothymiko on 11/25/14.
 */
public class DataProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int ENTRIES = 100;
    private static final int ENTRIES_ID = 101;

    private DatabaseHelper mDatabaseHelper;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = Contract.AUTHORITY;

        // Add a pattern that routes URIs terminated with "entries" to an ENTRIES operation
        // that is performed on the whole table
        matcher.addURI(authority, "entries", ENTRIES);

        // Add a pattern that routes URIs terminated with a number to an ENTRIES operation
        // that is aimed at a specific entry where the path ends with its uuid
        matcher.addURI(authority, "entries/*", ENTRIES_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = DatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ENTRIES:
            case ENTRIES_ID:
                return Entries.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        final SelectionBuilder builder = new SelectionBuilder();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ENTRIES: {
                builder
                        .table(Tables.ENTRIES)
                        .map(Entries.PROJ_ALL);
                break;
            }
            case ENTRIES_ID: {
                builder
                        .table(Tables.ENTRIES)
                        .where(Entries.UUID + "=?", uri.getLastPathSegment())
                        .map(Entries.PROJ_ALL);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }


        Cursor c =
                builder
                        .where(selection, selectionArgs)
                        .query(db, false, projection, sortOrder, null);

        Context context = getContext();
        if (null != context) {
            c.setNotificationUri(context.getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final int match = sUriMatcher.match(uri);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

        switch (match) {
            case ENTRIES: {
                db.insertOrThrow(Tables.ENTRIES, null, values);
                notifyChange(uri);
                return Entries.buildEntryUri(values.getAsString(Entries.UUID));
            }
            default: {
                throw new UnsupportedOperationException("Unknown insert uri: " + uri);
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LogUtil.log(DataProvider.class.getSimpleName(), "delete(uri=" + uri + ")");

        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final SelectionBuilder builder = new SelectionBuilder();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ENTRIES: {
                builder.table(Tables.ENTRIES);
                break;
            }
            case ENTRIES_ID: {
                builder
                        .table(Tables.ENTRIES)
                        .where(Entries.UUID + "=?", uri.getLastPathSegment());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }

        int count = builder.where(selection, selectionArgs).delete(db);
        notifyChange(uri);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final SelectionBuilder builder = new SelectionBuilder();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ENTRIES: {
                builder.table(Tables.ENTRIES);
                break;
            }
            case ENTRIES_ID: {
                builder
                        .table(Tables.ENTRIES)
                        .where(Entries.UUID + "=?", uri.getLastPathSegment());
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }

        int count = builder.where(selection, selectionArgs).update(db, values);
        notifyChange(uri);
        return count;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    private void notifyChange(Uri uri) {
        // We only notify changes if the caller is not the sync adapter.
        // The sync adapter has the responsibility of notifying changes (it can do so
        // more intelligently than we can -- for example, doing it only once at the end
        // of the sync instead of issuing thousands of notifications for each record).
        if (!Contract.hasCallerIsSyncAdapterParameter(uri)) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);

            // Widgets can't register content observers so we refresh widgets separately.
//            context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
            // TODO: add a broadcast to notify of updates
        }
    }
}

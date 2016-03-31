package com.datonicgroup.narrate.app.dataprovider.providers;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.datonicgroup.narrate.app.dataprovider.providers.Contract.Entries;
import com.datonicgroup.narrate.app.dataprovider.sync.AbsSyncService;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.models.AbsSyncItem;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by timothymiko on 5/24/14.
 * <p/>
 * This class is used to interact with the database. It handles adding and retrieving data from
 * the database. The methods in this class should not be run on the main UI thread. They should
 * be run from a separate Thread or in an AsyncTask.
 */
public class EntryHelper {

    public static boolean mCallerIsSyncAdapter;

    public static Entry getEntry(String uuid) {

        Cursor cursor = GlobalApplication.getAppContext().getContentResolver()
                .query(Entries.CONTENT_URI,
                        Entries.PROJ_ALL_ARRAY,

                        Entries.UUID + "=?",
                        new String[]{uuid},

                        null);

        Entry entry = null;

        if ((cursor != null) && (cursor.getCount() > 0)) {
            cursor.moveToFirst();
            entry = fromCursor(cursor);

            if (entry != null)
                entry.photos = PhotosDao.getPhotosForEntry(entry);
        }

        cursor.close();

        return entry;
    }

    public static Entry getEntryWithGoogleDriveId(String driveId) {
        Cursor cursor = GlobalApplication.getAppContext().getContentResolver()
                .query(Entries.CONTENT_URI,
                        Entries.PROJ_ALL_ARRAY,

                        Entries.GOOGLE_DRIVE_FILE_ID + "=? OR " + Entries.GOOGLE_DRIVE_PHOTO_FILE_ID + "=?",
                        new String[]{driveId, driveId},

                        null);

        Entry entry = null;

        if ((cursor != null) && (cursor.getCount() > 0)) {
            cursor.moveToFirst();
            entry = fromCursor(cursor);

            if (entry != null)
                entry.photos = PhotosDao.getPhotosForEntry(entry);
        }

        cursor.close();

        return entry;
    }

    public static MutableArrayList<Entry> getAllEntries() {

        Uri uri = mCallerIsSyncAdapter ? Contract.addCallerIsSyncAdapterParameter(Entries.CONTENT_URI) :
                Entries.CONTENT_URI;

        Cursor cursor = GlobalApplication.getAppContext().getContentResolver()
                .query(uri,
                        Entries.PROJ_ALL_ARRAY,

                        // only retrieve the entries that are not deleted
                        Entries.DELETED + "= ?",
                        new String[]{String.valueOf(0)},

                        null);

        MutableArrayList<Entry> entries = new MutableArrayList<Entry>();

        while (cursor.moveToNext()) {
            Entry entry = fromCursor(cursor);
            entry.photos = PhotosDao.getPhotosForEntry(entry);
            entries.add(entry);
        }

        cursor.close();

        return entries;
    }

    public static ArrayList<AbsSyncItem> getEntriesToSync(SyncService service) {

        Uri uri = mCallerIsSyncAdapter ? Contract.addCallerIsSyncAdapterParameter(Entries.CONTENT_URI) :
                Entries.CONTENT_URI;

        Cursor cursor = GlobalApplication.getAppContext().getContentResolver()
                .query(uri,
                        Entries.PROJ_ALL_ARRAY,
                        null,
                        null,
                        null);

        ArrayList<AbsSyncItem> entries = new ArrayList<>();

        while (cursor.moveToNext()) {
            Entry entry = fromCursor(cursor);
            entry.photos = PhotosDao.getPhotosForEntry(entry);
            entries.add(entry);
        }

        cursor.close();

        Collections.sort(entries, new Comparator<AbsSyncItem>() {
            @Override
            public int compare(AbsSyncItem lhs, AbsSyncItem rhs) {
                return ((Entry) rhs).creationDate.compareTo(((Entry) lhs).creationDate);
            }
        });

        List<SyncInfo> syncInfo = SyncInfoDao.getDataForService(service);
        HashMap<String, SyncInfo> mapping = new HashMap<>();

        for (SyncInfo s : syncInfo)
            mapping.put(s.getTitle(), s);

        for (AbsSyncItem e : entries)
            e.syncInfo = mapping.get(e.uuid);

        return entries;

    }

    public static ArrayList<Entry> getDeletedEntries() {

        Uri uri = mCallerIsSyncAdapter ? Contract.addCallerIsSyncAdapterParameter(Entries.CONTENT_URI) :
                Entries.CONTENT_URI;

        Cursor cursor = GlobalApplication.getAppContext().getContentResolver()
                .query(uri,
                        Entries.PROJ_ALL_ARRAY,

                        // only retrieve the entries that are not deleted
                        Entries.DELETED + "=?",
                        new String[]{String.valueOf(1)},

                        null);

        ArrayList<Entry> entries = new ArrayList<Entry>();

        while (cursor.moveToNext()) {
            Entry entry = fromCursor(cursor);
            entry.photos = PhotosDao.getPhotosForEntry(entry);
            entries.add(entry);
        }

        cursor.close();

        return entries;
    }

    public static boolean saveEntry(Entry entry) {

        Uri uri = mCallerIsSyncAdapter ? Contract.addCallerIsSyncAdapterParameter(Entries.CONTENT_URI) :
                Entries.CONTENT_URI;

        ContentValues vals = new ContentValues();
        vals.put(Entries.UUID, entry.uuid);
        vals.put(Entries.CREATION_DATE, entry.creationDate.getTimeInMillis());
        vals.put(Entries.MODIFIED_DATE, entry.modifiedDate);
        vals.put(Entries.TITLE, entry.title);
        vals.put(Entries.TEXT, entry.text);
        vals.put(Entries.PLACE_NAME, entry.placeName);
        vals.put(Entries.LATITUDE, entry.latitude);
        vals.put(Entries.LONGITUDE, entry.longitude);
        vals.put(Entries.STARRED, entry.starred);

        String tags = new JSONArray(entry.tags).toString();
        vals.put(Entries.TAGS_LIST, tags);

        vals.put(Entries.DELETED, entry.isDeleted);
        vals.put(Entries.DELETION_DATE, entry.deletionDate);
        vals.put(Entries.GOOGLE_DRIVE_FILE_ID, entry.googleDriveFileId);
        vals.put(Entries.GOOGLE_DRIVE_PHOTO_FILE_ID, entry.googleDrivePhotoFileId);

        GlobalApplication.getAppContext().getContentResolver()
                .insert(uri,
                        vals);

        return true;
    }

    public static boolean markDeleted(Entry entry) {

        List<AbsSyncService> services = SyncHelper.getSyncServices();
        for (int i = 0; i < services.size(); i++ ) {
            SyncInfo info = new SyncInfo();
            info.setTitle(entry.uuid);
            info.setSyncService(services.get(i).getSyncService().ordinal());
            info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
            info.setSyncStatus(SyncStatus.DELETE);
            SyncInfoDao.saveData(info);
        }

        entry.isDeleted = true;
        entry.deletionDate = Calendar.getInstance(Locale.getDefault()).getTimeInMillis();
        return saveEntry(entry);
    }

    public static boolean deleteEntry(Entry entry) {

        Uri uri = mCallerIsSyncAdapter ? Contract.addCallerIsSyncAdapterParameter(Entries.CONTENT_URI) :
                Entries.CONTENT_URI;

        int result = GlobalApplication.getAppContext().getContentResolver()
                .delete(uri,
                        Entries.UUID + "=?",
                        new String[]{entry.uuid});

        return result > 0;
    }

    public static void deleteAllEntries() {

            GlobalApplication.getAppContext().getContentResolver()
                    .delete(Entries.CONTENT_URI,
                            null,
                            null);
    }

    /**
     * Data helper methods
     */

    public static Entry fromCursor(Cursor cursor) {

        Entry entry = new Entry();
        entry.id = cursor.getInt(0);
        entry.uuid = cursor.getString(1);

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(cursor.getLong(2));
        entry.creationDate = date;

        entry.modifiedDate = cursor.getLong(3);
        entry.title = cursor.getString(4);
        entry.text = cursor.getString(5);
        entry.placeName = cursor.getString(6);
        entry.latitude = cursor.getDouble(7);
        entry.longitude = cursor.getDouble(8);
        entry.hasLocation = entry.latitude != 0 && entry.longitude != 0;
        entry.starred = cursor.getInt(9) == 1;

        try {
            JSONArray json = new JSONArray(cursor.getString(10));
            ArrayList<String> tags = new ArrayList<>();
            for (int i = 0; i < json.length(); i++)
                tags.add(json.getString(i));

            entry.tags = tags;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        entry.isDeleted = cursor.getInt(11) == 1;
        entry.deletionDate = cursor.getLong(12);
        entry.googleDriveFileId = cursor.getString(13);
        entry.googleDrivePhotoFileId = cursor.getString(14);

        return entry;
    }

    public static String toJson(Entry entry) {

        try {
            JSONObject json = new JSONObject();
            json.put("uuid", entry.uuid);
            json.put("creationDate", entry.creationDate.getTimeInMillis());
            json.put("modifiedDate", entry.modifiedDate);
            json.put("title", entry.title);
            json.put("text", entry.text);
            json.put("hasLocation", entry.hasLocation);
            json.put("placeName", entry.placeName);
            json.put("latitude", entry.latitude);
            json.put("longitude", entry.longitude);
            json.put("starred", entry.starred);
            json.put("tags", new JSONArray(entry.tags));
            json.put("isDeleted", entry.isDeleted);
            json.put("deletionDate", entry.deletionDate);

            return json.toString();
        } catch (JSONException e) {
            LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
            return null;
        }
    }

    public static Entry fromJson(String json) {
        try {
            JSONObject in = new JSONObject(json);
            Entry entry = new Entry();
            entry.uuid = in.getString("uuid");

            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTimeInMillis(in.getLong("creationDate"));
            entry.creationDate = cal;

            entry.modifiedDate = in.getLong("modifiedDate");

            try {
                entry.title = in.getString("title");
            } catch (Exception e) {
                LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
                entry.title = "";
            }

            try {
                entry.text = in.getString("text");
            } catch (Exception e) {
                LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
                entry.text = "";
            }

            entry.hasLocation = in.getBoolean("hasLocation");

            try {
                entry.placeName = in.getString("placeName");
            } catch (Exception e) {
                LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
                entry.placeName = null;
            }

            entry.latitude = in.getDouble("latitude");
            entry.longitude = in.getDouble("longitude");

            entry.starred = in.getBoolean("starred");

            try {
                JSONArray tagsJson = in.getJSONArray("tags");
                ArrayList<String> tags = new ArrayList<>();
                for (int i = 0; i < tagsJson.length(); i++)
                    tags.add(tagsJson.getString(i));

                entry.tags = tags;
            } catch (Exception e) {
                LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
                entry.tags = new ArrayList<>();
            }

            entry.isDeleted = in.getBoolean("isDeleted");

            try {
                entry.deletionDate = in.getLong("deletionDate");
            } catch (Exception e) {
                LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
                entry.deletionDate = 0;
            }

            return entry;

        } catch (JSONException e) {
            LogUtil.e("EntryUtil", "Exception in fromJson: ", e);
            e.printStackTrace();
            return null;
        }
    }

    public static NSDictionary toDictionary(Entry entry) {
        NSDictionary dictionary = new NSDictionary();

        dictionary.put("UUID", entry.uuid);
        dictionary.put("Creation Date", entry.creationDate.getTime());
        dictionary.put("Modified Date", entry.modifiedDate);

        NSDictionary creatorDictionary = new NSDictionary();
        creatorDictionary.put("Device Agent", "Narrate");
        creatorDictionary.put("Generation Date", entry.creationDate.getTime());
        creatorDictionary.put("Host Name", "Narrate");
        creatorDictionary.put("OS Agent", "Android");
        creatorDictionary.put("Software Agent", "Narrate");
        dictionary.put("Creator", creatorDictionary);

        if (entry.title != null && entry.title.length() > 0)
            dictionary.put("Entry Text", entry.title + "\n" + entry.text);
        else
            dictionary.put("Entry Text", entry.text);

        if (entry.hasLocation) {
            NSDictionary locationDict = new NSDictionary();
            locationDict.put("Administrative Area", "");
            locationDict.put("Country", "");
            locationDict.put("Latitude", entry.latitude);
            locationDict.put("Locality", "");
            locationDict.put("Longitude", entry.longitude);

            if (entry.placeName != null)
                locationDict.put("Place Name", entry.placeName);

            dictionary.put("Location", locationDict);
        }

        dictionary.put("Starred", entry.starred);
        dictionary.put("Time Zone", entry.creationDate.getTimeZone().getDisplayName());
        dictionary.put("Tags", entry.tags);

        return dictionary;
    }

    public static Entry parse(File file) {
        LogUtil.log("EntryUtil", "parse(File)");

        try {

            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(file);
            LogUtil.log("EntryUtil", "NSDictionary: " + rootDict.toXMLPropertyList());
            return fromDictionary(rootDict);

        } catch (Exception e) {
            LogUtil.e("EntryUtil", "Exception in parse: ", e);
            return null;
        }
    }

    public static Entry fromDictionary(NSDictionary rootDict) {
        Entry entry = new Entry();

        entry.uuid = rootDict.objectForKey("UUID").toString();
        LogUtil.log("EntryUtil", "UUID: " + entry.uuid);

        entry.creationDate = Calendar.getInstance();
        entry.creationDate.setTime((Date) rootDict.objectForKey("Creation Date").toJavaObject());

        try {
            entry.modifiedDate = (long) rootDict.get("Modified Date").toJavaObject();
        } catch (Exception e) {
            entry.modifiedDate = entry.creationDate.getTimeInMillis();
        }

        NSObject textObject = rootDict.objectForKey("Entry Text");

        if (textObject != null) {
            String text = textObject.toString();

            if (text != null && text.length() > 0) {

                try {
                    String patternString = "(^.{1,100}\\n)";

                    final Pattern pattern = Pattern.compile(patternString);
                    final Matcher matcher = pattern.matcher(text);
                    matcher.find();

                    entry.title = matcher.group(1).replaceFirst("\\n$", "");
                    entry.text = text.replaceFirst(matcher.group(1), "").replaceFirst("^\\n", "");


                    LogUtil.log("EntryUtil", "Title: " + entry.title);
                    LogUtil.log("EntryUtil", "Text: " + entry.text);
                } catch (Exception e) {
                    entry.text = text;
                }
            } else {
                entry.text = text;
            }
        } else {
            entry.text = null;
        }

        try {
            entry.latitude = (double) ((NSDictionary) rootDict.get("Location")).get("Latitude").toJavaObject();
        } catch (Exception e) {
            entry.latitude = 0;
            LogUtil.log("EntryUtil", "Error getting latitude: ");
            LogUtil.log("EntryUtil", e.toString());
        }

        try {
            entry.longitude = (double) ((NSDictionary) rootDict.get("Location")).get("Longitude").toJavaObject();
        } catch (Exception e) {
            entry.longitude = 0;
            LogUtil.log("EntryUtil", "Error getting longitude: ");
            LogUtil.log("EntryUtil", e.toString());
        }

        try {
            entry.placeName = (String) ((NSDictionary) rootDict.get("Location")).get("Place Name").toJavaObject();
        } catch (Exception e) {
            entry.placeName = null;
            LogUtil.log("EntryUtil", "Error getting placeName: ");
            LogUtil.log("EntryUtil", e.toString());
        }

        try {
            entry.starred = (boolean) rootDict.get("Starred").toJavaObject();
        } catch (Exception e) {
            entry.starred = false;
            LogUtil.log("EntryUtil", "Error getting starred: ");
            LogUtil.log("EntryUtil", e.toString());
        }

        try {
            Object[] objects = (Object[]) rootDict.get("Tags").toJavaObject();
            String[] tags = new String[objects.length];
            int i = 0;
            for (Object obj : objects) {
                tags[i] = (String) obj;
                i++;
            }
            entry.tags = Arrays.asList(tags);
        } catch (Exception e) {
            entry.tags = new ArrayList<>();
            LogUtil.log("EntryUtil", "Error getting tags: ");
            LogUtil.log("EntryUtil", e.toString());
        }

        return entry;
    }

    public static String getUUIDFromString(String s) {
        StringBuilder uuid = new StringBuilder();
        String str = s;
        str = str.toLowerCase().replace(".doentry", "").replace(".narrate", "").replace("-", "").replace(".jpg", "").toUpperCase();
        str = str.replace("[^a-zA-Z0-9 -]", "");
        str = str.trim();

        for (char c : str.toCharArray()) {
            if (!Character.isLetterOrDigit(c))
                break;
            else
                uuid.append(Character.toUpperCase(c));
        }

        return uuid.toString();
    }

    public static String getTags(Entry entry) {

        if (entry.tags.size() > 0) {

            StringBuilder tagsBuilder = new StringBuilder();
            for (int i = 0; i < entry.tags.size(); i++) {
                tagsBuilder.append(entry.tags.get(i));

                if (i < entry.tags.size() - 1) {
                    tagsBuilder.append("\u0020");
                    tagsBuilder.append("\u2022");
                    tagsBuilder.append("\u0020");
                }
            }
            return tagsBuilder.toString();
        } else {
            return null;
        }
    }
}

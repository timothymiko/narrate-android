package com.datonicgroup.narrate.app.dataprovider.providers;

import android.media.MediaScannerConnection;
import android.os.Environment;

import com.datonicgroup.narrate.app.dataprovider.sync.SyncInfoManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncObject;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.FileUtil;
import com.datonicgroup.narrate.app.util.LogUtil;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by timothymiko on 10/15/14.
 */
public class PhotosDao {

    public static List<Photo> getPhotosForEntry(Entry entry) {

        if (entry == null) {
            return new ArrayList<>();
        }

        Photo photo = new Photo();
        photo.path = getPathForPhoto(entry.uuid);

        List<Photo> results = new ArrayList<>();

        File file = new File(photo.path);
        if ( file.exists() ) {
            photo.name = file.getName();
            photo.uuid = entry.uuid;
            results.add(photo);
        }

        return results;
    }

    public static boolean deletePhoto(Photo photo) {
        LogUtil.log(PhotosDao.class.getSimpleName(), "deletePhoto()");
        markPhotoDeleted(photo);
        return new File(photo.path).delete();
    }

    public static void deleteEverything() {
        LogUtil.log(PhotosDao.class.getSimpleName(), "deletePhoto()");

        File dir = getPhotosFolder();

        String[] children = dir.list();
        File file;

        for (int i = 0; i < children.length; i++) {
            file = new File(dir, children[i]);

            if (file.isDirectory()) {
                FileUtil.deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }

    public static File getFileForPhoto(String uuid) throws IOException {
        File photoFile = new File(getPathForPhoto(uuid));

        if ( !photoFile.exists() )
            photoFile.createNewFile();

        return photoFile;
    }

    public static List<Photo> getPhotosToSync(SyncService service) {
        File folder = getPhotosFolder();
        File[] children = folder.listFiles();

        List<Photo> photos = new ArrayList<>();

        List<SyncInfo> syncInfoList = SyncInfoDao.getDataForService(service);
        HashMap<String, SyncInfo> mapping = new HashMap<>();
        for ( SyncInfo s : syncInfoList )
            mapping.put(s.getTitle().toUpperCase(), s);

        for ( File file : children ) {
            if ( !file.isDirectory() && !file.getName().equals(".syncinfo")) {
                Photo photo = new Photo();
                photo.path = file.getAbsolutePath();
                photo.name = file.getName().toLowerCase();
                photo.uuid = EntryHelper.getUUIDFromString(photo.name);
                photo.syncInfo = mapping.get(file.getName().toUpperCase());
                photos.add(photo);
            }
        }

        List<String> deletedChildren = getDeletedPhotos();
        if ( deletedChildren.size() > 0 ) {
            for (String name : deletedChildren) {
                Photo photo = new Photo();
                photo.path = name;
                photo.name = name.toLowerCase();
                photo.uuid = EntryHelper.getUUIDFromString(photo.name);
                photo.syncInfo = mapping.get(name.toUpperCase());
                photo.isDeleted = true;
                photos.add(photo);
            }
        }

        return photos;
    }

    private static String getPathForPhoto(String uuid) {
        return getPhotosFolder().getAbsolutePath() + File.separator + uuid + ".jpg";
    }

    private static String getDeletedPath(String uuid) {
        return getPhotosFolder().getAbsolutePath() + File.separator + ".deleted" + File.separator + uuid + ".jpg";
    }

    public static File getPhotosFolder() {
        File folder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES + "/Narrate");

        if ( folder.mkdirs() ) {
            MediaScannerConnection.scanFile(GlobalApplication.getAppContext(),
                    new String[]{folder.getAbsolutePath()}, null, null);
        }

        return folder;
    }

    /** RELATED TO SYNCING **/

    /**
     * This file currently contains a JSON array of JSON objects with a single key,value:
     * "fileName" --> name of photo file that needs to be deleted
     */
    private static void appendToSyncFile(File file, String key, String data) throws IOException, JSONException {
        LogUtil.log(SyncInfoManager.class.getSimpleName(), "appendToSyncFile: " + data);

        JSONParser parser = new JSONParser();
        JSONArray array;
        JSONObject obj;
        try {
            obj = (JSONObject) parser.parse(new FileReader(file));
            array = (JSONArray) obj.get("DeletedPhotos");
        } catch (ParseException e) {
            e.printStackTrace();
            obj = new JSONObject();
            array = new JSONArray();
        }

        JSONObject newData = new JSONObject();
        newData.put(key, data);

        array.add(newData);
        obj.put("DeletedPhotos", array);

        LogUtil.log(SyncInfoManager.class.getSimpleName(), "JSONArray: " + array.toString());

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(obj.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private static void markPhotoDeleted(Photo photo) {
        LogUtil.log(SyncInfoManager.class.getSimpleName(), "markPhotoDeleted()");

        try {
            // avoid duplicate entries of a photo
            removeRecordsOfPhoto(photo);

            appendToSyncFile(getSyncFile(SyncObject.Type.Photo), "fileName", photo.name);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void removeRecordsOfPhoto(Photo photo) {
        LogUtil.log(SyncInfoManager.class.getSimpleName(), "removeRecordsOfPhoto()");

        try {
            File file = getSyncFile(SyncObject.Type.Photo);

            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(file));
            JSONArray array = (JSONArray) obj.get("DeletedPhotos");

            List<Object> newArray = new ArrayList<>();
            if (array != null) {
                String name = photo.name;
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = (JSONObject) array.get(i);
                    String fileName = (String) item.get("fileName");

                    if (fileName != null && !fileName.toLowerCase().equals(name.toLowerCase()))
                        newArray.add(array.get(i));

                }
            }

            array = new JSONArray();
            array.addAll(newArray);
            obj.put("DeletedPhotos", array);

            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                writer.write(obj.toString());
                LogUtil.log(SyncInfoManager.class.getSimpleName(), "JSONArray: " + obj.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getDeletedPhotos() {
        LogUtil.log(SyncInfoManager.class.getSimpleName(), "getDeletedPhotos()");

        try {
            File file = getSyncFile(SyncObject.Type.Photo);

            JSONParser parser = new JSONParser();
            JSONObject obj = (JSONObject) parser.parse(new FileReader(file));
            JSONArray array = (JSONArray) obj.get("DeletedPhotos");

            List<String> data = new ArrayList<>();

            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject item = (JSONObject) array.get(i);
                    String fileName = (String) item.get("fileName");

                    if (fileName != null)
                        data.add(fileName);
                }
            }

            return data;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    /**
     * Related to sync file that is maintained in local photos folder
     */

    private static File getSyncFile(SyncObject.Type type) {
        if (type == SyncObject.Type.Photo) {
            File folder = PhotosDao.getPhotosFolder();
            File file = new File(folder, ".syncinfo");

            if (!file.exists())
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            return file;
        }

        return null;
    }
}
package com.datonicgroup.narrate.app.dataprovider.sync;

import android.text.TextUtils;
import android.util.Log;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.api.ServiceGenerator;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.GoogleApiHeaders;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.GoogleDriveFileService;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFile;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileChange;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileChangeList;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileList;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileMetadata;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFileMetadataRequest;
import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveStartPageToken;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.models.DriveEntry;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.models.PendingSyncCall;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.google.gson.Gson;
import com.parse.ParseInstallation;
import com.parse.ParsePush;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by timothymiko on 1/13/16.
 */
public class GoogleDriveSyncService {

    private static GoogleDriveSyncService sInstance;

    public GoogleDriveSyncService() {
        this.mService = ServiceGenerator.createService(
                    GoogleDriveFileService.class,
                    new GoogleApiHeaders(),
                    GoogleDriveFileService.BASE_URL
                );
    }

    public static GoogleDriveSyncService shared() {
        if (sInstance == null)
            sInstance = new GoogleDriveSyncService();

        return sInstance;
    }

    public interface GoogleDriveSyncSetupInterface {
        void onSetupComplete();
        void onSetupFailure();
    }

    private GoogleDriveFileService mService;


    public void setup(final GoogleDriveSyncSetupInterface callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    String query = "mimeType='" + GoogleDriveFileService.FOLDER_MIME + "'";
                    Response<DriveFileList> response = mService.list(null, query).execute();

                    if (!response.isSuccess()) {
                        LogUtil.log("GoogleDriveSyncSetup", "Failed to retrieve list of folders.");
                        callback.onSetupFailure();
                        return;
                    }

                    boolean createEntriesFolder = false;
                    boolean createPhotosFolder = false;

                    boolean syncEntriesFolder = false;
                    boolean syncPhotosFolder = false;

                    DriveFileList list = response.body();
                    if (list.items.isEmpty()) {
                        createEntriesFolder = true;
                        createPhotosFolder = true;
                    } else {
                        for (int i = 0; i < list.items.size(); i++) {
                            DriveFileMetadata folder = list.items.get(i);
                            if (folder.title.equals("entries")) {
                                syncEntriesFolder = true;
                                Settings.setGoogleDriveSyncEntriesFolderId(folder.id);
                            } else if (folder.title.equals("photos")) {
                                syncPhotosFolder = true;
                                Settings.setGoogleDriveSyncPhotosFolderId(folder.id);
                            }
                        }
                    }

                    if (createEntriesFolder) {
                        DriveFileMetadataRequest request = new DriveFileMetadataRequest(
                                "entries",
                                "appDataFolder",
                                GoogleDriveFileService.FOLDER_MIME
                        );
                        Response<DriveFileMetadata> response2 = mService.createMetadata(request).execute();

                        if (!response2.isSuccess()) {
                            LogUtil.log("GoogleDriveSyncSetup", "Failed to create entries folders.");
                            callback.onSetupFailure();
                            return;
                        }

                        String id = response2.body().id;
                        if (TextUtils.isEmpty(id)) {
                            LogUtil.log("GoogleDriveSyncSetup", "Entries folder ID is null.");
                            callback.onSetupFailure();
                            return;
                        }

                        Settings.setGoogleDriveSyncEntriesFolderId(id);
                    }

                    if (createPhotosFolder) {
                        DriveFileMetadataRequest request = new DriveFileMetadataRequest(
                                "photos",
                                "appDataFolder",
                                GoogleDriveFileService.FOLDER_MIME
                        );
                        Response<DriveFileMetadata> response2 = mService.createMetadata(request).execute();

                        if (!response2.isSuccess()) {
                            LogUtil.log("GoogleDriveSyncSetup", "Failed to create photos folders.");
                            callback.onSetupFailure();
                            return;
                        }

                        String id = response2.body().id;
                        if (TextUtils.isEmpty(id)) {
                            LogUtil.log("GoogleDriveSyncSetup", "Photos folder ID is null.");
                            callback.onSetupFailure();
                            return;
                        }

                        Settings.setGoogleDriveSyncPhotosFolderId(id);
                    }

                    manualSync();

                    callback.onSetupComplete();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public Call<DriveFileMetadata> create(Entry e) {

        if (TextUtils.isEmpty(Settings.getGoogleDriveSyncEntriesFolderId())) {
            LogUtil.e("GoogleDriveSyncService", "Entries folder id is null");
        }

        DriveFileMetadataRequest metadata = new DriveFileMetadataRequest(
                e.uuid + ".json",
                Settings.getGoogleDriveSyncEntriesFolderId(),
                "application/json"
        );

        DriveEntry de = new DriveEntry(e);
        DriveFile f = new DriveFile(de);

        Gson gson = new Gson();
        String entry = gson.toJson(f);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), entry);

        return mService.create(metadata, body);
    }

    public Call<DriveFileMetadata> create(Photo p) throws FileNotFoundException {

        if (TextUtils.isEmpty(Settings.getGoogleDriveSyncPhotosFolderId())) {
            LogUtil.e("GoogleDriveSyncService", "Photos folder id is null");
        }

        DriveFileMetadataRequest metadata = new DriveFileMetadataRequest(
                p.name,
                Settings.getGoogleDriveSyncPhotosFolderId(),
                "image/jpg"
        );

        File f = new File(p.path);
        if (!f.exists()) {
            throw new FileNotFoundException();
        }

        RequestBody body = RequestBody.create(MediaType.parse("image/*"), f);

        return mService.create(metadata, body);
    }

    public Call<DriveFile> retrieve(String id) {

        return mService.retrieve(id);
    }

    public Call<DriveFileMetadata> update(Entry e) {

        if (e.googleDriveFileId == null)
            throw new NullPointerException("GoogleDriveSyncService#delete requires a Google Drive File ID.");

        DriveFile f = new DriveFile(new DriveEntry(e));

        Gson gson = new Gson();
        String entry = gson.toJson(f);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), entry);

        return mService.update(e.googleDriveFileId, body);

    }

    public Call<DriveFileMetadata> update(String id, Photo p) throws FileNotFoundException {

        if (TextUtils.isEmpty(Settings.getGoogleDriveSyncPhotosFolderId())) {
            LogUtil.e("GoogleDriveSyncService", "Photos folder id is null");
        }

        File f = new File(p.path);
        if (!f.exists()) {
            throw new FileNotFoundException();
        }

        RequestBody body = RequestBody.create(MediaType.parse("image/*"), f);

        return mService.update(id, body);
    }

    public Call<Void> delete(final Entry e) {

        if (e.googleDriveFileId == null)
            throw new NullPointerException("GoogleDriveSyncService#delete requires a Google Drive File ID.");

        return mService.delete(e.googleDriveFileId);

    }

    public Call<Void> delete(String id) {

        if (id == null)
            throw new NullPointerException("GoogleDriveSyncService#delete requires a Google Drive File ID.");

        return mService.delete(id);

    }

    public void manualSync() throws IOException {
        Settings.setGoogleDriveManualSyncPending(true);

        Response<DriveFileList> resp = mService.list(null, null).execute();
        if (!resp.isSuccess() || resp.body() == null) {
            Log.e("GoogleDriveSyncService", "Error retrieving entries in Google Drive.");
            return;
        }

        List<Entry> localEntries = EntryHelper.getAllEntries();
        List<DriveFileMetadata> remoteEntries = resp.body().items;


        HashMap<String, Entry> localEntriesMap = new HashMap<>();
        HashMap<String, Photo> localPhotosMap = new HashMap<>();
        HashMap<String, DriveFileMetadata> remoteEntriesMap = new HashMap<>();
        HashMap<String, DriveFileMetadata> remotePhotosMap = new HashMap<>();

        for (int i = 0; i < localEntries.size(); i++) {
            Entry e = localEntries.get(i);
            localEntriesMap.put(e.uuid, e);

            if (e.photos.size() > 0) {
                Photo p = e.photos.get(0);
                localPhotosMap.put(e.uuid, p);
            }
        }

        for (int i = 0; i < remoteEntries.size(); i++) {
            DriveFileMetadata file = remoteEntries.get(i);
            String uuid = file.title.contains(".") ? file.title.substring(0, file.title.indexOf(".")) : null;
            if (file.mimeType.startsWith("image")) {
                remotePhotosMap.put(uuid, file);
            } else if (file.mimeType.equals("application/json")){
                remoteEntriesMap.put(uuid, file);
            }
        }

        /**
         * Entries
         */
        Iterator<String> localIter = localEntriesMap.keySet().iterator();
        while (localIter.hasNext()) {
            String uuid = localIter.next();
            Entry localEntry = localEntriesMap.get(uuid);
            if (!remoteEntriesMap.containsKey(uuid)) {
                Response<DriveFileMetadata> response = create(localEntry).execute();
                if (response.isSuccess()) {
                    localEntry.googleDriveFileId = response.body().id;

                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(localEntry);
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error creating new entry.");
                }
            } else {
                DriveFileMetadata remoteInfo = remoteEntriesMap.get(uuid);

                if (localEntry.googleDriveFileId == null || !localEntry.googleDriveFileId.equals(remoteInfo.id)) {
                    localEntry.googleDriveFileId = remoteInfo.id;
                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(localEntry);
                }

                long localModifiedDate = localEntry.modifiedDate;
                if (localModifiedDate == 0) {
                    localModifiedDate = localEntry.creationDate.getTimeInMillis();
                }

                if (Math.abs(localModifiedDate - remoteInfo.modifiedDate.getTime()) > 60000) {
                    if (localModifiedDate < remoteInfo.modifiedDate.getTime()) {
                        Response<DriveFile> response = retrieve(remoteInfo.id).execute();
                        if (response.isSuccess()) {

                            Entry entry = response.body().entry.toEntry();
                            entry.googleDriveFileId = remoteInfo.id;
                            entry.googleDrivePhotoFileId = localEntry.googleDrivePhotoFileId;

                            EntryHelper.mCallerIsSyncAdapter = true;
                            EntryHelper.saveEntry(entry);

                            remoteEntriesMap.remove(uuid);

                        } else {
                            Log.e("GoogleDriveSyncService", "Error retrieving remote entry in Google Drive.");
                        }
                    } else {

                        Response<DriveFileMetadata> response = update(localEntry).execute();
                        if (!response.isSuccess()) {
                            Log.e("GoogleDriveSyncService", "Error updating remote entry in Google Drive.");
                        } else {
                            remoteEntriesMap.remove(uuid);
                        }

                    }
                }
            }
        }

        Iterator<String> remoteIter = remoteEntriesMap.keySet().iterator();
        while (remoteIter.hasNext()) {
            String uuid = remoteIter.next();
            DriveFileMetadata info = remoteEntriesMap.get(uuid);

            Response<DriveFile> response = mService.retrieve(info.id).execute();
            if (response.isSuccess()) {

                Entry entry = response.body().entry.toEntry();
                entry.googleDriveFileId = info.id;

                EntryHelper.mCallerIsSyncAdapter = true;
                EntryHelper.saveEntry(entry);

            } else {
                LogUtil.log("GoogleDriveSyncService", "Error retrieving file from Google Drive: " + info.id);
            }
        }

        /**
         * Photos
         */
        for (int i = 0; i < remoteEntries.size(); i++) {
            DriveFileMetadata file = remoteEntries.get(i);
            String uuid = file.title.contains(".") ? file.title.substring(0, file.title.indexOf(".")) : null;
            if (file.mimeType.equals("application/json")){
                remoteEntriesMap.put(uuid, file);
            }
        }

        Iterator<String> localPhotosIter = localPhotosMap.keySet().iterator();
        while (localPhotosIter.hasNext()) {
            String uuid = localPhotosIter.next();
            Entry e = localEntriesMap.get(uuid);
            Photo localPhoto = localPhotosMap.get(uuid);
            if (!remotePhotosMap.containsKey(uuid)) {
                Response<DriveFileMetadata> response = create(localPhoto).execute();
                if (response.isSuccess()) {
                    e.googleDrivePhotoFileId = response.body().id;

                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(e);
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error creating new photo.");
                }
            } else {
                DriveFileMetadata remoteInfo = remotePhotosMap.get(uuid);

                if (e.googleDrivePhotoFileId == null || !e.googleDrivePhotoFileId.equals(remoteInfo.id)) {
                    e.googleDrivePhotoFileId = remoteInfo.id;
                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(e);
                }

                if (Math.abs(localPhoto.modifiedDate - remoteInfo.modifiedDate.getTime()) > 10000) {
                    if (localPhoto.modifiedDate < remoteInfo.modifiedDate.getTime()) {
                        Response<ResponseBody> response = mService.retrieveFile(remoteInfo.id).execute();
                        if (response.isSuccess()) {

                            InputStream is = response.body().byteStream();
                            File f = PhotosDao.getFileForPhoto(uuid);
                            FileOutputStream os = new FileOutputStream(f);

                            int bufferSize = 1024;
                            byte[] buffer = new byte[bufferSize];
                            int len = 0;
                            while ((len = is.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                            os.close();

                            e.googleDrivePhotoFileId = remoteInfo.id;

                            EntryHelper.mCallerIsSyncAdapter = true;
                            EntryHelper.saveEntry(e);

                            remotePhotosMap.remove(uuid);

                        } else {
                            Log.e("GoogleDriveSyncService", "Error retrieving remote photo in Google Drive.");
                        }
                    } else {

                        Response<DriveFileMetadata> response = update(e.googleDrivePhotoFileId, localPhoto).execute();
                        if (!response.isSuccess()) {
                            Log.e("GoogleDriveSyncService", "Error updating remote photo in Google Drive.");
                        } else {
                            remotePhotosMap.remove(uuid);
                        }

                    }
                }
            }
        }

        Iterator<String> remotePhotosIter = remotePhotosMap.keySet().iterator();
        while (remotePhotosIter.hasNext()) {
            String uuid = remotePhotosIter.next();
            DriveFileMetadata info = remotePhotosMap.get(uuid);
            Entry e = EntryHelper.getEntry(uuid);

            if (e != null) {
                Response<ResponseBody> response = mService.retrieveFile(info.id).execute();
                if (response.isSuccess()) {

                    InputStream is = response.body().byteStream();
                    File f = PhotosDao.getFileForPhoto(uuid);
                    FileOutputStream os = new FileOutputStream(f);

                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                    os.close();

                    e.googleDrivePhotoFileId = info.id;

                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(e);

                } else {
                    Log.e("GoogleDriveSyncService", "Error retrieving remote photo in Google Drive.");
                }
            } else {
                Log.e("GoogleDriveSyncService", "An entry does not exist for remote photo: " + info.id);
            }
        }

        Settings.setGoogleDriveManualSyncPending(false);
    }

    public void sync() throws IOException {

        long start = System.currentTimeMillis();

        String changeToken = Settings.getGoogleDriveSyncPageToken();
        if (changeToken == null) {
            Response<DriveStartPageToken> resp = mService.retrieveCurrentChangesToken().execute();
            if (resp.isSuccess()) {
                changeToken = resp.body().token;
            } else {
                Log.d("GoogleDriveSyncService", "Error fetching change token");
            }
        }

        boolean success = true;
        while (changeToken != null && success) {

            Response<DriveFileChangeList> resp = mService.listChanges(changeToken).execute();
            if (!resp.isSuccess() || resp.body().items == null) {
                Log.d("GoogleDriveSyncService", "Error retrieving change list");
                return;
            }

            DriveFileChangeList changes = resp.body();
            List<DriveFileChange> changeList = changes.items;
            if (changeList.isEmpty()) {
                Log.d("GoogleDriveSyncService", "No remote changes.");
                if (changes.newStartPageToken != null) {
                    Settings.setGoogleDriveSyncPageToken(changes.newStartPageToken);
                }
                break;
            }

            for (DriveFileChange change : changeList) {
                if (change.removed || change.deleted) {

                    Entry e = EntryHelper.getEntryWithGoogleDriveId(change.fileId);

                    if (e != null) {
                        if (change.fileId.equals(e.googleDriveFileId)) {

                            e.googleDrivePhotoFileId = null;
                            e.googleDriveFileId = null;

                            EntryHelper.markDeleted(e);
                            PendingSyncCall.delete(SyncService.GoogleDrive, change.fileId);

                            success = true;

                        } else {

                            Photo p = null;
                            if (e.photos.size() > 0) {
                                p = e.photos.get(0);
                            }

                            if (p != null) {
                                PhotosDao.deletePhoto(p);
                                success = true;
                            } else {
                                LogUtil.log("GoogleDriveSyncService", "Error deleting photo. Could not find file.");
                                success = false;
                                break;
                            }

                            e.googleDrivePhotoFileId = null;

                            EntryHelper.mCallerIsSyncAdapter = true;
                            EntryHelper.saveEntry(e);
                        }
                    } else {
                        success = true;
                    }

                } else {

                    if (change.file != null) {
                        if (change.file.mimeType.equals("application/json")) {

                            Response<DriveFile> response = mService.retrieve(change.fileId).execute();
                            if (response.isSuccess()) {

                                Entry entry = response.body().entry.toEntry();
                                entry.googleDriveFileId = change.fileId;

                                EntryHelper.mCallerIsSyncAdapter = true;
                                EntryHelper.saveEntry(entry);

                                success = true;

                            } else {
                                LogUtil.log("GoogleDriveSyncService", "Error retrieving file from Google Drive: " + change.fileId);

                                success = false;
                                break;
                            }
                        } else if (change.file.mimeType.startsWith("image/")) {

                            Response<ResponseBody> response = mService.retrieveFile(change.fileId).execute();
                            if (response.isSuccess()) {

                                String uuid = change.file.title.substring(0, change.file.title.indexOf("."));
                                InputStream is = response.body().byteStream();
                                File f = PhotosDao.getFileForPhoto(uuid);
                                FileOutputStream os = new FileOutputStream(f);

                                int bufferSize = 1024;
                                byte[] buffer = new byte[bufferSize];
                                int len = 0;
                                while ((len = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, len);
                                }
                                os.close();

                                Entry e = EntryHelper.getEntry(uuid);
                                e.googleDrivePhotoFileId = change.fileId;

                                EntryHelper.mCallerIsSyncAdapter = true;
                                EntryHelper.saveEntry(e);

                                success = true;

                            } else {
                                LogUtil.log("GoogleDriveSyncService", "Error retrieving file from Google Drive: " + change.fileId);

                                success = false;
                                break;
                            }

                        } else {
                            Log.d("Narrate", "GoogleDriveSyncService unknown mimeType: " + change.file.mimeType);
                        }
                    }
                }
            }


            if (success) {
                if (changes.nextPageToken != null) {
                    changeToken = changes.nextPageToken;
                } else {
                    changeToken = null;
                }
            }
        }

        HashMap<String, MutableArrayList<PendingSyncCall>> localEntryChanges = PendingSyncCall.retrieve(SyncService.GoogleDrive, false);
        HashMap<String, MutableArrayList<PendingSyncCall>> localPhotoChanges = PendingSyncCall.retrieve(SyncService.GoogleDrive, true);

        boolean didMakeAtLeastOneLocalChangeOnRemote = false;

        Iterator<String> entryiesKeyIter = localEntryChanges.keySet().iterator();
        while (entryiesKeyIter.hasNext()) {
            String name = entryiesKeyIter.next();
            String uuid = name.substring(0, name.indexOf("."));
            MutableArrayList<PendingSyncCall> pendingSyncCalls = localEntryChanges.get(name);

            Entry e = EntryHelper.getEntry(uuid);

            boolean containsCreateOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.CREATE;
                }
            }) != null;

            boolean containsUpdateOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.UPDATE;
                }
            }) != null;

            boolean containsDeleteOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.DELETE;
                }
            }) != null;

            success = false;

            if (containsCreateOp && !containsDeleteOp) {

                Response<DriveFileMetadata> response = create(e).execute();
                if (response.isSuccess()) {
                    e.googleDriveFileId = response.body().id;

                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.saveEntry(e);

                    success = true;
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error creating new entry.");
                }

            } else if (containsDeleteOp && !containsCreateOp) {

                Response<Void> response = delete(e).execute();
                if (response.isSuccess()) {
                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.deleteEntry(e);
                    success = true;
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error deleting entry.");
                }
            } else if (containsUpdateOp && !e.isDeleted) {

                Response<DriveFileMetadata> response = update(e).execute();
                if (!response.isSuccess()) {
                    LogUtil.log("GoogleDriveSyncService", "Error updating entry.");
                } else {
                    success = true;
                }
            }

            if (success) {
                PendingSyncCall.deleteAll(SyncService.GoogleDrive, name);
                didMakeAtLeastOneLocalChangeOnRemote = true;
            }
        }

        Iterator<String> photosKeyIter = localPhotoChanges.keySet().iterator();
        while (photosKeyIter.hasNext()) {
            String name = photosKeyIter.next();
            String uuid = name.substring(0, name.indexOf("."));
            MutableArrayList<PendingSyncCall> pendingSyncCalls = localPhotoChanges.get(name);
            String driveFileId = null;
            for (PendingSyncCall call: pendingSyncCalls) {
                if (call.serviceFileId != null) {
                    driveFileId = call.serviceFileId;
                    break;
                }
            }

            Entry e = EntryHelper.getEntry(uuid);
            Photo p = null;
            List<Photo> photos = PhotosDao.getPhotosForEntry(e);
            for (Photo photo: photos) {
                if (photo.name.equals(name)) {
                    p = photo;
                    break;
                }
            }

            boolean containsCreateOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.CREATE;
                }
            }) != null;

            boolean containsUpdateOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.UPDATE;
                }
            }) != null;

            boolean containsDeleteOp = pendingSyncCalls.find(new Predicate<PendingSyncCall>() {
                @Override
                public boolean apply(PendingSyncCall pendingSyncCall) {
                    return pendingSyncCall.operation == PendingSyncCall.Operation.DELETE;
                }
            }) != null;

            success = false;

            if (containsCreateOp && !containsDeleteOp) {

                if (p != null) {

                    Response<DriveFileMetadata> response = create(p).execute();
                    if (response.isSuccess()) {
                        e.googleDrivePhotoFileId = response.body().id;

                        EntryHelper.mCallerIsSyncAdapter = true;
                        EntryHelper.saveEntry(e);

                        success = true;
                    } else {
                        LogUtil.log("GoogleDriveSyncService", "Error creating new photo.");
                    }

                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error creating photo. p == null");
                }

            } else if (containsDeleteOp && !containsCreateOp) {

                if (driveFileId != null) {
                    Response<Void> response = delete(driveFileId).execute();
                    if (response.isSuccess() || response.raw().code() == 404) {
                        if (e != null) {
                            e.googleDrivePhotoFileId = null;
                            EntryHelper.mCallerIsSyncAdapter = true;
                            EntryHelper.saveEntry(e);
                        }
                        success = true;
                    } else {
                        LogUtil.log("GoogleDriveSyncService", "Error deleting photo.");
                    }
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error deleting photo. driveFileId is null.");
                }

            } else if (containsUpdateOp && !e.isDeleted) {

                if (p != null && driveFileId != null) {
                    Response<DriveFileMetadata> response = update(driveFileId, p).execute();
                    if (!response.isSuccess()) {
                        LogUtil.log("GoogleDriveSyncService", "Error updating photo.");
                    }
                } else {
                    LogUtil.log("GoogleDriveSyncService", "Error updating photo. p == null or googleDrivePhotoFileId == null");
                }
            }

            if (success) {
                PendingSyncCall.deleteAll(SyncService.GoogleDrive, name);
                didMakeAtLeastOneLocalChangeOnRemote = true;
            }
        }

        Response<DriveStartPageToken> resp = mService.retrieveCurrentChangesToken().execute();
        if (resp.isSuccess()) {
            changeToken = resp.body().token;
            Settings.setGoogleDriveSyncPageToken(changeToken);

            if (didMakeAtLeastOneLocalChangeOnRemote) {

                String userId = Settings.getUserId();
                String deviceId = ParseInstallation.getCurrentInstallation().getInstallationId();
                if (userId != null && deviceId != null) {

                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("deviceId", deviceId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    ParsePush push = new ParsePush();
                    push.setChannel("usersync_" + userId);
                    push.setData(payload);

                    try {
                        push.send();
                    } catch (Exception e) {
                        Log.d("Narrate", "Error notifying devices of updated content using Parse push notification.");
                        e.printStackTrace();
                    }
                }

            }

        } else {
            Log.d("GoogleDriveSyncService", "Error fetching change token");
        }

        long duration = System.currentTimeMillis() - start;

        Log.d("GoogleDriveSyncService", "Finished processing changes in " + duration + " ms.");
    }
}

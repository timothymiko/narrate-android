package com.datonicgroup.narrate.app.dataprovider.sync;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.crashlytics.android.Crashlytics;
import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.providers.SyncInfoDao;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.RemoteDataInfo;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 10/27/14.
 */
public class DriveSyncService extends AbsSyncService {

    public static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    public static final boolean CLEAR_FOLDER_CONTENTS = false;

    private Drive service;
    private File mAppFolder;

    public DriveSyncService() throws UserRecoverableAuthException {
        super(SyncService.GoogleDrive);

        try {
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = new JacksonFactory();

            String token = GoogleAuthUtil.getToken(GlobalApplication.getAppContext(), Settings.getEmail(), "oauth2:" + DriveScopes.DRIVE_APPDATA);

            if (SettingsUtil.shouldRefreshGDriveToken()) {
                GoogleAuthUtil.clearToken(GlobalApplication.getAppContext(), token);
                token = GoogleAuthUtil.getToken(GlobalApplication.getAppContext(), Settings.getEmail(), "oauth2:" + DriveScopes.DRIVE_APPDATA);
                SettingsUtil.refreshGDriveToken();
            }

            if (BuildConfig.DEBUG)
                LogUtil.log(getClass().getSimpleName(), "Access Token: " + token);

            GoogleCredential credential = new GoogleCredential().setAccessToken(token);
            service = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("Narrate").build();

        } catch (UserRecoverableAuthException ue) {
            throw ue;
        } catch (Exception e) {
            LogUtil.log(getClass().getSimpleName(), "Exception in creation: " + e);
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
        }

        if (CLEAR_FOLDER_CONTENTS) {
            deleteEverything();
        }
    }

    @Override
    public void save(Entry entry) {
        LogUtil.log(getClass().getSimpleName(), "save()");

        // update sync status
        SyncInfo info = new SyncInfo();
        info.setTitle(entry.uuid);
        info.setSyncService(SyncService.GoogleDrive.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.UPLOAD);
        SyncInfoDao.saveData(info);

        try {
            File data = getEntry(entry.uuid);
            boolean previouslyExisted = data != null;
            LogUtil.log(getClass().getSimpleName(), "Previously Existed? " + previouslyExisted);
            if (!previouslyExisted) data = new File();
            data.setTitle(entry.uuid + ".narrate");
            data.setAppDataContents(true);

            String json = EntryHelper.toJson(entry);
            LogUtil.log(getClass().getSimpleName(), json);

            InputStream stream = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8")));
            InputStreamContent content = new InputStreamContent(data.getMimeType(), stream);

            File appFolder = getAppFolder();
            LogUtil.log("DriveSyncService", "App Folder ID: " + appFolder.getId());
            LogUtil.log("DriveSyncService", "App Folder Title: " + appFolder.getTitle());

            data.setParents(Arrays.asList(new ParentReference().setId(appFolder.getId())));

            File file = null;
            if (previouslyExisted)
                file = service.files().update(data.getId(), data, content).execute();
            else
                file = service.files().insert(data, content).execute();

            info.setModifiedDate(file.getModifiedDate().getValue());
            info.setSyncStatus(SyncStatus.OK);
            info.setRevision(String.valueOf(file.getVersion()));
            SyncInfoDao.saveData(info);

        } catch (Exception e) {
            LogUtil.log(getClass().getSimpleName(), "Exception in save() - " + e.getMessage());
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
        }
    }

    @Override
    public Entry download(String uuid) {
        LogUtil.log(getClass().getSimpleName(), "download()");

        File file = getEntry(uuid);

        if (file != null && file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                HttpResponse resp =
                        service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                                .execute();

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                resp.download(os);
                LogUtil.log(getClass().getSimpleName(), "Download: " + os.toString("UTF-8"));

                // process input stream
                Entry entry = EntryHelper.fromJson(os.toString("UTF-8"));

                // close stream to prevent a memory leak
                os.close();

                if (entry != null) {
                    LogUtil.log(getClass().getSimpleName(), "entry != null");
                    SyncInfo info = new SyncInfo();
                    info.setTitle(entry.uuid);
                    info.setSyncService(SyncService.GoogleDrive.ordinal());
                    info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
                    info.setSyncStatus(SyncStatus.OK);
                    info.setRevision(String.valueOf(file.getVersion()));
                    SyncInfoDao.saveData(info);
                } else
                    LogUtil.log(getClass().getSimpleName(), "entry == null");

                return entry;

            } catch (Exception e) {
                LogUtil.log(getClass().getSimpleName(), "Exception in download() - " + e.getMessage());
                if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            }
        } else {
            LogUtil.log(getClass().getSimpleName(), "file == null: " + (file == null));
            if (!BuildConfig.DEBUG) Crashlytics.log("file == null in DriveSyncService#download(Entry): " + (file == null));
        }

        return null;
    }

    @Override
    public void delete(Entry entry) {
        LogUtil.log(getClass().getSimpleName(), "delete()");

        SyncInfo info = new SyncInfo();
        info.setTitle(entry.uuid);
        info.setSyncService(SyncService.GoogleDrive.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.DELETE);
        SyncInfoDao.saveData(info);

        File file = getEntry(entry.uuid);

        if (file != null && file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                service.files().delete(file.getId()).execute();

                info.setSyncStatus(SyncStatus.OK);
                SyncInfoDao.saveData(info);

            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            }
        } else {
            LogUtil.log(getClass().getSimpleName(), "file == null: " + (file == null));
            if (!BuildConfig.DEBUG) Crashlytics.log("file == null in DriveSyncService#delete(Entry): " + (file == null));

            info.setSyncStatus(SyncStatus.OK);
            SyncInfoDao.saveData(info);
        }
    }

    @Override
    public boolean doesContain(Entry entry) {
        return getEntry(entry.uuid) != null;
    }

    @Override
    public void save(Photo photo) {
        LogUtil.log(getClass().getSimpleName(), "save(Photo)");

        LogUtil.log(DriveSyncService.class.getSimpleName(), "Photo Name: " + photo.name);

        // update sync status
        SyncInfo info = new SyncInfo(photo.name.toUpperCase());
        info.setSyncService(SyncService.GoogleDrive.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.UPLOAD);
        SyncInfoDao.saveData(info);

        try {
            File data = getPhoto(photo.name);
            boolean previouslyExisted = data != null;
            LogUtil.log(getClass().getSimpleName(), "Previously Existed? " + previouslyExisted);
            if (!previouslyExisted) data = new File();
            data.setTitle(photo.name);
            data.setAppDataContents(true);

            data.setParents(Arrays.asList(new ParentReference().setId(getPhotosFolder().getId())));

            java.io.File imageFile = new java.io.File(photo.path);
            FileContent imageContent = new FileContent(data.getMimeType(), imageFile);

            File file = null;
            if (previouslyExisted)
                file = service.files().update(data.getId(), data, imageContent).execute();
            else
                file = service.files().insert(data, imageContent).execute();

            info.setModifiedDate(file.getModifiedDate().getValue());
            info.setSyncStatus(SyncStatus.OK);
            info.setRevision(String.valueOf(file.getVersion()));
            SyncInfoDao.saveData(info);

        } catch (Exception e) {
            LogUtil.log(getClass().getSimpleName(), "Exception in save(Photo) - " + e.getMessage());
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
        }
    }

    @Override
    public Photo downloadPhoto(String name) {
        LogUtil.log(getClass().getSimpleName(), "download(Photo)");

        try {
            String uuid = EntryHelper.getUUIDFromString(name);
            final java.io.File localFile = PhotosDao.getFileForPhoto(uuid);
            LogUtil.log(getClass().getSimpleName(), "Downloading: " + localFile.getName());

            File file = getPhoto(localFile.getName());

            if (file != null && file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
                try {
                    HttpResponse resp =
                            service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl()))
                                    .execute();

                    // process input stream
                    Bitmap mImage = BitmapFactory.decodeStream(resp.getContent());

                    if (mImage != null) {
                        LogUtil.log(getClass().getSimpleName(), "mImage != null");


                        if (localFile.exists()) {
                            localFile.delete();
                            localFile.createNewFile();
                        }

                        FileOutputStream fos = new FileOutputStream(localFile);
                        mImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        mImage.recycle();

                        SyncInfo info = new SyncInfo(localFile.getName().toUpperCase());
                        info.setSyncService(SyncService.GoogleDrive.ordinal());
                        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
                        info.setRevision(String.valueOf(file.getVersion()));
                        info.setSyncStatus(SyncStatus.OK);
                        SyncInfoDao.saveData(info);

                        Photo photo = new Photo();
                        photo.name = localFile.getName();
                        photo.path = localFile.getAbsolutePath();
                        photo.uuid = uuid;
                        photo.syncInfo = info;

                        return photo;

                    } else {
                        LogUtil.log(getClass().getSimpleName(), "image == null");
                        if (!BuildConfig.DEBUG) Crashlytics.log("image == null in DriveSyncService#downloadPhoto(): ");
                    }

                } catch (Exception e) {
                    LogUtil.log(getClass().getSimpleName(), "Exception in download(Photo) - " + e.getMessage());
                    if (!BuildConfig.DEBUG) Crashlytics.logException(e);
                }
            } else {
                LogUtil.log(getClass().getSimpleName(), "file == null: " + (file == null));
                if (!BuildConfig.DEBUG) Crashlytics.log("file == null in DriveSyncService#download(Photo): " + (file == null));
            }
        } catch (Exception e) {
            LogUtil.log(getClass().getSimpleName(), "Exception in download(Photo) - " + e.getMessage());
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
        }

        return null;
    }

    @Override
    public boolean delete(Photo photo) {
        LogUtil.log(getClass().getSimpleName(), "delete(Photo)");

        SyncInfo info = new SyncInfo();
        info.setTitle(photo.name.toUpperCase());
        info.setSyncService(SyncService.GoogleDrive.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.DELETE);
        SyncInfoDao.saveData(info);

        File file = getPhoto(photo.name);

        if (file != null && file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
            try {
                service.files().delete(file.getId()).execute();

                info.setSyncStatus(SyncStatus.OK);
                SyncInfoDao.saveData(info);

                return true;

            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                if (!BuildConfig.DEBUG) Crashlytics.logException(e);
                return false;
            }
        } else {
            LogUtil.log(getClass().getSimpleName(), "file == null: " + (file == null));
            if (!BuildConfig.DEBUG) Crashlytics.log("file == null in DriveSyncService#delete(Photo): " + (file == null));

            info.setSyncStatus(SyncStatus.OK);
            SyncInfoDao.saveData(info);

            return true;
        }
    }

    @Override
    public boolean doesContain(Photo photo) {
        return getPhoto(photo.name) != null;
    }

    @Override
    public void deleteEverything() {
        try {
            for (File f : getContents())
                try {
                    service.files().delete(f.getId()).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        } catch (Exception e) {

        }
    }

    private File getEntry(String title) {
        return getFile(title + ".narrate");
    }

    private File getPhoto(String title) {
        try {
            List<File> photos = getPhotosContents();

            if (photos != null) {
                for (File f : photos) {
                    if (f.getTitle().equalsIgnoreCase(title))
                        return f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private File getFile(String title) {
        try {
            List<File> contents = getContents();

            if (contents != null) {
                for (File f : contents) {
                    if (f.getTitle().equalsIgnoreCase(title))
                        return f;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public  List<RemoteDataInfo> getRemoteEntries() throws SyncFailedException {
        LogUtil.log(getClass().getSimpleName(), "Files in Narrate Drive AppFolder:");
        List<RemoteDataInfo> dataObjects = new ArrayList<>();

        try {
            List<File> contents = getContents();
            if (contents != null) {

                Iterator<File> iter = contents.iterator();
                File f;
                while (iter.hasNext()) {
                    f = iter.next();
                    LogUtil.log(getClass().getSimpleName(), f.getTitle());
                    if (!f.getTitle().equals("photos")) {
                        RemoteDataInfo info = new RemoteDataInfo();
                        info.name = f.getTitle();
                        info.isDirectory = f.getMimeType().equals(FOLDER_MIME);
                        info.isDeleted = f.getLabels().getTrashed();
                        info.modifiedDate = f.getModifiedDate().getValue();
                        info.revision = String.valueOf(f.getVersion());
                        dataObjects.add(info);
                    }
                }

                return dataObjects;
            }
        } catch (Exception e) {
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            throw new SyncFailedException(e.getMessage());
        }

        return null;
    }

    @Override
    public List<RemoteDataInfo> getRemotePhotos() throws SyncFailedException {
        LogUtil.log(DriveSyncService.class.getSimpleName(), "getRemotePhotos()");

        List<RemoteDataInfo> dataObjects = new ArrayList<>();
        try {
            List<File> result = getPhotosContents();

            LogUtil.log(getClass().getSimpleName(), "Files in Narrate Drive Photos Folder:");

            if (result.size() > 0) {
                for (File f : result) {
                    LogUtil.log(getClass().getSimpleName(), f.getTitle());
                    RemoteDataInfo info = new RemoteDataInfo();
                    info.name = f.getTitle();
                    info.isDirectory = f.getMimeType().equals(FOLDER_MIME);
                    info.isDeleted = f.getLabels().getTrashed();
                    info.modifiedDate = f.getModifiedDate().getValue();
                    info.revision = String.valueOf(f.getVersion());
                    dataObjects.add(info);
                }
            }
        } catch (Exception e) {
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            throw new SyncFailedException(e.getMessage());
        }

        return dataObjects;
    }

    private List<File> getPhotosContents() throws IOException {
        File folder = getPhotosFolder();

        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();
        request.setQ("'" + folder.getId() + "' in parents");

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());

            } catch (IOException e) {
                LogUtil.e(getClass().getSimpleName()
                        , "An error occurred: ", e);
                if (!BuildConfig.DEBUG) Crashlytics.logException(e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;

    }

    private List<File> getContents() throws IOException {

        List<File> result = new ArrayList<File>();
        Drive.Files.List request = service.files().list();
        request.setQ("'appfolder' in parents");

        do {
            try {
                FileList files = request.execute();

                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());

            } catch (IOException e) {
                LogUtil.e(getClass().getSimpleName()
                        , "An error occurred: ", e);
                if (!BuildConfig.DEBUG) Crashlytics.logException(e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null &&
                request.getPageToken().length() > 0);

        return result;
    }

    private File getPhotosFolder() {
        try {

            File folder = getFile("photos");

            if (folder == null) {

                File photos = new File();
                photos.setTitle("photos");
                photos.setAppDataContents(true);
                photos.setMimeType(FOLDER_MIME);
                photos.setParents(Arrays.asList(new ParentReference().setId(getAppFolder().getId())));

                photos = service.files().insert(photos).execute();

                return photos;

            } else {
                return folder;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
        }

        return null;
    }

    private File getAppFolder() throws IOException {
        if (mAppFolder == null)
            mAppFolder = service.files().get("appfolder").execute();

        return mAppFolder;
    }
}

package com.datonicgroup.narrate.app.dataprovider.sync;

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
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.RESTUtility;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 10/27/14.
 */
public class DropboxSyncService extends AbsSyncService {

    public final static String APP_KEY = BuildConfig.DROPBOX_API_KEY;
    public final static String APP_SECRET = BuildConfig.DROPBOX_API_SECRET;
    public final static Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    private DropboxAPI<AndroidAuthSession> mDBApi;

    final String ENTRIES = "/entries";
    final String DELETED_ENTRIES = "/entries/deleted";
    final String PHOTOS = "/photos";
    final String DELETED_PHOTOS = "/photos/deleted";
    final String APP_DIRECTORY = "/apps/Narrate";
    final String DAY_ONE_DIRECTORY = "/apps/Day One/Journal.dayone";

    public DropboxSyncService() {
        super(SyncService.Dropbox);
        initialize(null);
    }

    public DropboxSyncService(String token) {
        super(SyncService.Dropbox);
        initialize(token);
    }

    private void initialize(String token) {
        LogUtil.log(getClass().getSimpleName(), "initialize()");

        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);

        mDBApi = new DropboxAPI<>(session);

        if ( token == null )
            token = Settings.getDropboxSyncToken();

        if ( token != null )
            mDBApi.getSession().setOAuth2AccessToken(token);

        if (!doesFolderExist(getBaseFilePath())) {
            try {
                createFileStructure();
            } catch (DropboxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void save(Entry entry) {
        LogUtil.log(getClass().getSimpleName(), "save()");

        // update sync status
        SyncInfo info = new SyncInfo();
        info.setTitle(entry.uuid);
        info.setSyncService(SyncService.Dropbox.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.UPLOAD);
        SyncInfoDao.saveData(info);

        // save to user's dropbox
        String path = getPathForEntry(entry);
        File tmpFile = new File(GlobalApplication.getAppContext().getFilesDir() + File.separator + entry.uuid);

        DropboxAPI.Entry response = null;
        try {
            if (!tmpFile.exists())
                tmpFile.createNewFile();

            NSDictionary entryData = EntryHelper.toDictionary(entry);
            PropertyListParser.saveAsXML(entryData, tmpFile);

            // overwrite the entry if it already exists
            if (doesContain(entry))
                response = overwriteFile(path, tmpFile);
            else
                response = uploadFile(path, tmpFile);

            info.setModifiedDate(RESTUtility.parseDate(response.modified).getTime());
            info.setSyncStatus(SyncStatus.OK);
            info.setRevision(response.rev);
            SyncInfoDao.saveData(info);

        } catch (Exception e) {
            LogUtil.e(getClass().getSimpleName(), "Error Saving Entry: ", e);
        } finally {
            tmpFile.delete();
        }
    }

    @Override
    public void save(Photo photo) {
        LogUtil.log(getClass().getSimpleName(), "save(Photo)");

        File file = new File(photo.path);

        // update sync status
        SyncInfo info = new SyncInfo(photo.name.toUpperCase());
        info.setSyncService(SyncService.Dropbox.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.UPLOAD);
        SyncInfoDao.saveData(info);

        String path = getPathForPhoto(file.getName());

        DropboxAPI.Entry response = null;
        try {

            if (doesContain(photo))
                response = overwriteFile(path, file);
            else
                response = uploadFile(path, file);

            info.setModifiedDate(RESTUtility.parseDate(response.modified).getTime());
            info.setSyncStatus(SyncStatus.OK);
            info.setRevision(response.rev);
            SyncInfoDao.saveData(info);

        } catch (Exception e) {
            LogUtil.e(getClass().getSimpleName(), "Error Saving Photo: ", e);
        }
    }

    @Override
    public Entry download(String uuid) {
        LogUtil.log(getClass().getSimpleName(), "download()");

        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(uuid, null, GlobalApplication.getAppContext().getFilesDir());

            try {
                String path = getPathForEntry(uuid);
                LogUtil.log("DropboxSyncService", "Download path: " + path);
                downloadFile(path, tmpFile);
            } catch (DropboxException e) {
                LogUtil.e("DropboxSyncService", e);
                e.printStackTrace();
            }

            Entry entry = EntryHelper.parse(tmpFile);
            tmpFile.delete();

            if (entry != null) {
                SyncInfo info = new SyncInfo(entry.uuid);
                info.setSyncService(SyncService.Dropbox.ordinal());
                info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
                info.setRevision(getFileInfo(getPathForEntry(uuid)).rev);
                info.setSyncStatus(SyncStatus.OK);
                SyncInfoDao.saveData(info);
            }

            return entry;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DropboxException e) {
            e.printStackTrace();
        } finally {
            if (tmpFile != null)
                tmpFile.delete();
        }

        return null;
    }

    @Override
    public Photo downloadPhoto(String name) {
        LogUtil.log(getClass().getSimpleName(), "downloadPhoto()");

        try {
            String uuid = EntryHelper.getUUIDFromString(name);
            File file = PhotosDao.getFileForPhoto(uuid);
            String path = getPathForPhoto(file.getName());

            try {
                downloadFile(path, file);
            } catch (DropboxException e) {
                e.printStackTrace();
            }

            SyncInfo info = new SyncInfo(file.getName().toUpperCase());
            info.setSyncService(SyncService.Dropbox.ordinal());
            info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
            info.setRevision(getFileInfo(path).rev);
            info.setSyncStatus(SyncStatus.OK);
            SyncInfoDao.saveData(info);

            Photo photo = new Photo();
            photo.name = file.getName();
            photo.path = file.getPath();
            photo.uuid = uuid;
            photo.syncInfo = info;

            return photo;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (DropboxException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void delete(Entry entry) {
        LogUtil.log(getClass().getSimpleName(), "delete()");

        // update sync status
        SyncInfo info = new SyncInfo(entry.uuid);
        info.setSyncService(SyncService.Dropbox.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.DELETE);
        SyncInfoDao.saveData(info);

        // determine the path to the entry, if it exists
        StringBuilder from = new StringBuilder();
        StringBuilder to = new StringBuilder();

        boolean dayOneEntry;

        from.append(getBaseFilePath());
        to.append(from.toString());

        from.append(ENTRIES);
        from.append(File.separator);
        from.append(entry.uuid.toUpperCase());

        to.append(DELETED_ENTRIES);
        to.append(File.separator);
        to.append(entry.uuid.toUpperCase());

        dayOneEntry = !(doesFileExist(from.toString()) == 1);

        if (dayOneEntry) {
            from.append(".doentry");
            to.append(".doentry");
        }

        // attempt to delete from Dropbox
        try {
            DropboxAPI.Entry response = moveFile(from.toString(), to.toString());

            info.setModifiedDate(RESTUtility.parseDate(response.modified).getTime());
            info.setSyncStatus(SyncStatus.OK);
            SyncInfoDao.saveData(info);

        } catch (DropboxException e) {
            e.printStackTrace();
            if (e.toString().contains("404")) {
                info.setSyncStatus(SyncStatus.OK);
                SyncInfoDao.saveData(info);
            } else {
                info.setSyncStatus(SyncStatus.DELETE);
                SyncInfoDao.saveData(info);
            }
        }

    }

    @Override
    public boolean delete(Photo photo) {
        LogUtil.log(getClass().getSimpleName(), "delete(Photo)");

        // update sync status
        SyncInfo info = new SyncInfo(photo.name.toUpperCase());
        info.setSyncService(SyncService.Dropbox.ordinal());
        info.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        info.setSyncStatus(SyncStatus.DELETE);
        SyncInfoDao.saveData(info);

        String from = getPathForPhoto(photo.name);

        StringBuilder to = new StringBuilder();
        to.append(getBaseFilePath());
        to.append(DELETED_PHOTOS);
        to.append(File.separator);
        to.append(photo.name);


        // attempt to delete from Dropbox
        try {
            DropboxAPI.Entry response = moveFile(from, to.toString());

            info.setModifiedDate(RESTUtility.parseDate(response.modified).getTime());
            info.setSyncStatus(SyncStatus.OK);
            SyncInfoDao.saveData(info);

            return true;

        } catch (DropboxException e) {
            e.printStackTrace();
            if (e.toString().contains("404")) {

                // this may get thrown if the entry's photo has been deleted/removed/changed before
                // and there exists a file in the /Narrate/photos/deleted/ with the same name as
                // the one we are trying to save.
                try {
                    mDBApi.delete(to.toString());
                    moveFile(from, to.toString());
                } catch (DropboxException e1) {
                    e1.printStackTrace();
                }
                info.setSyncStatus(SyncStatus.OK);
                SyncInfoDao.saveData(info);
                return true;

            } else {
                info.setSyncStatus(SyncStatus.DELETE);
                SyncInfoDao.saveData(info);
            }
        }

        return false;
    }

    @Override
    public boolean doesContain(Entry entry) {
        StringBuilder path = new StringBuilder();

        path.append(getBaseFilePath());
        path.append(ENTRIES);
        path.append(File.separator);
        path.append(entry.uuid.toUpperCase());

        boolean exists = doesFileExist(path.toString()) == 1;

        if (exists)
            return true;
        else {
            path.append(".doentry");
            return doesFileExist(path.toString()) == 1;
        }
    }

    @Override
    public boolean doesContain(Photo photo) {

        StringBuilder path = new StringBuilder();
        path.append(getBaseFilePath());
        path.append(PHOTOS);
        path.append(File.separator);

        String base_path = path.toString();

        return doesFileExist(base_path + photo.name) == 1 ||
                doesFileExist(base_path + photo.name.toLowerCase()) == 1 ||
                doesFileExist(base_path + photo.name.toUpperCase()) == 1;
    }

    @Override
    public void deleteEverything() {
        try {
            mDBApi.delete(getBaseFilePath() + ENTRIES);
        } catch (DropboxException e) {
            e.printStackTrace();
        }
        try {
            mDBApi.delete(getBaseFilePath() + PHOTOS);
        } catch (DropboxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<RemoteDataInfo> getRemoteEntries() throws SyncFailedException {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseFilePath());
        sb.append(ENTRIES);

        List<RemoteDataInfo> dataInfoObjects = new ArrayList<>();

        try {
            List<DropboxAPI.Entry> dropboxEntries = getFileInfo(sb.toString()).contents;

            for (DropboxAPI.Entry entry : dropboxEntries) {
                if ( !entry.isDir ) {
                    RemoteDataInfo infoObject = new RemoteDataInfo();
                    infoObject.isDirectory = entry.isDir;
                    infoObject.isDeleted = entry.isDeleted;
                    infoObject.name = entry.fileName().toUpperCase();
                    infoObject.modifiedDate = RESTUtility.parseDate(entry.modified).getTime();
                    infoObject.revision = entry.rev;
                    dataInfoObjects.add(infoObject);
                }
            }

        } catch (Exception e) {
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            throw new SyncFailedException(e.getMessage());
        }

        return dataInfoObjects;
    }

    @Override
    public List<RemoteDataInfo> getRemotePhotos() throws SyncFailedException {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseFilePath());
        sb.append(PHOTOS);

        List<RemoteDataInfo> dataInfoObjects = new ArrayList<>();

        try {
            List<DropboxAPI.Entry> dropboxEntries = getFileInfo(sb.toString()).contents;

            for (DropboxAPI.Entry file : dropboxEntries) {
                if ( !file.isDir ) {
                    RemoteDataInfo infoObject = new RemoteDataInfo();
                    infoObject.isDirectory = file.isDir;
                    infoObject.isDeleted = file.isDeleted;
                    infoObject.name = file.fileName().toLowerCase();
                    infoObject.modifiedDate = RESTUtility.parseDate(file.modified).getTime();
                    infoObject.revision = file.rev;
                    dataInfoObjects.add(infoObject);
                }
            }

        } catch (Exception e) {
            if (!BuildConfig.DEBUG) Crashlytics.logException(e);
            e.printStackTrace();
            throw new SyncFailedException(e.getMessage());
        }

        return dataInfoObjects;
    }

    public void downloadFile(String dbPath, File localFile) throws DropboxException, IOException {

        BufferedInputStream br = null;
        BufferedOutputStream bw = null;

        if (!localFile.exists()) {
            localFile.createNewFile(); //otherwise dropbox client will fail silently
        }

        checkNotNull();
        DropboxAPI.DropboxInputStream fd = mDBApi.getFileStream(dbPath, null);
        br = new BufferedInputStream(fd);
        bw = new BufferedOutputStream(new FileOutputStream(localFile));


        byte[] buffer = new byte[4096];
        int read;
        while (true) {
            read = br.read(buffer);
            if (read <= 0) {
                break;
            }
            bw.write(buffer, 0, read);
        }

        br.close();
        bw.close();
    }

    private String getBaseFilePath() {

        String path = Settings.getDropboxSyncFolder();

        if (path == null || path.equals("")) {
            if (Settings.getDropboxSyncDayOne()) {
                path = DAY_ONE_DIRECTORY;
            } else {
                path = APP_DIRECTORY;
            }
        }

        return path;
    }

    private boolean createFolder(String path) throws DropboxException {
        checkNotNull();
        mDBApi.createFolder(path);
        return true;
    }

    private boolean doesFolderExist(String path) {
        try {
            DropboxAPI.Entry metadata = getFileInfo(path);
            return metadata.isDir;
        } catch (DropboxException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns 1 if it does exist
     * Returns 0 if it does not exist
     * Returns -1 if there is any other error
     *
     * @param path
     * @return
     */
    private int doesFileExist(String path) {
        try {
            DropboxAPI.Entry metadata = getFileInfo(path);
            return 1;
        } catch (DropboxException e) {
            e.printStackTrace();
            if (e.toString().contains("404"))
                return 0;
            else
                return -1;
        }
    }

    private void createFileStructure() throws DropboxException {

        /**
         * Creates the following folders:
         *  - /basePath
         *  - /basePath/entries
         *  - /basePath/photos
         *  - /basePath/entries/deleted
         *  - /basePath/photos/deleted
         */

        List<String> folders = new ArrayList<>();
        String basepath = getBaseFilePath();

        folders.add(basepath);
        folders.add(basepath + ENTRIES);
        folders.add(basepath + DELETED_ENTRIES);
        folders.add(basepath + PHOTOS);
        folders.add(basepath + DELETED_PHOTOS);

        for (String folder : folders)
            createFolder(folder);
    }

    private String getPathForEntry(String filename) {
        StringBuilder path = new StringBuilder();
        path.append(getBaseFilePath());
        path.append(ENTRIES);
        path.append(File.separator);
        path.append(filename);

        if (Settings.getDropboxSyncDayOne() || Settings.getDropboxSyncFolder().toLowerCase().contains("journal.dayone"))
            path.append(".doentry");

        return path.toString();
    }

    private String getPathForPhoto(String title) {
        StringBuilder path = new StringBuilder();
        path.append(getBaseFilePath());
        path.append(PHOTOS);
        path.append(File.separator);
        path.append(title);
        return path.toString();
    }

    private String getPathForEntry(Entry entry) {
        return getPathForEntry(entry.uuid);
    }

    private DropboxAPI.Entry getFileInfo(String path) throws DropboxException {
        checkNotNull();
        return mDBApi.metadata(path, 5000, null, true, null);
    }

    public DropboxAPI.Entry uploadFile(String dbPath, File localFile) throws FileNotFoundException, DropboxException {
        checkNotNull();
        FileInputStream inputStream = new FileInputStream(localFile);
        return mDBApi.putFile(dbPath, inputStream,
                localFile.length(), null, null);
    }

    public DropboxAPI.Entry overwriteFile(String dbPath, File localFile) throws DropboxException, FileNotFoundException {
        checkNotNull();
        FileInputStream inputStream = new FileInputStream(localFile);
        return mDBApi.putFileOverwrite(dbPath, inputStream,
                localFile.length(), null);
    }

    private DropboxAPI.Entry moveFile(String from, String to) throws DropboxException {
        checkNotNull();
        return mDBApi.move(from, to);
    }

    private void checkNotNull() {
        if (mDBApi == null)
            initialize(null);
    }
}

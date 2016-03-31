package com.datonicgroup.narrate.app.dataprovider;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.dataprovider.providers.TagsDao;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncInfoManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.PendingSyncCall;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.File;
import java.util.Date;

/**
 * Created by timothymiko on 6/11/14.
 * <p/>
 * This class is used as the interface for working with all files. It handles the local file
 * system as well as the remote file system on Dropbox. It will keep the two in sync if the user
 * has chosen to integrate with Dropbox.
 */
public class DataManager {

    private Context mContext;

    private static DataManager sInstance;

    public static DataManager getInstance() {
        if ( sInstance == null )
            sInstance = new DataManager();

        return sInstance;
    }

    public DataManager() {
        this.mContext = GlobalApplication.getAppContext();
    }

    public void save(Entry entry, boolean newEntry) {
        LogUtil.log(DataManager.class.getSimpleName(), "save(Entry)");

        // Stop syncing while we update the local data
        SyncHelper.cancelPendingActiveSync(User.getAccount());

        SyncInfoManager.setStatus(entry, SyncStatus.UPLOAD);

        if (Settings.getGoogleDriveSyncEnabled()) {
            PendingSyncCall call = new PendingSyncCall();
            call.service = SyncService.GoogleDrive;
            call.time = new Date();
            call.name = entry.uuid + ".json";

            if (newEntry) {
                call.operation = PendingSyncCall.Operation.CREATE;
            } else {
                call.operation = PendingSyncCall.Operation.UPDATE;
                call.serviceFileId = entry.googleDriveFileId;
            }

            boolean result = PendingSyncCall.save(call);
            if (!result) {
                LogUtil.log("DataManager", "Error saving pending sync call.");
            }
        }

        EntryHelper.mCallerIsSyncAdapter = false;
        EntryHelper.saveEntry(entry);

        if ( entry.hasLocation && entry.placeName != null )
            PlacesDao.storePlace(entry.placeName, entry.latitude, entry.longitude);

        if ( entry.tags != null && entry.tags.size() > 0 ) {
            for ( String t : entry.tags )
                TagsDao.storeTag(t);
        }
    }

    public void save(Photo photo, String googleDriveFileId) {
        LogUtil.log(DataManager.class.getSimpleName(), "save(Photo)");

        // Stop syncing while we update the local data
        SyncHelper.cancelPendingActiveSync(User.getAccount());

        SyncInfoManager.setStatus(photo, SyncStatus.UPLOAD);

        // Clear Glide cache for this photo to prevent old photos from showing
        File[] cacheDir = Glide.getPhotoCacheDir(GlobalApplication.getAppContext()).listFiles();
        if ( cacheDir != null ) {
            for (int i = 0; i < cacheDir.length; i++) {
                if ( cacheDir[i].getName().equals(photo.name) ) {
                    cacheDir[i].delete();
                    break;
                }
            }
        }

        if (Settings.getGoogleDriveSyncEnabled()) {
            PendingSyncCall call = new PendingSyncCall();
            call.service = SyncService.GoogleDrive;
            call.time = new Date();
            call.name = photo.name;

            if (googleDriveFileId == null) {
                call.operation = PendingSyncCall.Operation.CREATE;
            } else {
                call.operation = PendingSyncCall.Operation.UPDATE;
                call.serviceFileId = googleDriveFileId;
            }

            boolean result = PendingSyncCall.save(call);
            if (!result) {
                LogUtil.log("DataManager", "Error saving pending sync call.");
            }
        }

        // Photos aren't currently managed by the content provider, so we have to trigger
        // a sync manually in order for them to get updated on the server. Any time, an
        // entry is editing the entries table is changed, a sync is triggered. This doesn't
        // automatically happen with photos, so we must initiate it on our own.
        SyncHelper.requestManualSync(User.getAccount());
    }

    public void delete(Entry entry) {
        LogUtil.log(DataManager.class.getSimpleName(), "delete(Entry)");

        SyncInfoManager.setStatus(entry, SyncStatus.DELETE);

        EntryHelper.markDeleted(entry);

        if (Settings.getGoogleDriveSyncEnabled()) {
            PendingSyncCall call = new PendingSyncCall();
            call.service = SyncService.GoogleDrive;
            call.time = new Date();
            call.name = entry.uuid + ".json";
            call.serviceFileId = entry.googleDriveFileId;
            call.operation = PendingSyncCall.Operation.DELETE;

            boolean result = PendingSyncCall.save(call);
            if (!result) {
                LogUtil.log("DataManager", "Error saving pending sync call.");
            }
        }

        for (Photo p : entry.photos) {
            SyncInfoManager.setStatus(p, SyncStatus.DELETE);
            PhotosDao.deletePhoto(p);

            if (Settings.getGoogleDriveSyncEnabled()) {
                PendingSyncCall call = new PendingSyncCall();
                call.service = SyncService.GoogleDrive;
                call.time = new Date();
                call.name = p.name;
                call.serviceFileId = entry.googleDrivePhotoFileId;

                if (entry.googleDrivePhotoFileId != null) {
                    call.operation = PendingSyncCall.Operation.DELETE;
                }

                boolean result = PendingSyncCall.save(call);
                if (!result) {
                    LogUtil.log("DataManager", "Error saving pending sync call.");
                }
            }
        }
    }

    public void delete(Photo photo, String googleDriveFileId) {
        LogUtil.log(DataManager.class.getSimpleName(), "delete(Photo)");

        SyncInfoManager.setStatus(photo, SyncStatus.DELETE);
        PhotosDao.deletePhoto(photo);

        if (Settings.getGoogleDriveSyncEnabled()) {
            PendingSyncCall call = new PendingSyncCall();
            call.service = SyncService.GoogleDrive;
            call.time = new Date();
            call.name = photo.name;
            call.operation = PendingSyncCall.Operation.DELETE;
            call.serviceFileId = googleDriveFileId;

            boolean result = PendingSyncCall.save(call);
            if (!result) {
                LogUtil.log("DataManager", "Error saving pending sync call.");
            }
        }

        // Photos aren't currently managed by the content provider, so we have to trigger
        // a sync manually in order for them to get updated on the server. Any time, an
        // entry is editing the entries table is changed, a sync is triggered. This doesn't
        // automatically happen with photos, so we must initiate it on our own.
        SyncHelper.requestManualSync(User.getAccount());
    }

    public void sync() {
        LogUtil.log(DataManager.class.getSimpleName(), "sync()");
        SyncHelper.requestManualSync(User.getAccount());
    }
}

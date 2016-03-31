package com.datonicgroup.narrate.app.dataprovider.sync;

import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.dataprovider.providers.TagsDao;
import com.datonicgroup.narrate.app.models.AbsSyncItem;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.RemoteDataInfo;
import com.datonicgroup.narrate.app.models.SyncObject;
import com.datonicgroup.narrate.app.models.SyncService;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.SyncFailedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by timothymiko on 10/27/14.
 */
public abstract class AbsSyncService {

    private SyncService mSyncService;

    AbsSyncService(SyncService mSyncService) {
        this.mSyncService = mSyncService;
    }

    public abstract void save(Entry entry);

    public abstract Entry download(String uuid);

    public abstract void delete(Entry entry);

    public abstract boolean doesContain(Entry entry);

    public abstract void save(Photo photo);

    public abstract Photo downloadPhoto(String name);

    public abstract boolean delete(Photo photo);

    public abstract boolean doesContain(Photo photo);

    public abstract void deleteEverything();

    /**
     * Queries remote sync service for information about the data the service stores (i.e.
     * a list of file names, modified date, etc.)
     *
     * @return list of info objects
     */
    public abstract List<RemoteDataInfo> getRemoteEntries() throws SyncFailedException;

    public abstract List<RemoteDataInfo> getRemotePhotos() throws SyncFailedException;

    public SyncService getSyncService() {
        return mSyncService;
    }

    /**
     * Algorithm for syncing files is as follows:
     * <p/>
     * 1. Retrieve local list of entries
     * 2. Retrieve remote list of entries
     * 3. Create a HashSet<String> containing all of the remote entry UUIDs and a HashMap that maps the UUIDs to the remote service object (i.e. DropboxAPI.Entry)
     * 4. Create a HashSet<String> of all the local entry UUIDs
     * 5. Iterate through local entries:
     * a. Add entries marked for save to SyncObject array with 'Upload' operation
     * b. Add entries marked for deletion to SyncObject array with 'Delete' operation
     * c. Add entries that are out of date with the server to SyncObject array with 'Download' operation
     * d. Add entries that were deleted remotely to the SyncObject array wtih 'Delete' operation
     * 6. Iterate through remote entries:
     * a. Add entries that do not exist locally to the SyncObject array with 'Download' operation
     * 7. Return the size of the SyncObject array
     */

    public int sync() {
        int ops = 0;
        ops += syncEntries();
        ops += syncPhotos();
        return ops;
    }

    /**
     * This function should be used when a user changes his or her sync settings (i.e. switches folder)
     * to transfer and update data.
     */
    public void resyncFiles() {
        LogUtil.log(getClass().getSimpleName(), "resyncFiles()");

        resyncEntries();
        resyncPhotos();
    }

    private int syncEntries() {
        LogUtil.log(AbsSyncService.class.getSimpleName(), "Beginning sync for entries.");

        List<AbsSyncItem> localEntries = EntryHelper.getEntriesToSync(mSyncService);
        List<RemoteDataInfo> remoteEntries = null;
        try {
            remoteEntries = getRemoteEntries();
        } catch (SyncFailedException e) {
            LogUtil.e("", "Failed to get remote entries. Stopping entries sync.");
            return 0;
        }

        // let's do some error checking first
        if (remoteEntries == null) {
            LogUtil.e("", "Remote entries == null. Stopping entries sync.");
            return 0;
        }

        if (localEntries == null) {
            LogUtil.e("", "Local entries == null. Stopping entries sync.");
            return 0;
        }

        if (remoteEntries.isEmpty() && localEntries.isEmpty()) {
            LogUtil.log("", "Both remote entries and local entries are empty. Stopping entries sync");
            return 0;
        }

        HashSet<String> remoteUUIDs = new HashSet<>();
        HashSet<String> localUUIDs = new HashSet<>();

        HashMap<String, RemoteDataInfo> remoteEntriesHash = new HashMap<>();

        HashSet<SyncObject> syncObjects = new HashSet<>();

        // fill remoteUUIDs and remoteEntriesHash with data
        String uuid;
        Iterator<RemoteDataInfo> iter = remoteEntries.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo obj = iter.next();

            if (!obj.name.toLowerCase().contains("conflicted") && !obj.isDeleted && !obj.isDirectory) {
                uuid = EntryHelper.getUUIDFromString(obj.name);

                if (!remoteUUIDs.contains(uuid))
                    remoteUUIDs.add(uuid);

                if (!remoteEntriesHash.containsKey(uuid))
                    remoteEntriesHash.put(uuid, obj);
            }
        }

        // fill localUUIDs with data
        Iterator<AbsSyncItem> lIter = localEntries.iterator();
        while (lIter.hasNext()) {

            AbsSyncItem item = lIter.next();

            if (!localUUIDs.contains(item.uuid))
                localUUIDs.add(EntryHelper.getUUIDFromString(item.uuid));
        }

        SyncObject syncObject;
        RemoteDataInfo remoteInfo;

        // iterate through local items
        lIter = localEntries.iterator();
        while (lIter.hasNext()) {

            AbsSyncItem item = lIter.next();

            boolean deletedLocallyAndRemotely = item.syncInfo != null ? item.isDeleted && item.syncInfo.getSyncStatus() == SyncStatus.OK : false;

            if (item != null && !deletedLocallyAndRemotely) {

                uuid = EntryHelper.getUUIDFromString(item.uuid);
                remoteInfo = remoteEntriesHash.get(uuid);

                // items marked for save
                boolean upload = item.syncInfo == null ||
                        item.syncInfo.getSyncStatus() == SyncStatus.UPLOAD;
                if (upload) {
                    syncObject = new SyncObject(item.uuid, null, item, SyncObject.Operation.Upload);
                    LogUtil.log(getClass().getSimpleName(), "Upload " + item.uuid);
                    syncObjects.add(syncObject);
                    continue;
                }

                // items marked for deletion
                boolean delete = item.syncInfo.getSyncStatus() == SyncStatus.DELETE ||
                        (item.isDeleted && remoteUUIDs.contains(uuid));
                if (delete) {
                    syncObject = new SyncObject(item.uuid, remoteInfo, item, SyncObject.Operation.Delete);
                    LogUtil.log(getClass().getSimpleName(), "Delete " + item.uuid);
                    syncObjects.add(syncObject);
                    continue;
                }

                // items that are out of date with remote server
                if (remoteInfo != null && item.syncInfo.getRevision() != null) {

                    boolean doRevKeysMatch = item.syncInfo.getRevision().equals(remoteInfo.revision);

                    if (!doRevKeysMatch && !remoteInfo.isDeleted) {
                        syncObject = new SyncObject(item.uuid, remoteInfo, item, SyncObject.Operation.Download);
                        LogUtil.log(getClass().getSimpleName(), "Download " + item.uuid);
                        syncObjects.add(syncObject);
                        continue;
                    }
                }

                // items that have been deleted on the remote server
                if (!remoteUUIDs.contains(uuid)) {
                    if (item.syncInfo.getSyncStatus() == SyncStatus.OK && !item.isDeleted) {
                        syncObject = new SyncObject(item.uuid, remoteInfo, item, SyncObject.Operation.Delete);
                        LogUtil.log(getClass().getSimpleName(), "Delete2 " + item.uuid);
                        syncObjects.add(syncObject);
                        continue;
                    }
                }
            }
        }

        // download items that exist on remote client but not locally
        iter = remoteEntries.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.isDirectory && !info.isDeleted && !info.name.toLowerCase().contains("conflicted")) {

                uuid = EntryHelper.getUUIDFromString(info.name);

                if (!localUUIDs.contains(uuid)) {
                    SyncObject object = new SyncObject(uuid, info, null, SyncObject.Operation.Download);

                    if (!syncObjects.contains(object)) {
                        LogUtil.log(getClass().getSimpleName(), "Download2 " + uuid);
                        syncObjects.add(object);
                    }
                }
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), syncObjects.size() + " Entries to sync.");

        // process sync objects
        Iterator<SyncObject> syncIter = syncObjects.iterator();
        while (syncIter.hasNext()) {
            SyncObject so = syncIter.next();
            switch (so.operation) {
                case Upload:
                    save((Entry) so.item);
                    break;
                case Download:
                    Entry entry = download(so.uuid);
                    if (entry != null) {
                        EntryHelper.mCallerIsSyncAdapter = true;
                        EntryHelper.saveEntry(entry);

                        if ( entry.hasLocation && entry.placeName != null )
                            PlacesDao.storePlace(entry.placeName, entry.latitude, entry.longitude);

                        if ( entry.tags != null && entry.tags.size() > 0 ) {
                            for ( String t : entry.tags )
                                TagsDao.storeTag(t);
                        }
                    }
                    break;
                case Delete:
                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.markDeleted((Entry) so.item);
                    delete((Entry) so.item);
                    break;
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Entries sync complete.");

        return syncObjects.size();
    }

    private void resyncEntries() {

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Resyncing entries.");

        List<AbsSyncItem> localEntries = EntryHelper.getEntriesToSync(mSyncService);
        List<RemoteDataInfo> remoteEntries = null;
        try {
            remoteEntries = getRemoteEntries();
        } catch (SyncFailedException e) {
            LogUtil.e("", "Failed to get remote entries. Stopping entries re-sync.");
            return;
        }

        if (remoteEntries == null) {
            LogUtil.e("", "Remote entries == null. Stopping entries resync.");
            return;
        }

        if (localEntries == null) {
            LogUtil.e("", "Local entries == null. Stopping entries resync.");
            return;
        }

        if (remoteEntries.isEmpty() && localEntries.isEmpty()) {
            LogUtil.log("", "Both remote entries and local entries are empty. Stopping entries resync");
            return;
        }

        HashSet<SyncObject> syncObjects = new HashSet<>();

        HashSet<String> remoteUUIDs = new HashSet<>();
        HashSet<String> localUUIDs = new HashSet<>();

        String uuid;
        Iterator<RemoteDataInfo> iter = remoteEntries.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.name.toLowerCase().contains("conflicted") && !info.isDeleted && !info.isDirectory) {
                uuid = EntryHelper.getUUIDFromString(info.name);

                if (!remoteUUIDs.contains(uuid))
                    remoteUUIDs.add(uuid);
            }
        }

        Iterator<AbsSyncItem> lIter = localEntries.iterator();
        while (lIter.hasNext()) {

            AbsSyncItem e = lIter.next();

            if (!localUUIDs.contains(e.uuid))
                localUUIDs.add(EntryHelper.getUUIDFromString(e.uuid));
        }

        // check for local items that don't exist on the server
        lIter = localEntries.iterator();
        while (lIter.hasNext()) {

            AbsSyncItem e = lIter.next();

            uuid = EntryHelper.getUUIDFromString(e.uuid);

            if (!remoteUUIDs.contains(uuid)) {
                SyncObject object = new SyncObject(e.uuid, null, e, SyncObject.Operation.Upload);
                if (!syncObjects.contains(object)) {
                    LogUtil.log(getClass().getSimpleName(), "Uploading " + uuid);
                    syncObjects.add(object);
                }
            }
        }

        // check for remote items that don't exist locally
        iter = remoteEntries.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.isDirectory && !info.isDeleted && !info.name.toLowerCase().contains("conflicted")) {

                uuid = EntryHelper.getUUIDFromString(info.name);

                if (!localUUIDs.contains(uuid)) {
                    SyncObject object = new SyncObject(uuid, info, null, SyncObject.Operation.Download);
                    if (!syncObjects.contains(object)) {
                        LogUtil.log(getClass().getSimpleName(), "Downloading " + uuid);
                        syncObjects.add(object);
                    }
                }
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), syncObjects.size() + " Entries to process.");

        Iterator<SyncObject> syncIter = syncObjects.iterator();
        while (syncIter.hasNext()) {
            SyncObject so = syncIter.next();
            switch (so.operation) {
                case Upload:
                    save((Entry) so.item);
                    break;
                case Download:
                    Entry entry = download(so.uuid);
                    if (entry != null) {
                        EntryHelper.mCallerIsSyncAdapter = true;
                        EntryHelper.saveEntry(entry);

                        if ( entry.hasLocation && entry.placeName != null )
                            PlacesDao.storePlace(entry.placeName, entry.latitude, entry.longitude);

                        if ( entry.tags != null && entry.tags.size() > 0 ) {
                            for ( String t : entry.tags )
                                TagsDao.storeTag(t);
                        }
                    }
                    break;
                case Delete:
                    EntryHelper.mCallerIsSyncAdapter = true;
                    EntryHelper.markDeleted((Entry) so.item);
                    delete((Entry) so.item);
                    break;
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Entries resync complete.");
    }

    private int syncPhotos() {
        LogUtil.log(AbsSyncService.class.getSimpleName(), "Beginning sync for photos.");

        List<Photo> localPhotos = PhotosDao.getPhotosToSync(mSyncService);
        List<RemoteDataInfo> remotePhotos = null;
        try {
            remotePhotos = getRemotePhotos();
        } catch (SyncFailedException e) {
            LogUtil.e("", "Failed to get remote photos. Stopping photos sync.");
            return 0;
        }

        if (remotePhotos == null) {
            LogUtil.e("", "Remote photos == null. Stopping photos sync.");
            return 0;
        }

        if (localPhotos == null) {
            LogUtil.e("", "Local photos == null. Stopping photos sync.");
            return 0;
        }

        if (remotePhotos.isEmpty() && localPhotos.isEmpty()) {
            LogUtil.log("", "Both remote photos and local photos are empty. Stopping photos sync");
            return 0;
        }

        HashSet<SyncObject> syncObjects = new HashSet<>();

        HashSet<String> remoteItems = new HashSet<>();
        HashSet<String> localitems = new HashSet<>();

        HashMap<String, RemoteDataInfo> remoteEntriesHash = new HashMap<>();

        Iterator<RemoteDataInfo> iter = remotePhotos.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo obj = iter.next();

            if (!obj.isDeleted && !obj.isDirectory) {

                if (!remoteItems.contains(obj.name))
                    remoteItems.add(obj.name);

                if (!remoteEntriesHash.containsKey(obj.name))
                    remoteEntriesHash.put(obj.name, obj);
            }
        }

        // fill localUUIDs with data
        Iterator<Photo> pIter = localPhotos.iterator();
        while (pIter.hasNext()) {

            Photo photo = pIter.next();

            if (!localitems.contains(photo.name))
                localitems.add(photo.name);
        }

        SyncObject syncObject;
        RemoteDataInfo remoteInfo;

        // iterate through local items
        pIter = localPhotos.iterator();
        while (pIter.hasNext()) {

            Photo photo = pIter.next();

            boolean deletedLocally = photo.syncInfo != null ? photo.isDeleted : false;
            boolean deletedRemotely = !remoteItems.contains(photo.name);

            if (photo != null && !(deletedLocally && deletedRemotely)) {

                remoteInfo = remoteEntriesHash.get(photo.name);

                // items marked for save
                boolean upload = photo.syncInfo == null ||
                        photo.syncInfo.getSyncStatus() == SyncStatus.UPLOAD;
                if (upload) {
                    syncObject = new SyncObject(photo.name, null, photo, SyncObject.Operation.Upload);
                    LogUtil.log(getClass().getSimpleName(), "Upload " + photo.name);
                    syncObjects.add(syncObject);
                    continue;
                }

                // items marked for deletion
                boolean delete = photo.syncInfo.getSyncStatus() == SyncStatus.DELETE ||
                        (photo.isDeleted && remoteItems.contains(photo.name));
                if (delete) {
                    syncObject = new SyncObject(photo.name, remoteInfo, photo, SyncObject.Operation.Delete);
                    LogUtil.log(getClass().getSimpleName(), "Delete " + photo.name);
                    syncObjects.add(syncObject);
                    continue;
                }

                // items that are out of date with remote server
                if (remoteInfo != null && photo.syncInfo.getRevision() != null) {

                    boolean doRevKeysMatch = photo.syncInfo.getRevision().equals(remoteInfo.revision);

                    if (!doRevKeysMatch && !remoteInfo.isDeleted) {
                        syncObject = new SyncObject(photo.name, remoteInfo, photo, SyncObject.Operation.Download);
                        LogUtil.log(getClass().getSimpleName(), "Download " + photo.name);
                        syncObjects.add(syncObject);
                        continue;
                    }
                }

                // items that have been deleted on the remote server
                if (!remoteItems.contains(photo.name)) {
                    if (photo.syncInfo.getSyncStatus() == SyncStatus.OK && !photo.isDeleted) {
                        syncObject = new SyncObject(photo.name, remoteInfo, photo, SyncObject.Operation.Delete);
                        LogUtil.log(getClass().getSimpleName(), "Delete2 " + photo.name);
                        syncObjects.add(syncObject);
                        continue;
                    }
                }
            }
        }

        // download items that exist on remote client but not locally
        iter = remotePhotos.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.isDirectory && !info.isDeleted) {

                if (!localitems.contains(info.name)) {
                    SyncObject object = new SyncObject(info.name, info, null, SyncObject.Operation.Download);

                    if (!syncObjects.contains(object)) {
                        LogUtil.log(getClass().getSimpleName(), "Download2 " + info.name);
                        syncObjects.add(object);
                    }
                }
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), syncObjects.size() + " Photos to sync.");

        // process sync items
        Iterator<SyncObject> syncIter = syncObjects.iterator();
        while (syncIter.hasNext()) {
            SyncObject so = syncIter.next();

            switch (so.operation) {
                case Upload:
                    save((Photo) so.item);
                    break;
                case Download:
                    downloadPhoto(so.uuid);
                    break;
                case Delete:
                    PhotosDao.deletePhoto((Photo) so.item);
                    if (delete((Photo) so.item))
                        PhotosDao.removeRecordsOfPhoto((Photo) so.item);
                    break;
            }

        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Photos sync complete.");

        return syncObjects.size();
    }

    private void resyncPhotos() {

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Beginning photos resync.");

        List<Photo> localPhotos = PhotosDao.getPhotosToSync(mSyncService);
        List<RemoteDataInfo> remotePhotos = null;
        try {
            remotePhotos = getRemotePhotos();
        } catch (SyncFailedException e) {
            LogUtil.e("", "Failed to get remote photos. Stopping photos resync.");
            return;
        }

        if (remotePhotos == null) {
            LogUtil.e("", "Remote photos == null. Stopping photos resync.");
            return;
        }

        if (localPhotos == null) {
            LogUtil.e("", "Local photos == null. Stopping photos resync.");
            return;
        }

        if (remotePhotos.isEmpty() && localPhotos.isEmpty()) {
            LogUtil.log("", "Both remote photos and local photos are empty. Stopping photos resync");
            return;
        }

        HashSet<String> remoteItems = new HashSet<>();
        HashSet<String> localItems = new HashSet<>();

        HashSet<SyncObject> syncObjects = new HashSet<>();

        Iterator<RemoteDataInfo> iter = remotePhotos.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.isDeleted && !info.isDirectory) {

                if (!remoteItems.contains(info.name))
                    remoteItems.add(info.name);
            }
        }

        Iterator<Photo> pIter = localPhotos.iterator();
        while (pIter.hasNext()) {
            Photo p = pIter.next();

            if (!localItems.contains(p.name))
                localItems.add(p.name);
        }

        // check for local items that don't exist on the server
        pIter = localPhotos.iterator();
        while (pIter.hasNext()) {
            Photo p = pIter.next();

            if (!remoteItems.contains(p.name)) {
                SyncObject object = new SyncObject(p.name, null, p, SyncObject.Operation.Upload);
                if (!syncObjects.contains(object)) {
                    LogUtil.log(getClass().getSimpleName(), "Uploading " + p.name);
                    syncObjects.add(object);
                }
            }
        }

        // check for remote items that don't exist locally
        iter = remotePhotos.iterator();
        while (iter.hasNext()) {

            RemoteDataInfo info = iter.next();

            if (!info.isDirectory && !info.isDeleted && !info.name.toLowerCase().contains("conflicted")) {

                if (!localItems.contains(info.name)) {
                    SyncObject object = new SyncObject(info.name, info, null, SyncObject.Operation.Download);
                    if (!syncObjects.contains(object)) {
                        LogUtil.log(getClass().getSimpleName(), "Downloading " + info.name);
                        syncObjects.add(object);
                    }
                }
            }
        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), syncObjects.size() + " Photos to resync.");

        // process sync items
        Iterator<SyncObject> syncIter = syncObjects.iterator();
        while (syncIter.hasNext()) {
            SyncObject so = syncIter.next();
            switch (so.operation) {
                case Upload:
                    save((Photo) so.item);
                    break;
                case Download:
                    downloadPhoto(so.uuid);
                    break;
                case Delete:
                    PhotosDao.deletePhoto((Photo) so.item);
                    if (delete((Photo) so.item))
                        PhotosDao.removeRecordsOfPhoto((Photo) so.item);
                    break;
            }

        }

        LogUtil.log(AbsSyncService.class.getSimpleName(), "Photos resync complete.");
    }
}

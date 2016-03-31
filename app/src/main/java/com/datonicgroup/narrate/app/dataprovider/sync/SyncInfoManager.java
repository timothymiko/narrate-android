package com.datonicgroup.narrate.app.dataprovider.sync;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.SyncInfoDao;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.Photo;
import com.datonicgroup.narrate.app.models.SyncInfo;
import com.datonicgroup.narrate.app.models.SyncService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timothymiko on 7/23/14.
 */
public class SyncInfoManager {

    private SyncInfoManager() {
    }

    /**
     * @see com.datonicgroup.narrate.app.models.SyncStatus
     */
    public static void setStatus(Entry entry, int syncStatus) {
        List<SyncService> services = getSyncServices();

        for ( int i = 0; i < services.size(); i++ ) {
            SyncInfo info = SyncInfoDao.getInfo(entry.uuid, services.get(i));
            if ( info == null ) {
                info = new SyncInfo(entry.uuid);
                info.setSyncService(services.get(i).ordinal());
            }
            info.setSyncStatus(syncStatus);
            SyncInfoDao.saveData(info);
        }
    }

    /**
     * @see com.datonicgroup.narrate.app.models.SyncStatus
     * @see com.datonicgroup.narrate.app.models.SyncService
     */
    public static void setStatus(Entry entry, int syncStatus, SyncService service) {
        SyncInfo info = SyncInfoDao.getInfo(entry.uuid, service);
        if ( info == null ) {
            info = new SyncInfo(entry.uuid);
            info.setSyncService(service.ordinal());
        }
        info.setSyncStatus(syncStatus);
        SyncInfoDao.saveData(info);
    }

    /**
     * @see com.datonicgroup.narrate.app.models.SyncStatus
     */
    public static void setStatus(Photo photo, int syncStatus) {
        List<SyncService> services = getSyncServices();

        for ( int i = 0; i < services.size(); i++ ) {
            SyncInfo info = SyncInfoDao.getInfo(photo.name.toUpperCase(), services.get(i));
            if ( info == null ) {
                info = new SyncInfo(photo.name.toUpperCase());
                info.setSyncService(services.get(i).ordinal());
            }
            info.setSyncStatus(syncStatus);
            SyncInfoDao.saveData(info);
        }
    }

    /**
     * @see com.datonicgroup.narrate.app.models.SyncStatus
     * @see com.datonicgroup.narrate.app.models.SyncService
     */
    public static void setStatus(Photo photo, int syncStatus, SyncService service) {
        SyncInfo info = SyncInfoDao.getInfo(photo.name.toUpperCase(), service);
        if ( info == null ) {
            info = new SyncInfo(photo.name.toUpperCase());
            info.setSyncService(service.ordinal());
        }
        info.setSyncStatus(syncStatus);
        SyncInfoDao.saveData(info);
    }

    private static List<SyncService> getSyncServices() {

        List<SyncService> services = new ArrayList<>();

        if (Settings.getDropboxSyncEnabled())
            services.add(SyncService.Dropbox);

        if (Settings.getGoogleDriveSyncEnabled())
            services.add(SyncService.GoogleDrive);

        return services;
    }

}

package com.datonicgroup.narrate.app.dataprovider.sync;

import android.provider.Settings;
import android.util.Log;

import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.SyncInfoDao;
import com.datonicgroup.narrate.app.models.*;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Created by pigt on 10/22/17.
 */

public class SimpleAbsSyncService extends AbsSyncService {

    private File localDir = GlobalApplication.getAppContext().getFilesDir();

    private FileSyncInterface fs;


    SimpleAbsSyncService(com.datonicgroup.narrate.app.models.SyncService mSyncService, FileSyncInterface fs) {
        super(mSyncService);
        this.fs = fs;

        try {
            if (!fs.contain(""))
                createBaseFolder();
        } catch (SyncException e) {
            LogUtil.e(getClass().getSimpleName(), "Error checking root dir", e);
        }
    }

    //TODO:Proper dependency.
    private void onUpdateItem(AbsSyncItem item, int newStatus) {
        SyncInfo s = new SyncInfo();
        s.setTitle(item.uuid);
        s.setSyncService(mSyncService.ordinal());
        s.setModifiedDate(Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        s.setSyncStatus(newStatus);
        SyncInfoDao.saveData(s);
    }

    private String itemToPath(AbsSyncItem item) {
        return item.getDir() + File.separator + item.uuid;
    }

    //TODO:Day One support (Not in this file of course)
    private void psave(AbsSyncItem item) {
        onUpdateItem(item, SyncStatus.UPLOAD);
        String path = itemToPath(item);

        File local = new File(localDir, item.uuid);
        try {
            if (!local.exists())
                local.createNewFile();
            item.writeToFile(local);
            fs.upload(path, local);
            onUpdateItem(item, SyncStatus.OK);
        } catch (Exception e) {
            LogUtil.e(getClass().getSimpleName(), "Error while saving " + item.uuid, e);
        } finally {
            if (local != null)
                local.delete();
        }
    }

    //See note in AbsSyncItem
    private AbsSyncItem pdownload(AbsSyncItem item) {
        String path = itemToPath(item);
        File local = new File(localDir, item.uuid);
        try {
            if (!local.exists())
                local.createNewFile();
            item.writeToFile(local);
            fs.download(path, local);
            AbsSyncItem nitem = item.readFromFile(local);
            onUpdateItem(item, SyncStatus.OK);
            return nitem;
        } catch (Exception e) {
            LogUtil.e(getClass().getSimpleName(), "Error while downloading " + item.uuid, e);
        } finally {
            if (local != null)
                local.delete();
        }

        return null;
    }

    private boolean pdelete(AbsSyncItem item) {
        onUpdateItem(item, SyncStatus.DELETE);
        String path = itemToPath(item);

        String from = path;
        String to = item.getDir() + File.separator + "deleted" + item.uuid;

        try {
            boolean r = fs.move(from, to);
            onUpdateItem(item, SyncStatus.OK);
            return r;
        } catch (Exception e) {
            LogUtil.e(getClass().getSimpleName(), "Error while saving " + item.uuid, e);
        }

        return false;
    }

    private boolean pcontain(AbsSyncItem item) {
        try {
            return fs.contain(itemToPath(item));
        } catch (SyncException e) {
            LogUtil.e(getClass().getSimpleName(), "Error contain() " + item.uuid, e);
        }
        return false;
    }

    private void pdeleteAll()
    {
        try {
            fs.delete("");
        } catch (SyncException e) {
            LogUtil.e(getClass().getSimpleName(), "Error deleteAll " , e);
        }
    }

    private List<RemoteDataInfo> plist(String path) {
        try {
            return fs.list(path);
        } catch (SyncException e) {
            LogUtil.e(getClass().getSimpleName(), "Error list " + path, e);
        }
        return null;
    }

    private void createBaseFolder() {
        try {
            fs.mkdir("/entries/deleted");
            fs.mkdir("/photos/deleted");
        } catch (SyncException e) {
            LogUtil.e(getClass().getSimpleName(), "Error createBaseFolder ", e);
        }
    }

    @Override
    public void save(Entry entry) {
        psave(entry);
    }

    @Override
    public Entry download(String uuid) {
        Entry e = new Entry();
        e.uuid = uuid;
        return (Entry) pdownload(e);
    }

    @Override
    public void delete(Entry entry) {
        pdelete(entry);
    }

    @Override
    public boolean doesContain(Entry entry) {
        return pcontain(entry);
    }

    @Override
    public void save(Photo photo) {
        psave(photo);
    }

    //Breaking change : Both cloud and local will now use UUID.
    @Override
    public Photo downloadPhoto(String uuid) {
        Photo p = new Photo();
        p.uuid = uuid;

        return (Photo) pdownload(p);
    }

    @Override
    public boolean delete(Photo photo) {
        return pdelete(photo);
    }

    @Override
    public boolean doesContain(Photo photo) {
        return pcontain(photo);
    }

    @Override
    public void deleteEverything() { pdeleteAll(); }

    @Override
    public List<RemoteDataInfo> getRemoteEntries() throws SyncFailedException {
        return plist("/entries");
    }

    @Override
    public List<RemoteDataInfo> getRemotePhotos() throws SyncFailedException {
        return plist("/photos");
    }
}

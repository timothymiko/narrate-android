package com.datonicgroup.narrate.app.dataprovider.sync;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.RemoteDataInfo;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pigt on 10/22/17.
 */

public class DropboxFileSync implements FileSyncInterface {

    DbxClientV2 client;
    String appRoot = Settings.getDropboxSyncFolder();

    DropboxFileSync(String token) {
        DbxRequestConfig pref = new DbxRequestConfig("Narrate/1");
        client = new DbxClientV2(pref, token);
    }

    @Override
    public void upload(String path, File f) throws SyncException {
        path = appRoot + path;

        try {
            FileInputStream in = new FileInputStream(f);
            client.files().upload(path).uploadAndFinish(in);
        } catch (IOException | DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error uploading "+ path + " <- " + f.getPath(), e);
            throw new SyncException();
        }

    }

    @Override
    public void download(String path, File f) throws SyncException {
        path = appRoot + path;

        try {
            FileOutputStream out = new FileOutputStream(f);
            client.files().download(path).download(out);
        } catch (IOException | DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error downloading "+ path + " -> " + f.getPath(), e);
            throw new SyncException();
        }
    }

    @Override
    public boolean move(String from, String to) throws SyncException {
        from = appRoot + from;
        to = appRoot + to;

        try {
            RelocationResult r = client.files().moveV2(from, to);
            return true;
        } catch (DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error moving "+ from + " -> " + to, e);
            return false;
        }
    }

    @Override
    public boolean contain(String s) throws SyncException {
        s = appRoot + s;

        try {
            client.files().getMetadata(s);
            return true;
        } catch (DbxException e) {
            return false;
        }
    }

    @Override
    public void delete(String s) throws SyncException {
        s = appRoot + s;

        try {
            client.files().deleteV2(s);
        } catch (DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error deleting "+ s, e);
            throw new SyncException();
        }
    }

    @Override
    public List<RemoteDataInfo> list(String p) throws SyncException {
        p = appRoot + p;

        List<RemoteDataInfo> l = new ArrayList<RemoteDataInfo>();
        try {
            ListFolderResult lst = client.files().listFolderBuilder(p).withIncludeDeleted(true).start();
            for (Metadata m : lst.getEntries()) {
                RemoteDataInfo info = new RemoteDataInfo();
                info.name = m.getName();

                if (m instanceof FileMetadata) {
                    FileMetadata f = (FileMetadata) m;
                    info.modifiedDate = f.getServerModified().getTime();
                    info.revision = f.getRev();
                }

                info.isDirectory = m instanceof FolderMetadata;
                info.isDeleted = m instanceof DeletedMetadata;

                l.add(info);
            }
        } catch (DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error listing "+ p, e);
            throw new SyncException();
        }

        return l;
    }

    @Override
    public void mkdir(String s) throws SyncException {
        s = appRoot + s;

        try {
            client.files().createFolderV2(s);
        } catch (DbxException e) {
            LogUtil.e(getClass().getSimpleName(), "Error mkdir "+ s, e);
            throw new SyncException();
        }
    }
}


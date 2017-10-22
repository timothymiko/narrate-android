package com.datonicgroup.narrate.app.dataprovider.sync;

import com.datonicgroup.narrate.app.models.RemoteDataInfo;

import java.io.File;
import java.util.List;

/**
 * Created by pigt on 10/22/17.
 */

public interface FileSyncInterface {
    void upload(String path, File f) throws SyncException;
    void download(String path, File f) throws SyncException;
    boolean move(String from, String to) throws SyncException;
    boolean contain(String s) throws SyncException;
    void delete(String s) throws SyncException;
    List<RemoteDataInfo> list(String p) throws SyncException;
    void mkdir(String s) throws SyncException;
}

class SyncException extends Exception {

}
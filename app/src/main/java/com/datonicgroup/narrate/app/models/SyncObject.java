package com.datonicgroup.narrate.app.models;

/**
 * Created by timothymiko on 6/4/14.
 */
public class SyncObject {

    public enum Operation {
        Upload,
        Download,
        Delete
    }

    public enum Type {
        Entry,
        Photo
    }

    public String uuid;
    public RemoteDataInfo remoteDataInfo;
    public AbsSyncItem item;
    public Operation operation;

    public SyncObject(String uuid,
                      RemoteDataInfo obj,
                      AbsSyncItem item,
                      Operation operation) {
        this.uuid = uuid;
        this.remoteDataInfo = obj;
        this.item = item;
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if ( o instanceof SyncObject )
            return o.equals(((SyncObject) o).uuid);
        else
            return false;
    }
}

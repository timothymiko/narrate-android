package com.datonicgroup.narrate.app.models;

/**
 * Created by timothymiko on 10/28/14.
 */
public class SyncInfo implements Comparable<SyncInfo> {

    private String title;
    private int syncService;
    private long modifiedDate;
    private String revision;
    private int syncStatus;

    public SyncInfo() {
    }

    public SyncInfo(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSyncService() {
        return syncService;
    }

    public void setSyncService(int syncService) {
        this.syncService = syncService;
    }

    public long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(long modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int status) {
        this.syncStatus = status;
    }

    @Override
    public int compareTo(SyncInfo another) {
        return title.compareTo(another.title);
    }
}

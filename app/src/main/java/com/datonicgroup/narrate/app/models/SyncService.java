package com.datonicgroup.narrate.app.models;

/**
 * Created by timothymiko on 9/29/14.
 */
public enum SyncService {

    Dropbox(0),
    GoogleDrive(1);

    private int internalValue;

    private SyncService(int internalValue) {
        this.internalValue = internalValue;
    }

    public static SyncService lookup(int value) {
        for (SyncService status : values()) {
            if (status.internalValue == value) {
                return status;
            }
        }

        return null;
    }

    public int getInternalValue() {
        return internalValue;
    }
}

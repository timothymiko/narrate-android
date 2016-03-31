package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import com.datonicgroup.narrate.app.models.DriveEntry;

/**
 * Created by timothymiko on 1/12/16.
 */
public class DriveFile {
    public DriveEntry entry;

    public DriveFile(DriveEntry entry) {
        this.entry = entry;
    }
}

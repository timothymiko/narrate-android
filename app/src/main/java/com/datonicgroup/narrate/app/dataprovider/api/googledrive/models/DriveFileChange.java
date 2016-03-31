package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import java.util.Date;

/**
 * Created by timothymiko on 1/13/16.
 */
public class DriveFileChange {
    public String fileId;
    public boolean removed;
    public boolean deleted;
    public Date modificationDate;
    public DriveFileMetadata file;
}

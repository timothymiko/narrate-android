package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import java.util.List;

/**
 * Created by timothymiko on 1/13/16.
 */
public class DriveFileChangeList {
    public String nextPageToken;
    public String newStartPageToken;
    public List<DriveFileChange> items;
}

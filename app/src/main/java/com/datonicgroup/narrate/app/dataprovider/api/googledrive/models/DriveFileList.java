package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import java.util.List;

/**
 * Created by timothymiko on 1/12/16.
 */
public class DriveFileList {

    public List<DriveFileMetadata> items;
    public String nextPageToken;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DriveFileList: [\n");
        for (DriveFileMetadata file : items) {
            sb.append(file.toString());
            sb.append(",\n");
        }
        sb.append("\n]");
        return sb.toString();
    }
}

package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import java.util.Date;

/**
 * Created by timothymiko on 1/12/16.
 */
public class DriveFileMetadata {
    public String id;
    public String name; // v3 api
    public String title; //v2 api
    public String mimeType;
    public Date modifiedDate;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t{\n");
        sb.append("\t\t\"id\": " + id + "\n");
        sb.append("\t\t\"name\": " + name + "\n");
        sb.append("\t\t\"title\": " + title + "\n");
        sb.append("\t\t\"mimeType\": " + mimeType + "\n");
        sb.append("\t}");
        return sb.toString();
    }
}

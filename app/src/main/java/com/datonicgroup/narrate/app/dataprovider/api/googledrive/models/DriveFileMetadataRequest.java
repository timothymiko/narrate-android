package com.datonicgroup.narrate.app.dataprovider.api.googledrive.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timothymiko on 1/12/16.
 */
public class DriveFileMetadataRequest {

    private final String name;
    private final List<String> parents;
    private final String mimeType;

    public DriveFileMetadataRequest(String name, String parent, String mimeType) {
        this.name = name;
        this.mimeType = mimeType;

        this.parents = new ArrayList<>();
        this.parents.add(parent);
    }

    public DriveFileMetadataRequest(String name, List<String> parents, String mimeType) {
        this.name = name;
        this.parents = parents;
        this.mimeType = mimeType;
    }
}

package com.datonicgroup.narrate.app.models;

import com.datonicgroup.narrate.app.dataprovider.api.googledrive.models.DriveFile;

import java.util.Calendar;
import java.util.List;

/**
 * Created by timothymiko on 1/13/16.
 */
public class DriveEntry {

    public String uuid;
    public boolean isDeleted;
    public Calendar creationDate;
    public long modifiedDate;
    public String title;
    public String text;
    public boolean hasLocation;
    public String placeName;
    public double latitude;
    public double longitude;
    public boolean starred;
    public List<String> tags;

    public DriveEntry(Entry e) {
        this.uuid = e.uuid;
        this.isDeleted = e.isDeleted;
        this.creationDate = e.creationDate;
        this.modifiedDate = e.modifiedDate;
        this.title = e.title;
        this.text = e.text;
        this.hasLocation = e.hasLocation;
        this.placeName = e.placeName;
        this.latitude = e.latitude;
        this.longitude = e.longitude;
        this.starred = e.starred;
        this.tags = e.tags;
    }

    public Entry toEntry() {
        Entry e = new Entry();
        e.uuid = this.uuid;
        e.isDeleted = this.isDeleted;
        e.creationDate = this.creationDate;
        e.modifiedDate = this.modifiedDate = e.modifiedDate;
        e.title = this.title;
        e.text = this.text;
        e.hasLocation = this.hasLocation;
        e.placeName = this.placeName;
        e.latitude = this.latitude;
        e.longitude = this.longitude;
        e.starred = this.starred;
        e.tags = this.tags;
        return e;
    }
}

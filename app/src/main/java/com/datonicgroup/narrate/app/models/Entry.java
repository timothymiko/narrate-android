package com.datonicgroup.narrate.app.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Created by timothymiko on 6/3/14.
 */
public class Entry extends AbsSyncItem implements Parcelable {

    public static final String ACTION_SAVE_ENTRY = "com.narrate.action.SAVE_ENTRY";
    public static final String ACTION_NEW_ENTRY = "com.narrate.action.NEW_ENTRY";
    public static final String EXTRA_DATA = "com.narrate.extra.ENTRY_DATA";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATE_TIME = "date";
    public static final String EXTRA_PHOTO = "photo";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_PLACE_NAME = "place";
    public static final String EXTRA_TAGS = "tags";
    public static final String EXTRA_BOOKMARK = "bookmark";
    public static final String EXTRA_DRIVE_ID = "googleDriveId";

    public int id;
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
    public long deletionDate;
    public List<Photo> photos;

    public String revisionKey;
    public int dropboxSyncStatus;

    public String googleDriveFileId;
    public String googleDrivePhotoFileId;

    public Entry() {
        creationDate = Calendar.getInstance();
        tags = new ArrayList();
        uuid = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

    private Entry(Parcel in) {
        this.uuid = in.readString();
        this.modifiedDate = in.readLong();
        this.creationDate = (Calendar) in.readSerializable();
        this.title = in.readString();
        this.text = in.readString();
        this.hasLocation = in.readByte() != 0;
        this.placeName = in.readString();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.starred = in.readByte() != 0;
        this.tags = new ArrayList();
        in.readList(this.tags, ArrayList.class.getClassLoader());
        this.isDeleted = in.readByte() != 0;
        this.deletionDate = in.readLong();
        this.photos = new ArrayList<>();
        in.readList(this.photos, Photo.class.getClassLoader());
        this.googleDriveFileId = in.readString();
        this.googleDrivePhotoFileId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uuid);
        dest.writeLong(this.modifiedDate);
        dest.writeSerializable(this.creationDate);
        dest.writeString(this.title);
        dest.writeString(this.text);
        dest.writeByte(hasLocation ? (byte) 1 : (byte) 0);
        dest.writeString(this.placeName);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeByte(starred ? (byte) 1 : (byte) 0);
        dest.writeList(this.tags);
        dest.writeByte(isDeleted ? (byte) 1 : (byte) 0);
        dest.writeLong(this.deletionDate);
        dest.writeList(this.photos);
        dest.writeString(this.googleDriveFileId);
        dest.writeString(this.googleDrivePhotoFileId);
    }

    public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
        public Entry createFromParcel(Parcel source) {
            return new Entry(source);
        }

        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if ( o instanceof Entry ) {
            return uuid.equals(((Entry) o).uuid);
        } else
            return false;
    }
}

package com.datonicgroup.narrate.app.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by timothymiko on 10/15/14.
 */
public class Photo extends AbsSyncItem implements Parcelable {

    public String name;
    public String path;
    public long modifiedDate;

    public Photo() {

    }

    public Photo(Parcel in) {
        this.name = in.readString();
        this.uuid = in.readString();
        this.path = in.readString();
        this.modifiedDate = in.readLong();
        this.isDeleted = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(uuid);
        dest.writeString(path);
        dest.writeLong(modifiedDate);
        dest.writeInt(isDeleted ? 1 : 0);
    }

    public static final Parcelable.Creator<Photo> CREATOR = new Parcelable.Creator<Photo>() {
        public Photo createFromParcel(Parcel source) {
            return new Photo(source);
        }

        public Photo[] newArray(int size) {
            return new Photo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if ( o instanceof Photo ) {
            return path.equals(((Photo)o).path);
        } else
            return false;
    }
}

package com.datonicgroup.narrate.app.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.datonicgroup.narrate.app.dataprovider.providers.PhotosDao;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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

    @Override
    public String getDir() {
        return "/photos";
    }

    //TODO: Add more photo format.
    @Override
    public void writeToFile(File file) throws IOException {
        File local = new File(PhotosDao.getPhotosFolder(), uuid);
        copy(local, file);
    }

    @Override
    public AbsSyncItem readFromFile(File file) throws IOException {
        File local = new File(PhotosDao.getPhotosFolder(), uuid);
        copy(file, local);
        name = file.getName();
        path = local.getPath();
        syncInfo = null; //TODO:Don't really know what it does.
        return this;
    }

    private void copy(File src, File dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            byte[] buff = new byte[1024];
            while (in.read(buff) > 0)
                out.write(buff);

        } catch (IOException e) {
            LogUtil.e(getClass().getSimpleName(), "Error copying image " + src.toString() + " -> " + dst.toString(), e);
        } finally {
            try {
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
            } catch (IOException e) {
                LogUtil.e(getClass().getSimpleName(), "Error closing copying image " + src.toString() + " -> " + dst.toString(), e);
            }
        }
    }
}

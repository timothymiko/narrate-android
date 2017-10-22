package com.datonicgroup.narrate.app.models;

import java.io.File;
import java.io.IOException;

/**
 * Created by timothymiko on 11/14/14.
 */
public abstract class AbsSyncItem {

    public String uuid;
    public SyncInfo syncInfo;
    public boolean isDeleted;

    public abstract String getDir();
    public abstract void writeToFile(File file) throws IOException;
    //Ideally, We want to have this method mutate the state of itself.
    public abstract AbsSyncItem readFromFile(File file) throws IOException;

}

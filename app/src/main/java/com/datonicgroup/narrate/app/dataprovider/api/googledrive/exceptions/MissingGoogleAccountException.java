package com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions;

/**
 * Created by timothymiko on 1/12/16.
 */
public class MissingGoogleAccountException extends Exception {

    public MissingGoogleAccountException(String className) {
        super(className + "expected a valid Google account, but didn't find one.");
    }

}

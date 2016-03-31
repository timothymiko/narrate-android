package com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions;

import com.datonicgroup.narrate.app.util.PermissionsUtil;

/**
 * Created by timothymiko on 1/12/16.
 */
public class PermissionsException extends Exception {
    public PermissionsException(String expectedPermission) {
        super("Expected access to " + expectedPermission + ".");
    }
}

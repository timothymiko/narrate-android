package com.datonicgroup.narrate.app.dataprovider.api.googledrive.exceptions;

/**
 * Created by timothymiko on 1/12/16.
 */
public class InvalidAccountTypeException extends Exception {

    public InvalidAccountTypeException(String expectedType, String actualType) {
        super("InvalidAccountTypeException --> \nExpected: " + expectedType + "\nActual: " + actualType);
    }
}

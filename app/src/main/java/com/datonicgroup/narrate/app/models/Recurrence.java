package com.datonicgroup.narrate.app.models;

/**
 * Created by timothymiko on 9/24/14.
 */
public enum Recurrence {

    Once(0),
    Daily(1),
    Weekly(2),
    Monthly(3),
    Yearly(4);

    private int internalValue;

    private Recurrence(int internalValue) {
        this.internalValue = internalValue;
    }

    public static Recurrence lookup(int value) {
        for (Recurrence status : values()) {
            if (status.internalValue == value) {
                return status;
            }
        }

        return null;
    }

    public int getInternalValue() {
        return internalValue;
    }
}

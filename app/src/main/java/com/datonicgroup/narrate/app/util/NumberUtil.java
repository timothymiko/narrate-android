package com.datonicgroup.narrate.app.util;

/**
 * Created by timothymiko on 9/23/14.
 */
public class NumberUtil {

    public static String getOrdinalSuffix(int i) {
        int j = i % 10, k = i % 100;
        if (j == 1 && k != 11) {
            return i + "st";
        }
        if (j == 2 && k != 12) {
            return i + "nd";
        }
        if (j == 3 && k != 13) {
            return i + "rd";
        }
        return i + "th";
    }
}

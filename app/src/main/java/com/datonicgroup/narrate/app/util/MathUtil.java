package com.datonicgroup.narrate.app.util;

/**
 * Created by timothymiko on 6/10/14.
 */
public class MathUtil {

    /**
     * Generates a random number in the range [min, max]
     *
     * @param min minimum value to be returned
     * @param max maximum value to be returned
     *
     * @return random number
     */
    public static int randomNumberInRange(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }
}

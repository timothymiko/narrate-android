package com.datonicgroup.narrate.app.models.comparators;

import com.datonicgroup.narrate.app.models.Entry;

import java.util.Comparator;

/**
 * Created by timothymiko on 1/15/15.
 */
public abstract class BaseEntryComparator implements Comparator<Entry> {

    static <T extends Comparable<T>> int cp(T a, T b) {
        return
                a==null ?
                        (b==null ? 0 : Integer.MIN_VALUE) :
                        (b==null ? Integer.MAX_VALUE : a.compareTo(b));
    }

}

package com.datonicgroup.narrate.app.models.comparators;

import com.datonicgroup.narrate.app.models.Entry;

/**
 * Created by timothymiko on 12/28/14.
 */
public class EntryReverseAlphabetComparator extends BaseEntryComparator {
    @Override
    public int compare(Entry lhs, Entry rhs) {
        return cp(rhs.title, lhs.title);
    }
}
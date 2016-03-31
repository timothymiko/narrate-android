package com.datonicgroup.narrate.app.models.predicates;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.models.Entry;

/**
 * Created by timothymiko on 1/1/15.
 */
public class EntryBookmarkedPredicate implements Predicate<Entry> {
    @Override
    public boolean apply(Entry entry) {
        return entry.starred;
    }
}

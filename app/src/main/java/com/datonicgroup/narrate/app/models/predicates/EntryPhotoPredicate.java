package com.datonicgroup.narrate.app.models.predicates;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.models.Entry;

/**
 * Created by timothymiko on 12/28/14.
 */
public class EntryPhotoPredicate implements Predicate<Entry> {
    @Override
    public boolean apply(Entry entry) {
        return !entry.photos.isEmpty();
    }
}

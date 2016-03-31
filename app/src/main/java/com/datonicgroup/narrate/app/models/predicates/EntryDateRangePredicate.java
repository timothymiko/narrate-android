package com.datonicgroup.narrate.app.models.predicates;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.models.Entry;

/**
 * Created by timothymiko on 12/28/14.
 */
public class EntryDateRangePredicate implements Predicate<Entry> {

    private long start;
    private long end;


    public EntryDateRangePredicate(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean apply(Entry entry) {
        return (entry.creationDate.getTimeInMillis() >= start) && (entry.creationDate.getTimeInMillis() <= end);
    }
}

package com.datonicgroup.narrate.app.models.predicates;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.models.Entry;

import java.util.List;

/**
 * Created by timothymiko on 12/28/14.
 */
public class EntryTagsPredicate implements Predicate<Entry> {

    public List<String> tags;

    public EntryTagsPredicate(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean apply(Entry entry) {
        if ( entry.tags != null && !entry.tags.isEmpty() ) {
            for (int i = 0; i < entry.tags.size(); i++) {
                for ( int j = 0; j < tags.size(); j ++ ) {
                    if ( entry.tags.get(i).equals(tags.get(j)) )
                        return true;
                }
            }
        }

        return false;
    }
}

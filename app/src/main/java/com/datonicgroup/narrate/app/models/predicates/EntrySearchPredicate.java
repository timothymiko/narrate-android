package com.datonicgroup.narrate.app.models.predicates;

import com.android.internal.util.Predicate;
import com.datonicgroup.narrate.app.models.Entry;

import java.util.Locale;

/**
 * Created by timothymiko on 12/3/14.
 */
public class EntrySearchPredicate implements Predicate<Entry> {

    String text;
    String title;
    String search;

    public EntrySearchPredicate(String search) {
        this.search = search;
    }

    @Override
    public boolean apply(Entry entry) {
        text = entry.text.toLowerCase(Locale.getDefault()).trim();
        title = entry.title.toLowerCase(Locale.getDefault()).trim();

        if (text.contains(search) || title.contains(search))
            return true;
        else {
            for (String tag : entry.tags) {
                if (tag.toLowerCase(Locale.getDefault()).contains(search)) {
                    return true;
                }
            }
        }
        return false;
    }
}

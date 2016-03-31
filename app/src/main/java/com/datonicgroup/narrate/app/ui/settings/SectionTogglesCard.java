package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.datonicgroup.narrate.app.ui.calendar.CalendarFragment;
import com.datonicgroup.narrate.app.ui.entries.EntriesListFragment;
import com.datonicgroup.narrate.app.ui.entries.PhotosGridFragment;
import com.datonicgroup.narrate.app.ui.places.PlacesFragment;

/**
 * Created by timothymiko on 12/30/14.
 */
public class SectionTogglesCard extends PreferenceCard implements CompoundButton.OnCheckedChangeListener {

    private SwitchPreference mEntries;
    private SwitchPreference mPhotos;
    private SwitchPreference mCalendar;
    private SwitchPreference mPlaces;

    public SectionTogglesCard(Context context) {
        super(context);
    }

    public SectionTogglesCard(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SectionTogglesCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.toggle_sections);

        mEntries = new SwitchPreference(getContext());
        mPhotos = new SwitchPreference(getContext());
        mCalendar = new SwitchPreference(getContext());
        mPlaces = new SwitchPreference(getContext());

        mEntries.setTitle(R.string.toggle_entries);
        mPhotos.setTitle(R.string.toggle_photos);
        mCalendar.setTitle(R.string.toggle_calendar);
        mPlaces.setTitle(R.string.toggle_places);

        mEntries.setTag("toggle_sections_list");
        mPhotos.setTag("toggle_sections_photos");
        mCalendar.setTag("toggle_sections_calendar");
        mPlaces.setTag("toggle_sections_places");

        boolean mListEnabled = false;
        boolean mPhotosEnabled = false;
        boolean mCalendarEnabled = false;
        boolean mPlacesEnabled = false;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        int size = prefs.getInt("main_toolbar_num_sections", 3);
        for (int i = 0; i < size; i++) {
            String tag = prefs.getString("main_toolbar_order_" + i, null);

            if (tag != null) {
                switch (tag) {
                    case EntriesListFragment.TAG:
                        mListEnabled = true;
                        break;
                    case CalendarFragment.TAG:
                        mCalendarEnabled = true;
                        break;
                    case PlacesFragment.TAG:
                        mPlacesEnabled = true;
                        break;
                    case PhotosGridFragment.TAG:
                        mPhotosEnabled = true;
                        break;
                }
            }
        }

        mEntries.setChecked(mListEnabled);
        mPhotos.setChecked(mPhotosEnabled);
        mCalendar.setChecked(mCalendarEnabled);
        mPlaces.setChecked(mPlacesEnabled);

        mEntries.setOnCheckedChangedListener(this);
        mPhotos.setOnCheckedChangedListener(this);
        mCalendar.setOnCheckedChangedListener(this);
        mPlaces.setOnCheckedChangedListener(this);

        addView(mEntries);
        addView(mPhotos);
        addView(mCalendar);
        addView(mPlaces);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        String key = (String)buttonView.getTag();

        String tag = null;
        switch (key) {
            case "toggle_sections_list":
                tag = EntriesListFragment.TAG;
                break;
            case "toggle_sections_photos":
                tag = PhotosGridFragment.TAG;
                break;
            case "toggle_sections_calendar":
                tag = CalendarFragment.TAG;
                break;
            case "toggle_sections_places":
                tag = PlacesFragment.TAG;
                break;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();

        int size = prefs.getInt("main_toolbar_num_sections", 4);

        if ( isChecked ) {
            // adding a section

            if ( tag != null ) {

                editor.putString("main_toolbar_order_" + size, tag);
                editor.putInt("main_toolbar_num_sections", size + 1);
                editor.commit();
            }

        } else {

            if ( size == 1 ) {
                switch (key) {
                    case "toggle_sections_list":
                        mEntries.setChecked(true);
                        break;
                    case "toggle_sections_photos":
                        mPhotos.setChecked(true);
                        break;
                    case "toggle_sections_calendar":
                        mCalendar.setChecked(true);
                        break;
                    case "toggle_sections_places":
                        mPlaces.setChecked(true);
                        break;
                }
                Toast.makeText(getContext(), R.string.toggle_sections_min_error, Toast.LENGTH_LONG).show();
                return;
            }

            // removing a section
            String[] viewPagerOrder = new String[size];
            for (int i = 0; i < size; i++) {
                String ptag = prefs.getString("main_toolbar_order_" + i, null);
                viewPagerOrder[i] = ptag.equals(tag) ? null : ptag;
            }

            int count = 0;
            for (int i = 0; i < viewPagerOrder.length; i++) {
                if ( viewPagerOrder[i] != null ) {
                    editor.putString("main_toolbar_order_" + count, viewPagerOrder[i]);
                    count++;
                }
            }

            editor.remove("main_toolbar_order_" + count);
            editor.putInt("main_toolbar_num_sections", size-1);
            editor.commit();
        }

        Intent i = new Intent(LocalContract.ACTION);
        i.putExtra(LocalContract.COMMAND, LocalContract.UPDATE_SECTIONS);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(i);

    }
}

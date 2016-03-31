package com.datonicgroup.narrate.app.ui.views;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.datonicgroup.narrate.app.ui.calendar.CalendarFragment;
import com.datonicgroup.narrate.app.ui.entries.EntriesListFragment;
import com.datonicgroup.narrate.app.ui.entries.PhotosGridFragment;
import com.datonicgroup.narrate.app.ui.places.PlacesFragment;

/**
 * Created by timothymiko on 12/14/14.
 */
public class FragmentPagerAdapter extends SmartFragmentStatePagerAdapter {

    private String[] mOrder;
//    private HashMap<String, Fragment> mFragments = new HashMap<>();

    public FragmentPagerAdapter(FragmentManager fragmentManager, String[] fragments) {
        super(fragmentManager);
        this.mOrder = fragments;
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return mOrder.length;
    }

    @Override
    public int getItemPosition(Object object) {

        Fragment f = (Fragment) object;
        for ( int i = 0; i < mOrder.length; i++ ) {
            if ( f.equals(mOrder[i]) )
                return i;
        }

        return POSITION_NONE;
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int position) {
        String id = mOrder[position];

        Fragment frag = null;

        switch (id) {
            case EntriesListFragment.TAG:
                frag = EntriesListFragment.getInstance();
                break;
            case PhotosGridFragment.TAG:
                frag = PhotosGridFragment.newInstance();
                break;
            case CalendarFragment.TAG:
                frag = CalendarFragment.newInstance();
                break;
            case PlacesFragment.TAG:
                frag = PlacesFragment.newInstance();
                break;
        }

        return frag;
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        return "Page " + position;
    }

}

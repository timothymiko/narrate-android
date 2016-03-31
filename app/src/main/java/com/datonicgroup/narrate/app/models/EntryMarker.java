package com.datonicgroup.narrate.app.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by timothymiko on 11/18/14.
 */
public class EntryMarker implements ClusterItem {

    public Entry entry;
    private LatLng mPosition;

    public EntryMarker(Entry e) {
        this.mPosition = new LatLng(e.latitude, e.longitude);
        this.entry = e;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
}

package com.datonicgroup.narrate.app.ui.entryeditor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Pair;

import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.util.LocationUtil;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

public class NoteToSelfActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);

        if ((action.equals("com.google.android.gm.action.AUTO_SEND") && type != null)) {

            if (text != null) {

                final Entry entry = new Entry();
                entry.creationDate = Calendar.getInstance();
                entry.modifiedDate = entry.creationDate.getTimeInMillis();

                entry.title = "";
                entry.text = text;

                if (Settings.getAutomaticLocation()) {

                    String locationProvider = LocationManager.NETWORK_PROVIDER;
                    LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);

                    if (lastKnownLocation != null) {
                        LatLng location = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                        entry.hasLocation = true;
                        entry.latitude = location.latitude;
                        entry.longitude = location.longitude;

                        List<Pair<String, LatLng>> localPlaces = PlacesDao.getPlaces(location.latitude, location.longitude, 10);
                        MutableArrayList<Pair<String, Double>> places = new MutableArrayList();

                        if ( localPlaces.size() > 0 ) {
                            for (Pair<String, LatLng> p : localPlaces) {
                                double dist = LocationUtil.distanceBetweenLocations(location.latitude, location.longitude, p.second.latitude, p.second.longitude);
                                places.add(new Pair<>(p.first, dist));
                            }

                            places.sort(new Comparator<Pair<String, Double>>() {
                                @Override
                                public int compare(Pair<String, Double> lhs, Pair<String, Double> rhs) {
                                    return lhs.second.compareTo(rhs.second);
                                }
                            });

                            entry.placeName = places.get(0).first;
                        }

                    }
                }

                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        DataManager.getInstance().save(entry, true);
                    }
                }.start();
            }
        }

        finish();
    }
}

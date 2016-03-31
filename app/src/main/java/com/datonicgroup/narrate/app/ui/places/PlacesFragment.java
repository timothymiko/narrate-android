package com.datonicgroup.narrate.app.ui.places;


import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.EntryMarker;
import com.datonicgroup.narrate.app.ui.base.BaseEntryFragment;
import com.datonicgroup.narrate.app.ui.dialogs.EntryListDialogFragment;
import com.datonicgroup.narrate.app.ui.entries.EntriesRecyclerAdapter;
import com.datonicgroup.narrate.app.ui.entries.ViewEntryActivity;
import com.datonicgroup.narrate.app.util.PermissionsUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlacesFragment extends BaseEntryFragment implements ClusterManager.OnClusterClickListener<EntryMarker>, ClusterManager.OnClusterItemClickListener<EntryMarker> {

    public static final String TAG = "Places";

    /**
     * Control
     */
    private boolean portraitOrientation;

    /**
     * Data
     */
    private Location lastKnownLocation;

    //    private List<Entry> mSheetEntries;
    private EntriesRecyclerAdapter mSheetAdapter;

    /**
     * Maps
     */
    private ClusterManager<EntryMarker> mClusterManager;

    /**
     * Views
     */
    private MapView mapView;
    private GoogleMap map;
    private final EntryListDialogFragment mListDialog = new EntryListDialogFragment();

    public static PlacesFragment newInstance() {
        PlacesFragment frag = new PlacesFragment();
        return frag;
    }

    @Override
    public void onDataUpdated() {
        if (mClusterManager != null) {
            mClusterManager.clearItems();

            List<EntryMarker> mItems = new ArrayList<>();
            Iterator<Entry> iter = mainActivity.entries.iterator();
            while (iter.hasNext()) {
                Entry e = iter.next();
                if (e.hasLocation)
                    mItems.add(new EntryMarker(e));
            }
            mClusterManager.addItems(mItems);
            mItems.clear();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.mTitle = getString(R.string.places);

        portraitOrientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRoot = inflater.inflate(R.layout.fragment_locations, container, false);

        if (PermissionsUtil.checkAndRequest(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION, 100, R.string.permission_explanation_location, null)) {
            setup();
        }

        return mRoot;
    }

    private void setup() {
        mSheetAdapter = new EntriesRecyclerAdapter(mainActivity.entries);

        mapView = (MapView) mRoot.findViewById(R.id.mapView);
        mapView.setAlwaysDrawnWithCacheEnabled(true);
        mapView.setPersistentDrawingCache(MapView.PERSISTENT_ALL_CACHES);
        mapView.onCreate(null);

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        map = mapView.getMap();

        mClusterManager = new ClusterManager<EntryMarker>(getActivity(), map);
        map.setOnCameraChangeListener(mClusterManager);
        map.setOnMarkerClickListener(mClusterManager);

        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);

        onDataUpdated();

        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(false);

            if ( getActivity() != null && getActivity().getSystemService(Context.LOCATION_SERVICE) != null) {
                String locationProvider = LocationManager.NETWORK_PROVIDER;
                LocationManager mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                lastKnownLocation = mLocationManager.getLastKnownLocation(locationProvider);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ( !isAdded() )
                        return;

                    if ( lastKnownLocation != null ) {
                        LatLng position = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        CameraPosition newPosition = CameraPosition.fromLatLngZoom(position, 12.0f);
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition));
                    }

                }
            }, 500);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mSheetAdapter != null)
            mSheetAdapter.updateTimeFormat();
    }
    @Override
    public void onResume() {
        if ( mapView != null )
            mapView.onResume();

        super.onResume();

        if ( mapView != null ) {
            mapView.setAlpha(0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // prevent a black screen from flashing when resuming this fragment
                    if (getActivity() != null && mRoot != null) {
                        mapView.animate().alpha(1f).setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (getActivity() != null && mRoot != null) {
                                    findViewById(R.id.places_root).setBackgroundDrawable(null);
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        }).start();
                    }
                }
            }, 300);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        if ( getActivity() != null && mRoot != null )
            findViewById(R.id.places_root).setBackgroundColor(getResources().getColor(R.color.white));
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();
    }

    @Override
    public boolean onClusterClick(Cluster<EntryMarker> entryMarkerCluster) {
        Log.d("", "onClusterClick()");
        ArrayList<Entry> entries = new ArrayList<>();

        for ( EntryMarker marker : entryMarkerCluster.getItems())
            entries.add(marker.entry);

        mListDialog.setData(entries);
        mListDialog.show(getFragmentManager(), "EntryListDialogFragment");

        return true;
    }

    @Override
    public boolean onClusterItemClick(EntryMarker entryMarker) {
        Log.d("", "onClusterItemClick()");
        Intent intent = new Intent(getActivity(), ViewEntryActivity.class);
        Bundle b = new Bundle();
        b.putParcelable(ViewEntryActivity.ENTRY_KEY, entryMarker.entry);
        intent.putExtras(b);
        startActivity(intent);
        return true;
    }

    @Override
    protected void showLoader() {
        // This is ignored because this section does not have a loader.
    }

    public void onLocationPermissionVerified() {
        setup();
    }
}

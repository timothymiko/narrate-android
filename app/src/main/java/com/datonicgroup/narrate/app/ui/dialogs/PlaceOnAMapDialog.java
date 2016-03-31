package com.datonicgroup.narrate.app.ui.dialogs;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by timothymiko on 1/4/15.
 */
public class PlaceOnAMapDialog extends FragmentActivity {

    private String title;
    private LatLng location;

    private GoogleMap map;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getIntent().getExtras();
        this.title = args.getString("title", null);
        this.location = args.getParcelable("location");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setContentView(R.layout.dialog_place_on_a_map);

        TextView textViewTitle = (TextView) findViewById(R.id.dialog_title);
        if (this.title != null)
            textViewTitle.setText(this.title);
        else
            textViewTitle.setText(location.latitude + ", " + location.longitude);

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        map = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        map.getUiSettings().setScrollGesturesEnabled(false);
        map.getUiSettings().setZoomGesturesEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);

        map.addMarker(new MarkerOptions()
                .position(location));

        CameraPosition newPosition = CameraPosition.fromLatLngZoom(location, 12.0f);
        map.moveCamera(CameraUpdateFactory.newCameraPosition(newPosition));
    }
}

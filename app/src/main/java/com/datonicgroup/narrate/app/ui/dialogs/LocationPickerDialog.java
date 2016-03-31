package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.Window;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.ui.LocalContract;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by timothymiko on 9/26/14.
 */
public class LocationPickerDialog extends FragmentActivity implements View.OnClickListener {

    private GoogleMap mMap;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.location_picker_dialog);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMap = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        mMap.setMyLocationEnabled(true);

        if ( getIntent().getParcelableExtra("location") != null ) {
            final CameraPosition newPosition = CameraPosition.fromLatLngZoom((LatLng) getIntent().getParcelableExtra("location"), 15);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(newPosition));
                }
            }, 500);
        }

        findViewById(R.id.save).setOnClickListener(this);
        findViewById(R.id.cancel).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                Intent result = new Intent(LocalContract.ACTION);
                result.putExtra(LocalContract.COMMAND, LocalContract.UPDATE_ENTRY_LOCATION);
                result.putExtra("location", mMap.getCameraPosition().target);
                LocalBroadcastManager.getInstance(this).sendBroadcast(result);
                finish();
                break;
            case R.id.cancel:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;
        }
    }
}

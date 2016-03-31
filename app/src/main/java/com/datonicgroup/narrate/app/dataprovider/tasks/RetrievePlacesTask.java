package com.datonicgroup.narrate.app.dataprovider.tasks;

import android.os.AsyncTask;
import android.util.Pair;

import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.util.LocationUtil;
import com.datonicgroup.narrate.app.util.LogUtil;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Created by timothymiko on 1/8/15.
 */
public class RetrievePlacesTask  extends AsyncTask<LatLng, Void, MutableArrayList<Pair<String, Double>>> {

    public static final String API_KEY = "AIzaSyAr3X3_n6t92AgwOYLJIN8mL8cdnJ_o2gM";
    public static final String PLACES_ENDPOINT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    public String pageToken;
    public boolean hasMoreData;
    private boolean hasLocalPlaces;

    private RetrievePlacesTask() {
    }

    public RetrievePlacesTask(boolean hasLocalPlaces, String token) {
        this.hasLocalPlaces = hasLocalPlaces;
        this.pageToken = token;
    }

    protected LatLng location;
    protected MutableArrayList<Pair<String, Double>> localPlaces;

    @Override
    protected MutableArrayList<Pair<String, Double>> doInBackground(LatLng... params) {
        location = params[0];

        StringBuilder sb = new StringBuilder();
        sb.append(getUrlWithKey(PLACES_ENDPOINT));
        sb.append("&location=");
        sb.append(location.latitude);
        sb.append(",");
        sb.append(location.longitude);
        sb.append("&radius=500");

        if ( pageToken != null ) {
            sb.append("&pagetoken=");
            sb.append(pageToken);
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .url(sb.toString())
                .get()
                .build();

        LogUtil.log(RetrievePlacesTask.class.getSimpleName(), "Request: " + request.toString());

        Call call = client.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            MutableArrayList<Pair<String, Double>> places = new MutableArrayList<>();

            if (response.isSuccessful()) {

                String body = response.body().string();
                LogUtil.log(RetrievePlacesTask.class.getSimpleName(), "Response: " + body);

                JSONObject data = new JSONObject(body);
                JSONArray results = data.getJSONArray("results");

                try {
                    pageToken = data.getString("next_page_token");
                    hasMoreData = true;
                } catch (JSONException e) {
                    pageToken = null;
                    hasMoreData = false;
                }

                JSONObject obj;
                for ( int i = 0; i < results.length(); i++ ) {
                    obj = (JSONObject) results.get(i);
                    JSONObject location = obj.getJSONObject("geometry").getJSONObject("location");
                    double latitude = location.getDouble("lat");
                    double longitude = location.getDouble("lng");
                    places.add(new Pair<>(obj.getString("name"),
                            LocationUtil.distanceBetweenLocations(this.location.latitude, this.location.longitude, latitude, longitude)));
                }

            }

            if ( !hasLocalPlaces ) {
                List<Pair<String, LatLng>> localPlaces = PlacesDao.getPlaces(location.latitude, location.longitude, 10);
                this.localPlaces = new MutableArrayList<Pair<String, Double>>();

                for (Pair<String, LatLng> p : localPlaces) {
                    double dist = LocationUtil.distanceBetweenLocations(this.location.latitude, this.location.longitude, p.second.latitude, p.second.longitude);
                    places.add(new Pair<>(p.first, dist));
                    this.localPlaces.add(new Pair<>(p.first, dist));
                }

            }


            if ( localPlaces != null )
                localPlaces.sort(mSort);

            places.sort(mSort);

            return places;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static final String getUrlWithKey(String endpoint) {
        StringBuilder sb = new StringBuilder();
        sb.append(endpoint);
        sb.append("?key=");
        sb.append(API_KEY);
        return sb.toString();
    }

    private final Comparator<Pair<String, Double>> mSort = new Comparator<Pair<String, Double>>() {
        @Override
        public int compare(Pair<String, Double> lhs, Pair<String, Double> rhs) {
            return lhs.second.compareTo(rhs.second);
        }
    };
}
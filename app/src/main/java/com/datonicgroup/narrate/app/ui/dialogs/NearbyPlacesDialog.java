package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.tasks.RetrievePlacesTask;
import com.datonicgroup.narrate.app.models.MutableArrayList;
import com.datonicgroup.narrate.app.util.ScreenUtil;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;
import java.util.List;

/**
 * Created by timothymiko on 9/26/14.
 */
public class NearbyPlacesDialog extends MaterialDialogFragment {

    public static final String API_KEY = "AIzaSyAIVuArbVquNjo77AYLb1KOU8vO7sffeDY";
    public static final String PLACES_ENDPOINT = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    public interface PlacesListener {
        void onPlaceSelected(String place);
    }

    private PlacesListener mListener;

    private LatLng mLocation;

    private ListView mListView;
    private Adapter mAdapter;
    private MutableArrayList<Pair<String, Double>> places;
    private String pageToken;

    private View mLoadingview;

    private View mLoadingFooter;

    private boolean loadingData;
    private boolean hasMoreData;
    private boolean hasLocalPlaces;
    private boolean autoSelectFirstOption;

    public void setLatLng(LatLng latLng) {
        this.mLocation = latLng;

        if (getActivity() != null)
            requestData(latLng);
    }

    public void setPlacesListener(PlacesListener listener) {
        this.mListener = listener;
    }

    public void autoSelectFirst() {
        if (places != null && places.size() > 0) {
            if (mListener != null)
                mListener.onPlaceSelected(mAdapter.getItem(0).first);
        } else {
            autoSelectFirstOption = true;
        }
    }

    public void cancelAutoSelect() {
        autoSelectFirstOption = false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {

            setTitle(R.string.nearby_places);
            setContentView(R.layout.nearby_places);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            dialog.findViewById(R.id.dialog_buttons_layout).setVisibility(View.GONE);

            mLoadingFooter = View.inflate(getActivity(), R.layout.loading_footer, null);

            mListView = (ListView) dialog.findViewById(R.id.items);
            mLoadingview = dialog.findViewById(R.id.loader);

            if (mListView.getHeight() > 0)
                mListView.getLayoutParams().height = Math.min(mListView.getHeight(), ScreenUtil.getPixelsFromDP(300));
            else
                mListView.getLayoutParams().height = Math.max(mListView.getHeight(), ScreenUtil.getPixelsFromDP(300));

            mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (firstVisibleItem > (totalItemCount - (2 * visibleItemCount)) && !loadingData && hasMoreData) {
                        mListView.addFooterView(mLoadingFooter, null, false);
                        requestData(mLocation);
                    }
                }
            });

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dismiss();
                    if (mListener != null)
                        mListener.onPlaceSelected(mAdapter.getItem(position).first);
                }
            });

            if (mAdapter == null)
                requestData(mLocation);
            else {
                mListView.setAdapter(mAdapter);
                mLoadingview.setVisibility(View.GONE);
            }

            return dialog;

        } else
            return null;
    }

    private void requestData(LatLng latLng) {
        loadingData = true;
        new RetrievePlacesTask(mAdapter != null, pageToken) {
            @Override
            protected void onPostExecute(MutableArrayList<Pair<String, Double>> places) {
                super.onPostExecute(places);
                NearbyPlacesDialog.this.pageToken = this.pageToken;
                NearbyPlacesDialog.this.hasMoreData = this.hasMoreData;

                if (mAdapter == null) {

                    NearbyPlacesDialog.this.places = new MutableArrayList<>();

                    if (places != null)
                        NearbyPlacesDialog.this.places.addAll(places);

                    mAdapter = new Adapter(getActivity(), R.layout.simple_list_item_1, NearbyPlacesDialog.this.places);
                    mListView.setAdapter(mAdapter);
                    mLoadingview.setVisibility(View.GONE);
                } else {
                    NearbyPlacesDialog.this.places.addAll(places);
                    NearbyPlacesDialog.this.places.sort(mSort);
                    mAdapter.notifyDataSetChanged();
                    mListView.removeFooterView(mLoadingFooter);
                }

                NearbyPlacesDialog.this.loadingData = false;

            }
        }.execute(latLng);
    }

//    public class RetrievePlacesTask extends AsyncTask<LatLng, Void, MutableArrayList<Pair<String, Double>>> {
//
//        private RetrievePlacesTask() {
//            loadingData = true;
//        }
//
//        LatLng location;
//        MutableArrayList<Pair<String, Double>> localPlaces;
//
//        @Override
//        protected MutableArrayList<Pair<String, Double>> doInBackground(LatLng... params) {
//            location = params[0];
//
//            StringBuilder sb = new StringBuilder();
//            sb.append(getUrlWithKey(PLACES_ENDPOINT));
//            sb.append("&location=");
//            sb.append(location.latitude);
//            sb.append(",");
//            sb.append(location.longitude);
//            sb.append("&radius=500");
//
//            if ( pageToken != null ) {
//                sb.append("&pagetoken=");
//                sb.append(pageToken);
//            }
//
//            OkHttpClient client = new OkHttpClient();
//            Request request = new Request.Builder()
//                    .addHeader("Content-Type", "application/json")
//                    .url(sb.toString())
//                    .get()
//                    .build();
//
//            LogUtil.log(CreateUserTask.class.getSimpleName(), "Request: " + request.toString());
//
//            Call call = client.newCall(request);
//            Response response = null;
//            try {
//                response = call.execute();
//                MutableArrayList<Pair<String, Double>> places = new MutableArrayList<>();
//
//                if (response.isSuccessful()) {
//
//                    String body = response.body().string();
//                    LogUtil.log(LoginTask.class.getSimpleName(), "Response: " + body);
//
//                    JSONObject data = new JSONObject(body);
//                    JSONArray results = data.getJSONArray("results");
//
//                    try {
//                        pageToken = data.getString("next_page_token");
//                        hasMoreData = true;
//                    } catch (JSONException e) {
//                        pageToken = null;
//                        hasMoreData = false;
//                    }
//
//                    JSONObject obj;
//                    for ( int i = 0; i < results.length(); i++ ) {
//                        obj = (JSONObject) results.get(i);
//                        JSONObject location = obj.getJSONObject("geometry").getJSONObject("location");
//                        double latitude = location.getDouble("lat");
//                        double longitude = location.getDouble("lng");
//                        places.add(new Pair<>(obj.getString("name"),
//                                LocationUtil.distanceBetweenLocations(this.location.latitude, this.location.longitude, latitude, longitude)));
//                    }
//
//                }
//
//                if ( !hasLocalPlaces ) {
//                    List<Pair<String, LatLng>> localPlaces = PlacesDao.getPlaces(location.latitude, location.longitude, 10);
//                    this.localPlaces = new MutableArrayList<Pair<String, Double>>();
//                    hasLocalPlaces = true;
//
//                    for (Pair<String, LatLng> p : localPlaces) {
//                        double dist = LocationUtil.distanceBetweenLocations(this.location.latitude, this.location.longitude, p.second.latitude, p.second.longitude);
//                        places.add(new Pair<>(p.first, dist));
//                        this.localPlaces.add(new Pair<>(p.first, dist));
//                    }
//
//                }
//
//
//                if ( localPlaces != null )
//                    localPlaces.sort(mSort);
//
//                places.sort(mSort);
//
//                return places;
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(MutableArrayList<Pair<String, Double>> places) {
//            super.onPostExecute(places);
//
//            if ( mAdapter == null ) {
//                NearbyPlacesDialog.this.places = new MutableArrayList<>();
//
//                if ( places != null )
//                    NearbyPlacesDialog.this.places.addAll(places);
//
//                mAdapter = new Adapter(getActivity(), R.layout.simple_list_item_1, NearbyPlacesDialog.this.places);
//                mListView.setAdapter(mAdapter);
//                mLoadingview.setVisibility(View.GONE);
//            } else {
//                NearbyPlacesDialog.this.places.addAll(places);
//                NearbyPlacesDialog.this.places.sort(mSort);
//                mAdapter.notifyDataSetChanged();
//                mListView.removeFooterView(mLoadingFooter);
//            }
//
//            loadingData = false;
//
//            if ( autoSelectFirstOption ) {
//                autoSelectFirstOption = false;
//                String place = null;
//
//                // check to see if the first custom place is within 1.5km of where we are at
//                if ( localPlaces != null && localPlaces.size() > 0 ) {
//                    Pair<String, Double> firstCustomPlace = localPlaces.get(0);
//
//                    if ( firstCustomPlace.second  < 2 )
//                        place = firstCustomPlace.first;
//                }
//
//                if ( place == null && NearbyPlacesDialog.this.places.size() > 0 ) {
//                    place = mAdapter.getItem(0).first;
//                }
//
//                if ( place != null && mListener != null )
//                    mListener.onPlaceSelected(place);
//            }
//        }
//    }

    private final Comparator<Pair<String, Double>> mSort = new Comparator<Pair<String, Double>>() {
        @Override
        public int compare(Pair<String, Double> lhs, Pair<String, Double> rhs) {
            return lhs.second.compareTo(rhs.second);
        }
    };

    private class Adapter extends ArrayAdapter<Pair<String, Double>> {

        public Adapter(Context context, int resource, List<Pair<String, Double>> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null)
                v = View.inflate(getContext(), R.layout.simple_list_item_1, null);

            TextView text = (TextView) v.findViewById(android.R.id.text1);
            text.setText(getItem(position).first);

            return v;
        }
    }

    public static final String getUrlWithKey(String endpoint) {
        StringBuilder sb = new StringBuilder();
        sb.append(endpoint);
        sb.append("?key=");
        sb.append(API_KEY);
        return sb.toString();
    }
}

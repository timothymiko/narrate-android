package com.datonicgroup.narrate.app.dataprovider.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.entryeditor.EditEntryActivity;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by timothymiko on 1/5/15.
 */
public class EntryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.log("Narrate", "onReceive()");

        Bundle b = intent.getExtras();
        if (intent.getAction().equals(Entry.ACTION_NEW_ENTRY)) {

            Intent i = new Intent(context, EditEntryActivity.class);
            i.setAction(Entry.ACTION_NEW_ENTRY);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (b != null)
                i.putExtras(b);
            context.startActivity(i);

        } else if (intent.getAction().equals(Entry.ACTION_SAVE_ENTRY)) {

            final Entry entry = new Entry();
            entry.creationDate = Calendar.getInstance();
            entry.modifiedDate = entry.creationDate.getTimeInMillis();
            entry.title = "";
            entry.text = "";
            entry.photos = new ArrayList<>();

            String data = b.getString(Entry.EXTRA_DATA, null);

            if (data != null) {

                try {
                    JSONObject json = new JSONObject(data);

                    String title = json.has(Entry.EXTRA_TITLE) ? json.getString(Entry.EXTRA_TITLE) : null;
                    String text = json.has(Entry.EXTRA_TEXT) ? json.getString(Entry.EXTRA_TEXT) : null;
                    long date = json.has(Entry.EXTRA_DATE_TIME) ? json.getLong(Entry.EXTRA_DATE_TIME) : 0;
                    String photo = json.has(Entry.EXTRA_PHOTO) ? json.getString(Entry.EXTRA_PHOTO) : null;
                    double latitude = json.has(Entry.EXTRA_LATITUDE) ? json.getDouble(Entry.EXTRA_LATITUDE) : 0;
                    double longitude = json.has(Entry.EXTRA_LONGITUDE) ? json.getDouble(Entry.EXTRA_LONGITUDE) : 0;
                    String placeName = json.has(Entry.EXTRA_PLACE_NAME) ? json.getString(Entry.EXTRA_PLACE_NAME) : null;
                    JSONArray tags = json.has(Entry.EXTRA_TAGS) ? json.getJSONArray(Entry.EXTRA_TAGS) : null;
                    boolean bookmark = json.has(Entry.EXTRA_BOOKMARK) ? json.getBoolean(Entry.EXTRA_BOOKMARK) : false;

                    if (title != null)
                        entry.title = title;

                    if (text != null)
                        entry.text = text;

                    if (date > 0) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(date * DateUtil.SECOND_IN_MILLISECONDS);
                        entry.creationDate = cal;
                    }

                    if (photo != null) {
                        // todo
                    }

                    if (latitude != 0 && longitude != 0) {
                        entry.latitude = latitude;
                        entry.longitude = longitude;

                        if (placeName != null) {
                            entry.placeName = placeName;
                        }
                    }

                    if (tags != null) {
                        try {
                            ArrayList<String> tagsArray = new ArrayList<>();
                            for (int i = 0; i < tags.length(); i++)
                                tagsArray.add(tags.getString(i));

                            entry.tags = tagsArray;
                        } catch (Exception e) {
                            e.printStackTrace();
                            entry.tags = new ArrayList<>();
                        }
                    }

                    entry.starred = bookmark;

                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            DataManager.getInstance().save(entry, true);
                        }
                    }.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else
                Log.e("Narrate", "Entry data is null.");
        }
    }
}

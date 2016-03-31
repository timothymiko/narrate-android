package com.datonicgroup.narrate.app.dataprovider.receivers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.datonicgroup.narrate.app.dataprovider.DataManager;
import com.datonicgroup.narrate.app.models.User;
import com.parse.ParseInstallation;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by timothymiko on 1/21/16.
 */
public class ParseGCMPushReceiver extends ParsePushBroadcastReceiver {

    @Override
    protected void onPushReceive(Context context, Intent intent) {
        Log.d("Narrate", "GCM Broadcast received.");
        /**
         *
         * A parse.com push contains a payload with the following key, value pairs:
         *
         *  - push_id --> An identifier for the push notification
         *  - data --> Any custom data that was sent with the push notification
         *  - from --> not sure what this means
         *  - time --> the time the push was sent at
         *  - channel --> the channel the push was sent through
         *  - collapse_key --> not sure what this means
         *
         */
        if (User.isAbleToSync()) {
            String data = intent.getStringExtra(KEY_PUSH_DATA);
            try {
                JSONObject payload = new JSONObject(data);
                String senderDeviceId = payload.getString("deviceId");
                String thisDeviceId = ParseInstallation.getCurrentInstallation().getInstallationId();
                if (senderDeviceId != null && !senderDeviceId.equals(thisDeviceId)) {
                    DataManager.getInstance().sync();
                }
            } catch (JSONException e) {
                Log.d("Narrate", "Error loading push notification payload into json object.");
                e.printStackTrace();
            }
        }
    }

}

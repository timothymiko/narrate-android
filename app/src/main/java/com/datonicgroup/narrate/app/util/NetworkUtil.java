package com.datonicgroup.narrate.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.datonicgroup.narrate.app.ui.GlobalApplication;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by timothymiko on 8/2/14.
 */
public class NetworkUtil {

    public static boolean hasInternet() {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) GlobalApplication.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            return false;
        }
        if (info.isRoaming()) {
            // here is the roaming option you can change it if you want to
            // disable internet while roaming, just return false
            return false;
        }

        return true;
    }

    public static boolean isOnWifi() {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) GlobalApplication.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (info == null) return false;
        return info.isConnected();
    }

    public static Map<String, String> getQueryMap(String query)
    {
        String[] params = null;
        try {
            params = URLDecoder.decode(query, "UTF-8").split("&");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        if ( params != null ) {
            Map<String, String> map = new HashMap<String, String>();
            for (String param : params) {
                if (param.split("=").length > 1) {
                    String name = param.split("=")[0];
                    String value = param.split("=")[1];
                    map.put(name, value);
                }
            }
            return map;
        }

        return null;
    }
}

package com.datonicgroup.narrate.app.models;

/**
 * Created by timothymiko on 12/30/14.
 */
public interface PreferenceListener {
    void onClick(String key);
    void onValueChanged(String key);
}

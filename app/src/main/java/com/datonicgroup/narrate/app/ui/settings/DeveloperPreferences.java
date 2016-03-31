package com.datonicgroup.narrate.app.ui.settings;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.models.Entry;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by timothymiko on 9/22/14.
 */
public class DeveloperPreferences extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    CheckBoxPreference mLogging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.developer_preferences);

        findPreference("disable").setOnPreferenceClickListener(this);

//        mLogging = (CheckBoxPreference) findPreference("logging_enabled");
//        mLogging.setChecked(GlobalApplication.getUser().loggingEnabled);

        if (BuildConfig.DEBUG) {
            Preference pref = new Preference(this);
            pref.setTitle("Prepare for screenshots");
            pref.setSummary("Clears all data and adds entries for screenshots");
            pref.setKey("screenshots");
            pref.setOnPreferenceClickListener(this);
            getPreferenceScreen().addPreference(pref);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch ( preference.getKey() ) {
            case "disable":
//                setResult(5);
//                finish();
//
//                GlobalApplication.getUser().developerModeEnabled = false;
//                GlobalApplication.getUser().loggingEnabled = false;
//                SettingsUtil.updateUser();
                return true;
            case "screenshots":
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setCancelable(false);
                mDialog.setTitle("Processing...");
                mDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        EntryHelper.deleteAllEntries();

                        Entry entry = new Entry();
                        entry.title = "New Job";
                        entry.text = "Tomorrow, I start my new job. I am starting as an Software Engineer Intern. I am extremely excited to start this opportunity and excel.";

                        entry.tags = new ArrayList<String>();
                        entry.tags.add("career");
                        entry.tags.add("success");

                        Calendar date = Calendar.getInstance();
                        date.set(Calendar.HOUR_OF_DAY, 15);
                        date.set(Calendar.MINUTE, 9);
                        entry.creationDate = date;

                        EntryHelper.saveEntry(entry);

                        entry = new Entry();
                        entry.title = "Positivity";
                        entry.text = "I am really trying to be more positive. I read an article recently about avoiding negative actions and thoughts. Simply thinking more positively can result in better moods, improved attitudes, and more.";

                        entry.tags = new ArrayList<String>();
                        entry.tags.add("happiness");

                        date = Calendar.getInstance();
                        date.set(Calendar.HOUR_OF_DAY, 12);
                        date.set(Calendar.MINUTE, 6);
                        entry.creationDate = date;

                        EntryHelper.saveEntry(entry);

                        entry = new Entry();
                        entry.title = "Spend More Time with Family";
                        entry.text = "Today, I visited with family. We talked for hours and caught up a lot on things. I really need to start spending more time with them.";

                        entry.tags = new ArrayList<String>();
                        entry.tags.add("happiness");
                        entry.tags.add("family");

                        date = Calendar.getInstance();
                        date.set(Calendar.DAY_OF_MONTH, 19);
                        date.set(Calendar.HOUR_OF_DAY, 19);
                        date.set(Calendar.MINUTE, 5);
                        entry.creationDate = date;

                        EntryHelper.saveEntry(entry);

                        entry = new Entry();
                        entry.title = "Divergent";
                        entry.text = "I started reading a new book today. It is called Divergent by Veronica Roth. I haven't been able to put the book down since I picked it up. It is just so good.";
                        entry.latitude = 1;
                        entry.longitude = 1;
                        entry.placeName = "Home";

                        entry.tags = new ArrayList<String>();
                        entry.tags.add("reading");
                        entry.tags.add("leisure");

                        date = Calendar.getInstance();
                        date.set(Calendar.DAY_OF_MONTH, 18);
                        date.set(Calendar.HOUR_OF_DAY, 3);
                        date.set(Calendar.MINUTE, 20);
                        entry.creationDate = date;
                        entry.starred = true;

                        EntryHelper.saveEntry(entry);

                        entry = new Entry();
                        entry.title = "";
                        entry.text = "I am doing really well with sticking to my new gym schedule. I am slowly going more frequently and staying for longer periods of time as well. I am really happy that things are coming around.";

                        entry.tags = new ArrayList<String>();
                        entry.tags.add("workouts");

                        date = Calendar.getInstance();
                        date.set(Calendar.DAY_OF_MONTH, 17);
                        date.set(Calendar.HOUR_OF_DAY, 23);
                        date.set(Calendar.MINUTE, 32);
                        entry.creationDate = date;

                        EntryHelper.saveEntry(entry);


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDialog.dismiss();
                            }
                        });
                    }
                }).start();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId() ) {
            case android.R.id.home:
                onBackPressed();
                overridePendingTransition(R.anim.ease_in_from_left, R.anim.slide_out_to_right);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "logging_enabled":
//                GlobalApplication.getUser().loggingEnabled = mLogging.isChecked();
//                SettingsUtil.updateUser();
//                LogUtil.log("DeveloeprPreferences", "Logging Enabled Changed To: " + GlobalApplication.getUser().loggingEnabled);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.ease_in_from_left, R.anim.slide_out_to_right);
    }
}

package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.widget.CompoundButton;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;

/**
 * Created by timothymiko on 1/12/15.
 */
public class DeveloperCard extends PreferenceCard {

    public interface DeveloperOptionsListener {
        void onDisableDevOptions();
    }

    private DeveloperOptionsListener mListener;

    private SwitchPreference mLoggingPref;

    public DeveloperCard(Context context, DeveloperOptionsListener listener) {
        super(context);
        this.mListener = listener;
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.developer_options);
        setSwitchEnabled(true);
        mTitle.setChecked(true);

        mLoggingPref = new SwitchPreference(getContext());
        mLoggingPref.setTitle(R.string.logging_summary);
        mLoggingPref.setOnCheckedChangedListener(this);
        mLoggingPref.setTag(0);
        mLoggingPref.setChecked(Settings.getLoggingEnabled());

        addView(mLoggingPref);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if ( buttonView.getId() == R.id.settings_title ) {
            Settings.setDeveloperModeEnabled(false);
            Settings.setLoggingEnabled(false);
            mListener.onDisableDevOptions();
            return;
        }

        switch ((Integer)buttonView.getTag()) {
            case 0:
                Settings.setLoggingEnabled(isChecked);
                break;
        }
    }
}

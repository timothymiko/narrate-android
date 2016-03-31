package com.datonicgroup.narrate.app.ui.settings;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.dataprovider.tasks.SaveReminderTask;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.ui.dialogs.ReminderDialog;
import com.datonicgroup.narrate.app.ui.reminders.RemindersActivity;

/**
 * Created by timothymiko on 1/6/15.
 */
public class RemindersCard extends PreferenceCard implements View.OnClickListener, ReminderDialog.OnSaveListener {

    private ButtonPreference mAddNewPref;
    private ButtonPreference mViewPref;
    private FragmentActivity mActivity;

    public RemindersCard(FragmentActivity activity) {
        super(activity);
        this.mActivity = activity;
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.reminders);
        setSwitchEnabled(true);

        mAddNewPref = new ButtonPreference(getContext());
        mAddNewPref.setTag(0);
        mAddNewPref.setTitle(R.string.reminders_add_new);
        mAddNewPref.setButtonText(getContext().getString(R.string.add_uc));
        mAddNewPref.setOnClickListener(this);
        addView(mAddNewPref);

        mViewPref = new ButtonPreference(getContext());
        mViewPref.setTag(1);
        mViewPref.setTitle(R.string.reminders_view_existing);
        mViewPref.setButtonText(getResources().getString(R.string.view));
        mViewPref.setOnClickListener(this);
        addView(mViewPref);

        mTitle.setChecked(Settings.getRemindersEnabled());
    }

    @Override
    public void onClick(View v) {
        int key = (int) v.getTag();
        switch (key) {
            case 0:
                ReminderDialog dialog = new ReminderDialog();
                dialog.setSaveListener(this);
                dialog.show(mActivity.getSupportFragmentManager(), "ReminderDialog");
                break;
            case 1:
                Intent i = new Intent(getContext(), RemindersActivity.class);
                getContext().startActivity(i);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        super.onCheckedChanged(buttonView, isChecked);
        switch (buttonView.getId()) {
            case R.id.settings_title:
                Settings.setRemindersEnabled(isChecked);
                break;
        }
    }

    @Override
    public void onSave(Reminder r) {
        new SaveReminderTask().execute(r);
    }
}

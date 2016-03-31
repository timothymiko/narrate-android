package com.datonicgroup.narrate.app.ui.settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CompoundButton;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.dialogs.PasscodeLockTimeDialog;
import com.datonicgroup.narrate.app.ui.dialogs.PasscodeSetDialog;
import com.datonicgroup.narrate.app.ui.passcode.AbstractAppLock;
import com.datonicgroup.narrate.app.ui.passcode.AppLockManager;
import com.datonicgroup.narrate.app.ui.passcode.PasscodeManagePasswordActivity;

/**
 * Created by timothymiko on 1/9/15.
 */
public class PasscodeLockCard extends PreferenceCard {

    public static final int ENABLE_PASSLOCK = 200;
    public static final int DISABLE_PASSLOCK = 201;
    public static final int CHANGE_PASSWORD = 202;

    private ButtonPreference mSetPasscode;
    private ButtonPreference mSetLockTime;

    private PasscodeLockTimeDialog mTimeoutDialog;
    private PasscodeSetDialog mSetDialog;

    private FragmentActivity mActivity;

    private boolean mEnablingPasscode;

    public PasscodeLockCard(FragmentActivity activity) {
        super(activity);
        this.mActivity = activity;
    }

    @Override
    protected void init() {
        super.init();

        mTimeoutDialog = new PasscodeLockTimeDialog();
        mTimeoutDialog.setOnClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateIntervalText();
            }
        });

        mSetDialog = new PasscodeSetDialog();
        mSetDialog.setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ( mEnablingPasscode ) {
                    mEnablingPasscode = false;
                    Settings.setPasscodeLockEnabled(true);
                }
            }
        });
        mSetDialog.setNegativeButton(R.string.cancel_uc, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ( mEnablingPasscode ) {
                    mEnablingPasscode = false;
                    Settings.setPasscodeLockEnabled(false);
                    mTitle.setChecked(false);
                }

            }
        });

        setTitle(R.string.passcode_lock_title);
        setSwitchEnabled(true);

        mSetPasscode = new ButtonPreference(getContext());
        mSetLockTime = new ButtonPreference(getContext());

        mSetPasscode.setTitle(R.string.passcode_set);
        mSetLockTime.setTitle(R.string.passcode_set_lock_time);

        mSetPasscode.setButtonText(R.string.set);
        updateIntervalText();

        mSetPasscode.setTag(0);
        mSetLockTime.setTag(1);

        mSetPasscode.setOnClickListener(this);
        mSetLockTime.setOnClickListener(this);

        addView(mSetPasscode);
        addView(mSetLockTime);

        mTitle.setChecked(Settings.getPasscodeLockEnabled());
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch ((Integer)v.getTag()) {
            case 0:
                Intent i = new Intent(mActivity, PasscodeManagePasswordActivity.class);
                i.putExtra("type", CHANGE_PASSWORD);
                i.putExtra("message", getResources().getString(R.string.passcode_enter_old_passcode));
                mActivity.startActivityForResult(i, CHANGE_PASSWORD);
                break;
            case 1:
                mTimeoutDialog.show(mActivity.getSupportFragmentManager(), "PasscodeTimeoutDialog");
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_title:
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ( mActivity != null ) {
                            int type = AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ? DISABLE_PASSLOCK : ENABLE_PASSLOCK;
                            Intent i = new Intent(mActivity, PasscodeManagePasswordActivity.class);
                            i.putExtra("type", type);
                            mActivity.startActivityForResult(i, type);
                        }
                    }
                }, 300);
                return;
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        switch (requestCode) {
            case ENABLE_PASSLOCK:
            case DISABLE_PASSLOCK:
                final boolean enabled = AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked();
                Settings.setPasscodeLockEnabled(enabled);
                mTitle.setChecked(enabled);

                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if ( enabled ) {
                            if ( !mExpanded )
                                animateOpen();
                        } else {
                            if ( mExpanded )
                                animateClosed();
                        }
                    }
                }, 300);
                break;
            case CHANGE_PASSWORD:
                break;
        }
    }

    private void updateIntervalText() {
        int timeout = Settings.getPasscodeLockTimeout();
        int index = -1;
        for ( int i = 0; i < mTimeoutDialog.values.length; i++ ) {
            if ( mTimeoutDialog.values[i] == timeout ) {
                mSetLockTime.setButtonText(mTimeoutDialog.entries[i]);
                index = i;
                break;
            }
        }

        if ( index == -1 ) {

            int default_index = 2;
            int val = PasscodeLockTimeDialog.values[default_index];
            mSetLockTime.setButtonText(mTimeoutDialog.entries[default_index]);
            Settings.setPasscodeLockTimeout(val);

            AbstractAppLock.DEFAULT_TIMEOUT = val;
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putInt("passcode_timeout_index", default_index);
            editor.apply();
        }
    }
}

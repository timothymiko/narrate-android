package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.passcode.AbstractAppLock;
import com.datonicgroup.narrate.app.util.DateUtil;

/**
 * Created by timothymiko on 1/9/15.
 */
public class PasscodeLockTimeDialog extends MaterialDialogFragment {

    public static String[] entries = {
            5 + " " + GlobalApplication.getAppContext().getString(R.string.seconds),
            1 + " " + GlobalApplication.getAppContext().getString(R.string.minute),
            5 + " " + GlobalApplication.getAppContext().getString(R.string.minutes).toLowerCase(),
            15 + " " + GlobalApplication.getAppContext().getString(R.string.minutes).toLowerCase(),
            30 + " " + GlobalApplication.getAppContext().getString(R.string.minutes).toLowerCase()
    };

    public static int[] values = {
            5,
            1 * DateUtil.MINUTE_IN_SECONDS,
            5 * DateUtil.MINUTE_IN_SECONDS,
            15 * DateUtil.MINUTE_IN_SECONDS,
            30 * DateUtil.MINUTE_IN_SECONDS,
    };
    private DialogInterface.OnClickListener mListener;
    private RadioGroup mRadioGroup;

    private final int[] ids = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five};

    private RadioButton one;
    private RadioButton two;
    private RadioButton three;
    private RadioButton four;
    private RadioButton five;

    public PasscodeLockTimeDialog() {
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {

            setTitle(R.string.passcode_set_lock_time);
            setContentView(R.layout.dialog_passcode_lock_time);
            setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    int key = mRadioGroup.getCheckedRadioButtonId();
                    int val = -1;
                    int index = 0;
                    for (index = 0; index < ids.length; index++) {
                        if (key == ids[index]) {
                            val = values[index];
                            Settings.setPasscodeLockTimeout(val);
                            break;
                        }
                    }

                    AbstractAppLock.DEFAULT_TIMEOUT = val;
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putInt("passcode_timeout_index", index);
                    editor.apply();

                    if (mListener != null)
                        mListener.onClick(dialog, which);
                }
            });
            setNegativeButton(R.string.cancel_uc, null);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mRadioGroup = (RadioGroup) dialog.findViewById(R.id.pickerRadioGroup);
            one = (RadioButton) dialog.findViewById(R.id.one);
            two = (RadioButton) dialog.findViewById(R.id.two);
            three = (RadioButton) dialog.findViewById(R.id.three);
            four = (RadioButton) dialog.findViewById(R.id.four);
            five = (RadioButton) dialog.findViewById(R.id.five);

            one.setText(entries[0]);
            two.setText(entries[1]);
            three.setText(entries[2]);
            four.setText(entries[3]);
            five.setText(entries[4]);

            int timeout = Settings.getPasscodeLockTimeout();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == timeout) {
                    mRadioGroup.check(ids[i]);
                    break;
                }
            }

            return dialog;

        } else
            return null;
    }
}

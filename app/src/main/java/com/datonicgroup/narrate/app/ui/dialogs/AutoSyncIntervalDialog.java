package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.DateUtil;

/**
 * Created by timothymiko on 1/9/15.
 */
public class AutoSyncIntervalDialog extends MaterialDialogFragment {

    public String[] entries = {
            GlobalApplication.getAppContext().getString(R.string.none),
            1 + " " + GlobalApplication.getAppContext().getString(R.string.hour),
            3 + " " + GlobalApplication.getAppContext().getString(R.string.hours),
            6 + " " + GlobalApplication.getAppContext().getString(R.string.hours),
            12 + " " + GlobalApplication.getAppContext().getString(R.string.hours)
    };

    private int[] values = {
            -1,
            1 * DateUtil.HOUR_IN_SECONDS,
            3 * DateUtil.HOUR_IN_SECONDS,
            6 * DateUtil.HOUR_IN_SECONDS,
            12 * DateUtil.HOUR_IN_SECONDS,
    };
    private DialogInterface.OnClickListener mListener;
    private RadioGroup mRadioGroup;

    private final int[] ids = {R.id.one, R.id.two, R.id.three, R.id.four, R.id.five};

    private RadioButton one;
    private RadioButton two;
    private RadioButton three;
    private RadioButton four;
    private RadioButton five;

    public AutoSyncIntervalDialog() {
    }

    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {

            setTitle(R.string.sync_interval);
            setContentView(R.layout.dialog_number_picker);
            setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    int key = mRadioGroup.getCheckedRadioButtonId();
                    for (int i = 0; i < ids.length; i++) {
                        if (key == ids[i]) {
                            Settings.setAutoSyncInterval(values[i]);
                        }
                    }

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

            int interval = Settings.getAutoSyncInterval();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == interval) {
                    mRadioGroup.check(ids[i]);
                    break;
                }
            }

            return dialog;

        } else
            return null;
    }
}

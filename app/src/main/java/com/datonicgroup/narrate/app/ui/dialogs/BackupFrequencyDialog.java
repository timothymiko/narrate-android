package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.RadioGroup;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.LocalBackupManager;
import com.datonicgroup.narrate.app.dataprovider.Settings;

/**
 * Created by timothymiko on 1/9/15.
 */
public class BackupFrequencyDialog extends MaterialDialogFragment {

    private DialogInterface.OnClickListener mListener;
    private RadioGroup radioGroup;

    public BackupFrequencyDialog() {
    }

    public void setListener(DialogInterface.OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.local_backup_frequency);
            setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    int id = radioGroup.getCheckedRadioButtonId();

                    switch (id) {
                        case R.id.daily:
                            Settings.setLocalBackupFrequency(0);
                            break;
                        case R.id.weekly:
                            Settings.setLocalBackupFrequency(1);
                            break;
                        case R.id.monthly:
                            Settings.setLocalBackupFrequency(2);
                            break;
                    }

                    if ( Settings.getLocalBackupsEnabled() ) {
                        LocalBackupManager.setEnabled(false);
                        LocalBackupManager.setEnabled(true);
                    }

                    if ( mListener != null )
                        mListener.onClick(dialog, which);
                }
            });
            setNegativeButton(R.string.cancel_uc, null);
            setContentView(R.layout.dialog_backup_frequency);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            radioGroup = (RadioGroup) dialog.findViewById(R.id.radio_group);

            int frequency = Settings.getLocalBackupFrequency();
            if ( frequency > -1 ) {
                switch (frequency) {
                    case 0:
                        radioGroup.check(R.id.daily);
                        break;
                    case 1:
                        radioGroup.check(R.id.weekly);
                        break;
                    case 2:
                        radioGroup.check(R.id.monthly);
                        break;
                }
            }

            return dialog;

        } else
            return null;
    }
}

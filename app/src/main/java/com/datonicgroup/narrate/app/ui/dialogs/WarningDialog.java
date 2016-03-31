package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 10/15/14.
 */
public class WarningDialog extends MaterialDialogFragment implements View.OnClickListener {

    private DialogInterface.OnClickListener pos;
    private DialogInterface.OnClickListener neg;

    public WarningDialog() {
    }

    public void setPositiveListener(DialogInterface.OnClickListener pos) {
        this.pos = pos;
    }

    public void setNegativeListener(DialogInterface.OnClickListener neg) {
        this.neg = neg;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.warning);
            setContentView(R.layout.dialog_restore_dialog);
            setPositiveButton(R.string.continue_uc, pos);
            setNegativeButton(R.string.cancel_uc, neg);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);;

            TextView text = (TextView) dialog.findViewById(R.id.text);
            text.setText(R.string.multiple_sync_services_warning);

            return dialog;
        } else
            return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.negative:
                dismiss();

                if ( pos != null )
                    neg.onClick(null, 0);
                break;
            case R.id.positive:
                dismiss();

                if ( pos != null )
                    pos.onClick(null, 1);
                break;
        }
    }
}

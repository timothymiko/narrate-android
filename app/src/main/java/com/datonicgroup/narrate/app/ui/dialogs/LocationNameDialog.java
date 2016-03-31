package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 9/26/14.
 */
public class LocationNameDialog extends MaterialDialogFragment implements DialogInterface.OnClickListener {

    public interface SaveListener {
        void onSave(String text);
    }

    private EditText mEditText;
    private SaveListener sListener;
    private String name;

    public void setSaveListener(SaveListener listener) {
        this.sListener = listener;
    }

    public void setName(String s) {
        this.name = s;

        if ( mEditText != null )
            mEditText.setText(name);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.custom_location);
            setPositiveButton(R.string.save_uc, this);
            setNegativeButton(R.string.cancel_uc, null);
            setContentView(R.layout.location_name_dialog);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mEditText = (EditText) dialog.findViewById(R.id.edit_text);

            if ( name != null )
                mEditText.setText(name);

            return dialog;

        } else
            return null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if ( sListener != null && mEditText != null ) {
            sListener.onSave(mEditText.getText().toString());
        }
    }
}

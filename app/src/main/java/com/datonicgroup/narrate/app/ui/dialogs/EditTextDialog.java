package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.EditText;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 1/9/15.
 */
public class EditTextDialog extends MaterialDialogFragment {

    private EditText mEditText;
    private int mInputType = -1;
    private String text;

    public EditTextDialog() {
    }

    public EditText getEditText() {
        return mEditText;
    }

    public void setInputType(int inputType) {
        this.mInputType = inputType;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setContentView(R.layout.dialog_edit_text);
            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mEditText = (EditText) dialog.findViewById(R.id.edit_text);

            if ( mInputType != -1 )
                mEditText.setInputType(mInputType);

            if ( text != null )
                mEditText.setText(text);

            return dialog;

        } else
            return null;
    }
}

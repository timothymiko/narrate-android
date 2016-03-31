package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 1/6/15.
 */
public class DeleteConfirmationDialog extends MaterialDialogFragment {

    DialogInterface.OnClickListener listener;

    public static DeleteConfirmationDialog newInstance(DialogInterface.OnClickListener deleteListener) {
        DeleteConfirmationDialog d = new DeleteConfirmationDialog();
        d.listener = deleteListener;
        return d;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.delete_title);
            setContentView(R.layout.dialog_delete_note);
            setPositiveButton(R.string.delete_title, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if ( listener != null )
                        listener.onClick(dialog, which);
                }
            });
            setNegativeButton(android.R.string.cancel, null);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            return dialog;

        } else
            return null;
    }
}

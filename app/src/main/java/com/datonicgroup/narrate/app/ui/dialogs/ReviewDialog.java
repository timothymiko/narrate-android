package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 9/4/14.
 */
public class ReviewDialog extends MaterialDialogFragment {

    public ReviewDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.review_t);
            setMessage(R.string.review_body);
            setNegativeButton(R.string.no_thanks_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            setPositiveButton(R.string.play_store_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String url = "market://details?id=com.datonicgroup.narrate.app";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
            });

            return super.onCreateDialog(savedInstanceState);

        } else
            return null;
    }
}

package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;

import java.util.List;

/**
 * Created by timothymiko on 1/6/15.
 */
public class OptionsDialog extends MaterialDialogFragment {

    private Context mContext;
    private List<String> mItems;
    private List<Integer> mIcons;
    private AdapterView.OnItemClickListener mClickListener;

    private LinearLayout mRoot;

    public static OptionsDialog newInstance(List<String> items, List<Integer> icons, AdapterView.OnItemClickListener listener) {
        OptionsDialog dialog = new OptionsDialog();
        dialog.mItems = items;
        dialog.mIcons = icons;
        dialog.mClickListener = listener;
        return dialog;
    }

    public void notifyDataSetChanged() {
        if ( getActivity() != null && mRoot != null ) {
            mRoot.removeAllViews();
            inflateViews();
        }
    }

    private void inflateViews() {
        if ( mItems != null ) {
            for (int i = 0; i < mItems.size(); i++) {
                View v = View.inflate(mContext, R.layout.options_item, null);

                ImageView icon = (ImageView) v.findViewById(R.id.icon);
                TextView text = (TextView) v.findViewById(R.id.text);

                icon.setImageResource(mIcons.get(i));
                text.setText(mItems.get(i));

                final int pos = i;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        dismiss();

                        if ( mClickListener != null ) {
                            mClickListener.onItemClick(null, v, pos, 0);
                        }
                    }
                });

                mRoot.addView(v);
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            mContext = getActivity();

            setContentView(R.layout.dialog_options);
            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mRoot = (LinearLayout) dialog.findViewById(R.id.root);
            dialog.findViewById(R.id.dialog_buttons_layout).setVisibility(View.GONE);

            inflateViews();

            return dialog;

        } else
            return null;
    }

}

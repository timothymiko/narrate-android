package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 1/9/15.
 */
public class PasscodeSetDialog extends MaterialDialogFragment {

    public static final int SETTING_PASSCODE = 1;
    public static final int CONFIRMING_PASSCODE = 2;
    public static final int CHANGING_PASSCODE = 3;

    private int state = SETTING_PASSCODE;

    private EditText one;
    private EditText two;
    private EditText three;
    private EditText four;

    private boolean verified;
    private String unverifiedPasscode;

    public PasscodeSetDialog() {
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.passcode_set);
            setPositiveButton(R.string.save_uc, null);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            dialog.findViewById(R.id.dialog_button_negative).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.dialog_button_positive).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if ( state == SETTING_PASSCODE && isCancelable() )
                        dismiss();
                    else
                    if ( state == CONFIRMING_PASSCODE && verified)
                        dismiss();
                }
            });

            LinearLayout root = (LinearLayout) dialog.findViewById(R.id.root);
            one = (EditText) root.getChildAt(0);
            two = (EditText) root.getChildAt(1);
            three = (EditText) root.getChildAt(2);
            four = (EditText) root.getChildAt(3);

            one.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ( s.length() == 1 )
                        two.requestFocus();
                }
            });

            two.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ( s.length() == 1 )
                        three.requestFocus();
                }
            });

            three.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ( s.length() == 1 )
                        four.requestFocus();
                }
            });

            four.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if ( s.length() == 1 ) {
                        if ( state == SETTING_PASSCODE ) {

                            unverifiedPasscode = one.getText().toString() + two.getText().toString() + three.getText().toString() + four.getText().toString();

                            one.setText(null);
                            two.setText(null);
                            three.setText(null);
                            four.setText(null);

                            one.requestFocus();
                            textViewTitle.setText(R.string.passcode_confirm);

                        } else if ( state == CONFIRMING_PASSCODE ) {
                            String passcode = one.getText().toString() + two.getText().toString() + three.getText().toString() + four.getText().toString();

                            verified = unverifiedPasscode.equals(passcode);
                            if ( verified ) {
                                // passcode confirmed and verified
                            } else {
                                // passcode not verified
                            }
                        }
                    }
                }
            });

            return dialog;

        } else
            return null;
    }
}

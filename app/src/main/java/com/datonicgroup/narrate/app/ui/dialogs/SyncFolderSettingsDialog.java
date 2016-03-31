package com.datonicgroup.narrate.app.ui.dialogs;


import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.SyncFolderType;

import java.util.Locale;

/**
 * Created by timothymiko on 7/10/14.
 */
public class SyncFolderSettingsDialog extends MaterialDialogFragment {

    public interface Callbacks {
        void onCancel();

        void onPathSaved(SyncFolderType type, String path);
    }

    private EditText mCustomPath;
    private RadioGroup mRadioGroup;

    private int mSelectedOption;

    private Callbacks callback;

    private SyncFolderType type = SyncFolderType.DEFAULT;
    private String path = "/apps/Narrate";

    public SyncFolderSettingsDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.dialog_title);
            setPositiveButton(R.string.save_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    boolean validPath = true;

                    switch (type) {
                        case DEFAULT:
                            path = "/apps/narrate";
                            break;
                        case DAYONE:
                            path = "/apps/day one/journal.dayone";
                            break;
                        case CUSTOM:
                            if (mCustomPath.getText() != null && mCustomPath.getText().length() > 0) {
                                path = mCustomPath.getText().toString();

                                if (path != null) {
                                    while (path.charAt(path.length() - 1) == '/')
                                        path = path.substring(0, path.length() - 1);
                                    path = path.toLowerCase(Locale.getDefault());
                                }

                            } else {
                                Toast.makeText(getActivity(), "Error: Invalid folder path.", Toast.LENGTH_LONG).show();
                                callback.onCancel();
                                validPath = false;
                            }
                            break;
                    }

                    if (validPath) {
                        Settings.setDropboxSyncFolder(path);
                        Settings.setDropboxSyncFolderType(type);
                        Settings.setDropboxSyncDayOne(type == SyncFolderType.DAYONE || path.toLowerCase().contains("journal.dayone"));

                        if (callback != null)
                            callback.onPathSaved(type, path);
                    }
                }
            });
            setNegativeButton(R.string.cancel_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    mRadioGroup.check(mSelectedOption);

                    if (callback != null)
                        callback.onCancel();
                }
            });
            setContentView(R.layout.sync_folder_chooser_dialog);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            mRadioGroup = (RadioGroup) dialog.findViewById(R.id.folderRadioGroup);
            RadioButton mDefaultButton = (RadioButton) dialog.findViewById(R.id.default_folder);
            RadioButton mDayOneButton = (RadioButton) dialog.findViewById(R.id.day_one_folder);
            RadioButton mCustomButton = (RadioButton) dialog.findViewById(R.id.custom_folder);
            mCustomPath = (EditText) dialog.findViewById(R.id.custom_folder_text);
            dialog.findViewById(R.id.dialog_button_negative).setVisibility(View.VISIBLE);

            type = type.values()[Settings.getDropboxSyncFolderType()];

            String folder = Settings.getDropboxSyncFolder();
            if ( folder != null )
                path = Settings.getDropboxSyncFolder();

            switch (type) {
                case DEFAULT:
                    mSelectedOption = R.id.default_folder;
                    mDefaultButton.setChecked(true);
                    break;
                case DAYONE:
                    mSelectedOption = R.id.day_one_folder;
                    mDayOneButton.setChecked(true);
                    break;
                case CUSTOM:
                    mSelectedOption = R.id.custom_folder;
                    mCustomButton.setChecked(true);
                    mCustomPath.setText(path);
                    break;
            }

            mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.default_folder:
                            mCustomPath.setVisibility(View.GONE);
                            type = SyncFolderType.DEFAULT;
                            break;
                        case R.id.day_one_folder:
                            mCustomPath.setVisibility(View.GONE);
                            type = SyncFolderType.DAYONE;
                            break;
                        case R.id.custom_folder:
                            mCustomPath.setVisibility(View.VISIBLE);
                            type = SyncFolderType.CUSTOM;
                            break;
                    }
                }
            });

            mDefaultButton.setText(Html.fromHtml(getResources().getString(R.string.default_folder)));
            mDayOneButton.setText(Html.fromHtml(getResources().getString(R.string.day_one_folder)));

            if (type == SyncFolderType.CUSTOM)
                mCustomPath.setVisibility(View.VISIBLE);

            return dialog;

        } else
            return null;
    }

    public void setCallback(Callbacks callback) {
        this.callback = callback;
    }

    public String getPath() {
        return path;
    }

    public SyncFolderType getType() {
        return type;
    }
}

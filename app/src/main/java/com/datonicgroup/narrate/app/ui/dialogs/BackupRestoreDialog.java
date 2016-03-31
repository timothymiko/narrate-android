package com.datonicgroup.narrate.app.ui.dialogs;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.LocalBackupManager;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by timothymiko on 12/12/14.
 */
public class BackupRestoreDialog extends MaterialDialogFragment {

    private LinearLayout mList;
    private Adapter mAdapter;
    private Handler mHandler;

    public BackupRestoreDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.tap_to_restore);
            setContentView(R.layout.backup_restore_dialog);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);
            dialog.findViewById(R.id.dialog_buttons_layout).setVisibility(View.GONE);

            final View loader = dialog.findViewById(R.id.loading_indicator);
            mList = (LinearLayout) dialog.findViewById(R.id.list);
            final TextView noBackupsTextView = (TextView) dialog.findViewById(R.id.no_backups_found);
            mHandler = new Handler(Looper.getMainLooper());

            new Thread() {
                @Override
                public void run() {
                    super.run();
                    // get the data
                    final File[] backups = LocalBackupManager.getBackups();

                    if ( backups == null || backups.length == 0 ) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                noBackupsTextView.setVisibility(View.VISIBLE);
                                loader.animate().alpha(0f).start();
                                noBackupsTextView.animate().alpha(1f).start();
                            }
                        });
                        return;
                    }

                    final List<File> backupList = new ArrayList<File>();
                    for (int i = 0; i < backups.length; i++)
                        backupList.add(backups[i]);

                    Collections.sort(backupList, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    });

                    final String[] titles = new String[backupList.size()];

                    for ( int i = 0; i < backupList.size(); i++ ) {
                        Date date = null;
                        for ( int j = 0; j < backupList.get(i).getName().length(); j++ ) {
                            if ( backupList.get(i).getName().charAt(j) == '-' ) {
                                try {
                                    date = LocalBackupManager.DATE_FORMAT.parse(backupList.get(i).getName().substring(j+1));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    LogUtil.e("Narrate", "Failed to parse Narrate backup file.");
                                    date = null;
                                }
                                break;
                            }
                        }

                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        if ( date != null ) {
                            titles[i] = df.format(date);
                        }
                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter = new Adapter(getActivity(), R.layout.simple_list_item_1, titles);
                            mAdapter.setItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                                    setCancelable(false);
                                    textViewTitle.setText(R.string.local_backup_restoring);
                                    loader.animate().alpha(1f).start();
                                    mList.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                                        @Override
                                        public void onAnimationStart(Animator animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            mList.setVisibility(View.INVISIBLE);
                                        }

                                        @Override
                                        public void onAnimationCancel(Animator animation) {

                                        }

                                        @Override
                                        public void onAnimationRepeat(Animator animation) {

                                        }
                                    }).start();
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            super.run();
                                            LocalBackupManager.restore(backupList.get(position));
                                            dismiss();
                                        }
                                    }.start();
                                }
                            });
                            loader.animate().alpha(0f).start();
                            updateListViews();
                        }
                    });
                }
            }.start();

            return dialog;

        } else
             return null;
    }

    private void updateListViews() {
        mList.removeAllViews();

        for ( int i = 0; i < mAdapter.getCount(); i++ ) {
            View v = mAdapter.getView(i, null, null);

            if ( v != null )
                mList.addView(v);
        }
    }

    private class Adapter extends ArrayAdapter<String> {

        private AdapterView.OnItemClickListener itemClickListener;

        public Adapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        public void setItemClickListener(AdapterView.OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if ( getItem(position) != null ) {
                View v = super.getView(position, convertView, parent);

                v.setClickable(true);
                v.setBackgroundResource(R.drawable.default_selector);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (itemClickListener != null)
                            itemClickListener.onItemClick(null, v, position, 0);
                    }
                });

                return v;
            } else
                return null;
        }
    }
}

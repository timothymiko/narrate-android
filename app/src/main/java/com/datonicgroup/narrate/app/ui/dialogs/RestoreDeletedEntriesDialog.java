package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.providers.EntryHelper;
import com.datonicgroup.narrate.app.dataprovider.providers.PlacesDao;
import com.datonicgroup.narrate.app.dataprovider.providers.TagsDao;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncHelper;
import com.datonicgroup.narrate.app.dataprovider.sync.SyncInfoManager;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.models.SyncStatus;
import com.datonicgroup.narrate.app.models.User;
import com.datonicgroup.narrate.app.ui.GlobalApplication;

import java.util.List;

/**
 * Created by timothymiko on 1/22/16.
 */
public class RestoreDeletedEntriesDialog extends MaterialDialogFragment {

    public static RestoreDeletedEntriesDialog newInstance() {
        return new RestoreDeletedEntriesDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.restore_deleted_entries);
            setContentView(R.layout.dialog_restore_dialog);
            setPositiveButton(R.string.continue_uc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.restoring_deleted_entries), Toast.LENGTH_SHORT);
                    toast.show();
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();

                            // Stop syncing while we update the local data
                            SyncHelper.cancelPendingActiveSync(User.getAccount());

                            final List<Entry> deleted = EntryHelper.getDeletedEntries();

                            for ( int i = 0; i < deleted.size(); i++ ) {
                                Entry entry = deleted.get(i);
                                entry.isDeleted = false;
                                entry.deletionDate = 0;

                                SyncInfoManager.setStatus(entry, SyncStatus.UPLOAD);

                                // add this to prevent a database change notification until the last entry is saved
                                EntryHelper.mCallerIsSyncAdapter = i < deleted.size()-1;
                                EntryHelper.saveEntry(entry);

                                if ( entry.hasLocation && entry.placeName != null )
                                    PlacesDao.storePlace(entry.placeName, entry.latitude, entry.longitude);

                                if ( entry.tags != null && entry.tags.size() > 0 ) {
                                    for ( String t : entry.tags )
                                        TagsDao.storeTag(t);
                                }
                            }

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    toast.cancel();
                                    Toast.makeText(GlobalApplication.getAppContext(), GlobalApplication.getAppContext().getString(R.string.num_entries_restored, deleted.size()), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }.start();
                }
            });
            setNegativeButton(R.string.cancel_uc, null);

            return super.onCreateDialog(savedInstanceState);
        } else {
            return null;
        }
    }
}

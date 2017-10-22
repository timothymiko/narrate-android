package com.datonicgroup.narrate.app.dataprovider.sync;

import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.SyncService;

/**
 * Created by timothymiko on 10/27/14.
 */
public class DropboxSyncService extends SimpleAbsSyncService {

    DropboxSyncService(DropboxFileSync fs) {
        super(SyncService.Dropbox, fs);
    }

    DropboxSyncService() {
        this(new DropboxFileSync(Settings.getDropboxSyncToken()));
    }

}

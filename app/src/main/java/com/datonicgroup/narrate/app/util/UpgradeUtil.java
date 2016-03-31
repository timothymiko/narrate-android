package com.datonicgroup.narrate.app.util;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.dataprovider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by timothymiko on 7/10/14.
 */
public class UpgradeUtil {

    /**
     * Array that corresponds to whether or not the corresponding version codes, starting with
     * 1 and positively increasing by one, need to run any sort of upgrade or code (i.e. to copy
     * files, rename folders, etc.)
     */

    private static HashSet<Integer> VersionNeedsUpgrade = new HashSet<Integer>() {{
        add(1);
        add(3);
        add(16);
        add(25);
        add(30);
        add(33);
        add(38);
        add(42);
        add(52);
    }};

    /**
     * Used to determine if the user has just updated to a new version of Narrate AND the app needs
     * to do some work (aka copy files, rename folders, etc.) upon upgrading.
     *
     * @return true if app needs to run {@link com.datonicgroup.narrate.app.ui.dialogs.UpgradeDialog}
     */
    public static boolean doesUserNeedUpgrading() {
        boolean needsUpgrade = false;
        int versionCode = Settings.getAppVersion();

        if ( versionCode == BuildConfig.VERSION_CODE )
            return false;

        while ( versionCode <= BuildConfig.VERSION_CODE ) {

            needsUpgrade = needsUpgrade || VersionNeedsUpgrade.contains(versionCode);
            versionCode++;

            if ( needsUpgrade )
                break;
        }

        return needsUpgrade;
    }
}


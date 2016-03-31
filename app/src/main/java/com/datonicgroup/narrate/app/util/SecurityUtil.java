package com.datonicgroup.narrate.app.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.datonicgroup.narrate.app.ui.GlobalApplication;

/**
 * Created by timothymiko on 11/6/14.
 */
public class SecurityUtil {

    interface SecurityCheck {
        boolean run();
    }

    private static SecurityCheck[] checks = new SecurityCheck[]{
            new SecurityCheck() { public boolean run() { return checkInstalledByGooglePlay(); } },
            new SecurityCheck() { public boolean run() { return checkSignature(); } },
            new SecurityCheck() { public boolean run() { return checkDebuggable(); } },
            new SecurityCheck() { public boolean run() { return checkEmulator(); } }
    };

    public static void performChecks() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (SecurityCheck check : checks)
                    if (check.run()) android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

    private static boolean checkInstalledByGooglePlay() {
        Log.d("SecurityUtil", "checkInstalledByGooglePlay()");
        Context ctx = GlobalApplication.getAppContext();
        return !ctx.getPackageManager().getInstallerPackageName(ctx.getPackageName()).equals("com.android.vending");
    }

    private static boolean checkSignature() {
        Log.d("SecurityUtil", "checkSignature()");

        return false;
    }

    private static boolean checkDebuggable() {
        Log.d("SecurityUtil", "checkDebuggable()");
        return (GlobalApplication.getAppContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private static boolean checkEmulator() {
        Log.d("SecurityUtil", "checkEmulator()");
        Context ctx = GlobalApplication.getAppContext();
        try {
            boolean goldfish = System.getProperty("ro.hardware").contains("goldfish");
            boolean emu = System.getProperty("ro.kernel.qemu").length() > 0;
            boolean sdk = System.getProperty("ro.product.model").equals("sdk");

            if (emu || goldfish || sdk) {
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }
}

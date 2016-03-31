package com.datonicgroup.narrate.app.util;

import android.util.Log;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.dataprovider.Settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by timothymiko on 7/13/14.
 *
 * Wrapper class for logging. Makes it easier to turn logging on and off. Note: logging should
 * never be enabled in production builds
 */
public class LogUtil {

    public static void log(String tag, String output) {
        if ( BuildConfig.DEBUG || Settings.getLoggingEnabled()) {
            Log.d(tag, output);

            if ( Settings.getLoggingEnabled() )
                appendLog(System.currentTimeMillis() + " - " + tag + ": " + output);
        }
    }

    public static void e(String tag, String output) {
        if ( BuildConfig.DEBUG || Settings.getLoggingEnabled()) {
            Log.e(tag, output);

            if ( Settings.getLoggingEnabled() )
                appendLog(System.currentTimeMillis() + " - " + tag + ": " + output);
        }
    }

    public static void e(String tag, String output, Exception e) {
        if ( BuildConfig.DEBUG || Settings.getLoggingEnabled()) {
            Log.e(tag, "Exception: " + output, e);

            if ( Settings.getLoggingEnabled() ) {
                StringWriter error = new StringWriter();
                e.printStackTrace(new PrintWriter(error));
                appendLog(System.currentTimeMillis() + " - " + tag + ": " + output + error.toString());
            }
        }
    }

    public static void e(String tag, String output, Throwable t) {
        if ( BuildConfig.DEBUG || Settings.getLoggingEnabled()) {
            Log.e(tag, "Exception: " + output, t);

            if ( Settings.getLoggingEnabled() ) {
                StringWriter error = new StringWriter();
                t.printStackTrace(new PrintWriter(error));
                appendLog(System.currentTimeMillis() + " - " + tag + ": " + output + error.toString());
            }
        }
    }

    public static void e(String tag, Exception e) {
        if ( BuildConfig.DEBUG || Settings.getLoggingEnabled()) {
            e.printStackTrace();

            if ( Settings.getLoggingEnabled() ) {
                StringWriter error = new StringWriter();
                e.printStackTrace(new PrintWriter(error));
                appendLog(System.currentTimeMillis() + " - " + tag + ": " + error.toString());
            }
        }
    }

    private static void appendLog(String text)
    {
        File logFile = new File("sdcard/narrate.log");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            LineNumberReader lnr = new LineNumberReader(new FileReader(logFile));
            lnr.skip(Long.MAX_VALUE);

            if ( lnr.getLineNumber() > 5000 ) {
                logFile.delete();

                logFile = new File("sdcard/narrate.log");

                try
                {
                    logFile.createNewFile();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // Finally, the LineNumberReader object should be closed to prevent resource leak
            lnr.close();

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

package com.datonicgroup.narrate.app.ui.base;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by timothymiko on 7/6/14.
 */
public abstract class BaseActivity extends ActionBarActivity  {

    public float mScreenWidth;
    public float mScreenHeight;

    protected GlobalApplication mApp;

    protected Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    protected Thread.UncaughtExceptionHandler mExceptionHandler = new Thread.UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            LogUtil.log("ExceptionHandler", "FATAL EXCEPTION:");

            StringWriter error = new StringWriter();
            ex.printStackTrace(new PrintWriter(error));
            LogUtil.log("ExceptionHandler", error.toString());

            mDefaultExceptionHandler.uncaughtException(thread, ex);
        }
    };

    public View mStatusBarBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);

        mApp = (GlobalApplication) getApplication();

        DisplayMetrics dm = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        onCreateView();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        onCreateView();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        onCreateView();
    }

    private void onCreateView() {
        assignViews();
        setupActionBar();
    }

    protected void assignViews() {

    }

    protected void setupActionBar() {

    }

    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The activity has become visible (it is now "resumed").
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // The activity is about to be destroyed.
    }

    public void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            mStatusBarBg.setBackgroundColor(color);
        }
    }

}

package com.datonicgroup.narrate.app.ui;

import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.crashlytics.android.Crashlytics;
import com.datonicgroup.narrate.app.BuildConfig;
import com.parse.Parse;
import com.parse.ParseInstallation;

import io.fabric.sdk.android.Fabric;

/**
 * Created by admin on 5/29/2014.
 */
public class GlobalApplication extends MultiDexApplication {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        setupGlide();
        setupAnalytics();
        setupParse();
    }

    public static Context getAppContext() {
        return GlobalApplication.appContext;
    }

    private void setupGlide() {
        MemorySizeCalculator calculator = new MemorySizeCalculator(this);
        int defaultMemoryCacheSize = calculator.getMemoryCacheSize();
        int defaultBitmapPoolSize = calculator.getBitmapPoolSize();

        // disable the disk cache because all of our images are already cached on the sd card
        Glide.setup(new GlideBuilder(this)
                        .setDiskCache(new DiskCacheAdapter())
                        .setBitmapPool(new LruBitmapPool(defaultBitmapPoolSize))
                        .setMemoryCache(new LruResourceCache(defaultMemoryCacheSize))
        );
    }

    private void setupAnalytics() {
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }
    }

    private void setupParse() {
        Parse.initialize(this, BuildConfig.PARSE_APP_ID, BuildConfig.PARSE_CLIENT_KEY);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}

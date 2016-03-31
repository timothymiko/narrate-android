package com.datonicgroup.narrate.app.dataprovider.tasks;

import android.os.AsyncTask;

/**
 * Created by timothymiko on 9/10/14.
 */
public abstract class BaseTask<T, U, V> extends AsyncTask<T, U, V> {

    public interface Listener {
        void onFinish(Object result);
        void onStart();
    }

    private Listener mListener;

    public BaseTask<T, U, V> setListener(Listener listener) {
        this.mListener = listener;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if ( mListener != null )
            mListener.onStart();
    }

    @Override
    protected void onPostExecute(V v) {
        super.onPostExecute(v);

        if ( mListener != null )
            mListener.onFinish(v);
    }
}

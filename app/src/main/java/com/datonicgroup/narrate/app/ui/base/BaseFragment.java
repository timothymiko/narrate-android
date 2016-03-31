package com.datonicgroup.narrate.app.ui.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.OnActivityInteractionListener;
import com.datonicgroup.narrate.app.util.LogUtil;

/**
 * Created by timothymiko on 7/9/14.
 */
public abstract class BaseFragment extends Fragment implements OnActivityInteractionListener {

    protected GlobalApplication mApp;
    protected View mRoot;

    protected String mTitle = "Narrate";

    public boolean isFirstLoad = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = (GlobalApplication) getActivity().getApplication();
    }

    protected BaseActivity getMainActivity() {
        return (BaseActivity) getActivity();
    }

    protected void assignViews() {

        if ( mRoot == null )
            throw new RuntimeException("You must call set setContentView() before calling assignViews()!");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return mRoot;
    }

    @Override
    public void onStart() {
        super.onStart();
        if ( isFirstLoad ) {
            isFirstLoad = false;
            onFirstRun();
        }
    }

    public void onFirstRun() {

    }

    protected View findViewById(int resourceId) {
        return mRoot.findViewById(resourceId);
    }

    protected void setContentView(View v) {
        mRoot = v;
        assignViews();
    }

    protected void setContentView(int resourceId) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(inflater.inflate(resourceId, null));
    }

    public String getIdentifier() {
        return ((Object) this).getClass().getSimpleName();
    }

    private void log(String msg) {
        LogUtil.log(getIdentifier(), msg);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if ( !(activity instanceof BaseActivity) )
            throw new IllegalStateException("BaseFragment can only be used by classes that extend BaseActivity!");
    }
}

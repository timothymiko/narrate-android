package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.SectionListAdapter;
import com.datonicgroup.narrate.app.ui.entries.EntriesRecyclerAdapter;
import com.datonicgroup.narrate.app.ui.entries.ViewEntryActivity;

import java.util.ArrayList;

/**
 * Created by timothymiko on 12/29/14.
 */
public class EntryListDialogFragment extends DialogFragment implements RecyclerView.OnItemTouchListener {

    private final String STATE_ENTRIES = "listData";

    private ArrayList<Entry> mItems = new ArrayList<>();
    private RecyclerView mList;
    private EntriesRecyclerAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;
    private SectionListAdapter mSectionListAdapter;
    private GestureDetectorCompat mGestureDetector;

    public EntryListDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGestureDetector = new GestureDetectorCompat(getActivity(), new GestureListener());
        setRetainInstance(true);

        if ( savedInstanceState != null )
            mItems = savedInstanceState.getParcelableArrayList(STATE_ENTRIES);
    }

    public void setData(ArrayList<Entry> items) {
        mItems.clear();
        mItems.addAll(items);

        if ( mAdapter != null )
            mAdapter.notifyDataSetChanged();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            final Dialog dialog = new Dialog(getActivity(), android.support.v7.appcompat.R.style.Base_Theme_AppCompat_Light_Dialog);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_entry_list);

            mList = (RecyclerView) dialog.findViewById(R.id.list);
            mList.setHasFixedSize(true);
            mList.addOnItemTouchListener(this);

            mLayoutManager = new LinearLayoutManager(getActivity());
            mList.setLayoutManager(mLayoutManager);

            mAdapter = new EntriesRecyclerAdapter(mItems);
            mSectionListAdapter = new SectionListAdapter(getActivity(), R.layout.entries_month_header, R.id.month_header, mAdapter);
            mList.setAdapter(mSectionListAdapter);

            mAdapter.notifyDataSetChanged();

            mList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {

                    if ( mList.getWidth() > 0 && mList.getHeight() > 0 ) {
                        mList.getLayoutParams().height = getResources().getDimensionPixelOffset(R.dimen.entries_dialog_max_height);
                        mList.requestLayout();

                        mList.getViewTreeObserver().removeOnPreDrawListener(this);
                    }

                    return true;
                }
            });

            return dialog;

        } else
            return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putParcelableArrayList(STATE_ENTRIES, mItems);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            View view = mList.findChildViewUnder(e.getX(), e.getY());

            if (view != null) {
                view.playSoundEffect(SoundEffectConstants.CLICK);

                int pos = mList.getChildPosition(view);

                Intent i = new Intent(getActivity(), ViewEntryActivity.class);
                Bundle b = new Bundle();
                b.putParcelable(ViewEntryActivity.ENTRY_KEY, mItems.get(pos - mAdapter.getSectionOffset(pos)));
                i.putExtras(b);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.buildDrawingCache(true);
                    Bitmap drawingCache = view.getDrawingCache(true);
                    Bundle bundle = ActivityOptions.makeThumbnailScaleUpAnimation(view, drawingCache, 0, 0).toBundle();
                    getActivity().startActivity(i, bundle);
                } else {
                    startActivity(i);
                }
            }

            return super.onSingleTapUp(e);
        }
    }
}

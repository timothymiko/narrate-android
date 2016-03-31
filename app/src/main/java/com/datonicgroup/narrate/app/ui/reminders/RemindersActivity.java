package com.datonicgroup.narrate.app.ui.reminders;

import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.dataprovider.providers.RemindersDao;
import com.datonicgroup.narrate.app.dataprovider.tasks.DeleteReminderTask;
import com.datonicgroup.narrate.app.dataprovider.tasks.SaveReminderTask;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.ui.dialogs.ReminderDialog;
import com.datonicgroup.narrate.app.util.GraphicsUtil;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.List;

public class RemindersActivity extends BaseActivity implements View.OnClickListener, ReminderDialog.OnSaveListener, ReminderDialog.OnDeleteListener {

    public static final String TAG = "RemindersActivity";

    /**
     * Control
     */
    private RemindersTask mRemindersTask;
    private RemindersAdapter mAdapter;

    /**
     * Data
     */
    private List<Reminder> mReminders;

    /**
     * Views
     */
    private ListView mListView;
    private ProgressWheel mLoadingIndicator;
    private TextView mNoRemindersText;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolbar.setTitle(getString(R.string.title_activity_reminders));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                int statusBarHeight = 0;
                int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                RelativeLayout root = (RelativeLayout) findViewById(R.id.root);

                mStatusBarBg = new View(this);
                mStatusBarBg.setId(R.id.status_bar_bg);

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, statusBarHeight);
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                mStatusBarBg.setLayoutParams(lp);

                root.addView(mStatusBarBg);

                lp = (RelativeLayout.LayoutParams) mToolbar.getLayoutParams();
                lp.addRule(RelativeLayout.BELOW, R.id.status_bar_bg);
                mToolbar.setLayoutParams(lp);
            }

            setStatusBarColor(GraphicsUtil.darkenColor(getResources().getColor(R.color.primary), 0.85f));
        }

        findViewById(R.id.fab).setOnClickListener(this);
        ( (ImageView) findViewById(R.id.fab_bg) ).getDrawable().mutate().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.MULTIPLY);
    }

    @Override
    protected void assignViews() {
        super.assignViews();
        mListView = (ListView) findViewById(R.id.list);

        mReminders = new ArrayList<>();
        mAdapter = new RemindersAdapter(this, mReminders);
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ReminderDialog mDialog = new ReminderDialog();
                mDialog.setReminder(mReminders.get(position));
                mDialog.setDeleteListener(RemindersActivity.this);
                mDialog.setSaveListener(RemindersActivity.this);
                mDialog.show(getSupportFragmentManager(), "ReminderDialog");
            }
        });

        mLoadingIndicator = (ProgressWheel) findViewById(R.id.loading_indicator);
        mNoRemindersText = (TextView) findViewById(android.R.id.text1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRemindersTask = new RemindersTask();
        mRemindersTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( mRemindersTask != null )
            mRemindersTask.cancel(true);
    }

    @Override
    public void onClick(View v) {
        if ( v.getId() == R.id.fab ) {
            ReminderDialog mDialog = new ReminderDialog();
            mDialog.setDeleteListener(this);
            mDialog.setSaveListener(this);
            mDialog.show(getSupportFragmentManager(), "ReminderDialog");
        }
    }

    @Override
    public void onDelete(final Reminder r) {
        new DeleteReminderTask() {
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if ( !aBoolean ) {
                    Toast.makeText(RemindersActivity.this, "Error deleting reminder.", Toast.LENGTH_LONG).show();
                } else {
                    mReminders.remove(r);
                    mAdapter.notifyDataSetChanged();

                    if ( mReminders.size() == 0 )
                        mNoRemindersText.setVisibility(View.VISIBLE);
                }
            }
        }.execute(r);
    }

    @Override
    public void onSave(Reminder r) {
        new SaveReminderTask() {
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if (!aBoolean) {
                    Toast.makeText(RemindersActivity.this, "Error saving reminder.", Toast.LENGTH_LONG).show();
                } else {
                    mReminders.clear();
                    mReminders.addAll(RemindersDao.getAllReminders());
                    mAdapter.notifyDataSetChanged();
                    mNoRemindersText.setVisibility(View.GONE);
                }
            }
        }.execute(r);
    }

    private class RemindersTask extends AsyncTask<Void, Void, List<Reminder>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Reminder> doInBackground(Void... params) {

            return RemindersDao.getAllReminders();
        }

        @Override
        protected void onPostExecute(List<Reminder> reminders) {
            super.onPostExecute(reminders);

            mReminders.clear();
            mReminders.addAll(reminders);
            mAdapter.notifyDataSetChanged();

            final boolean mHasEntries = mReminders.size() > 0;

            if (mHasEntries) {
                mNoRemindersText.setVisibility(View.GONE);
            } else {
                mNoRemindersText.setVisibility(View.VISIBLE);
            }

            mListView.setVisibility(View.VISIBLE);
            mLoadingIndicator.setVisibility(View.GONE);
        }
    }
}

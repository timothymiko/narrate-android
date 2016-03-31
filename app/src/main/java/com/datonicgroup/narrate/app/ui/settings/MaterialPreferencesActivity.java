package com.datonicgroup.narrate.app.ui.settings;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.util.GraphicsUtil;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by timothymiko on 12/20/14.
 */
public class MaterialPreferencesActivity extends BaseActivity implements View.OnClickListener, DeveloperCard.DeveloperOptionsListener {

    /**
     * Control
     */
    private int versionTapCount;
    private int developerModeThreshold = 10;
    private Toast mVersionToast;

    private int mAccentColor;

    /**
     * Views
     */
    private Toolbar mToolbar;
    private View fab;
    private LinearLayout mCardsLayout;
    private TextView mTitle;
    private TextView mCredits;

    /**
     * Preferences
     */
    private GeneralSettingsCard mGeneralSettings;
    private SectionTogglesCard mSectionToggles;
    private PasscodeLockCard mPasscodeCard;
    private RemindersCard mRemindersCard;
    private SyncCard mSyncCard;
    private LocalBackupCard mLocalBackupCard;
    private LinksCard mLinksCard;
    private DeveloperCard mDeveloperCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccentColor = getResources().getColor(R.color.accent);

        setContentView(R.layout.activity_material_preferences);

        if ( Settings.getEmail() != null )
            ((TextView) findViewById(R.id.email)).setText(Settings.getEmail());

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // add a view behind status bar on KitKat builds
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                int statusBarHeight = 0;
                int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                LinearLayout root = (LinearLayout) findViewById(R.id.root);

                mStatusBarBg = new View(this);
                mStatusBarBg.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, statusBarHeight));
                root.addView(mStatusBarBg, 0);
            }

            setStatusBarColor(GraphicsUtil.darkenColor(getResources().getColor(R.color.primary), 0.85f));
        }

        mTitle = (TextView) findViewById(R.id.title);

        mCardsLayout = (LinearLayout) findViewById(R.id.settings_group);

        mGeneralSettings = new GeneralSettingsCard(this);
        mCardsLayout.addView(mGeneralSettings);

        mSectionToggles = new SectionTogglesCard(this);
        mCardsLayout.addView(mSectionToggles);

        mPasscodeCard = new PasscodeLockCard(this);
        mCardsLayout.addView(mPasscodeCard);

        mRemindersCard = new RemindersCard(this);
        mCardsLayout.addView(mRemindersCard);

        mSyncCard = new SyncCard(this);
        mCardsLayout.addView(mSyncCard);

        mLocalBackupCard = new LocalBackupCard(this);
        mCardsLayout.addView(mLocalBackupCard);

        mLinksCard = new LinksCard(this);
        mCardsLayout.addView(mLinksCard);

        if ( Settings.getDeveloperModeEnabled() ) {
            mDeveloperCard = new DeveloperCard(MaterialPreferencesActivity.this, MaterialPreferencesActivity.this);
            mCardsLayout.addView(mDeveloperCard);
        }

        mCredits = new TextView(this);
        StringBuilder s = new StringBuilder();
        s.append('\n');
        s.append(getString(R.string.version));
        s.append(' ');
        s.append(BuildConfig.VERSION_NAME);
        s.append('\n');
        s.append(getString(R.string.made_in_detroit));
        s.append("\n\n");
        s.append(getString(R.string.settings_open_source_footer));
        s.append("\n\n");
        mCredits.setText(s.toString());
        mCredits.setTextColor(getResources().getColor(R.color.secondary_text));
        mCredits.setGravity(Gravity.CENTER);
        mCredits.setLineSpacing(0, 1.2f);
        mCardsLayout.addView(mCredits);

        mCredits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( !Settings.getDeveloperModeEnabled() ) {
                    if (versionTapCount < developerModeThreshold - 1) {
                        versionTapCount++;

                        if (mVersionToast != null)
                            mVersionToast.cancel();

                        if (versionTapCount > 2) {
                            mVersionToast = Toast.makeText(MaterialPreferencesActivity.this, String.format("You are now %d steps away from enabling developer mode.", (developerModeThreshold - versionTapCount)), Toast.LENGTH_SHORT);
                            mVersionToast.show();
                        }

                    } else {

                        if (mVersionToast != null)
                            mVersionToast.cancel();

                        Toast.makeText(MaterialPreferencesActivity.this, "Developer mode enabled.", Toast.LENGTH_LONG).show();

                        Settings.setDeveloperModeEnabled(true);

                        // add developer mode preference
                        mDeveloperCard = new DeveloperCard(MaterialPreferencesActivity.this, MaterialPreferencesActivity.this);
                        mCardsLayout.addView(mDeveloperCard, mCardsLayout.getChildCount()-1);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSyncCard.mAuthenticatingDropbox)
            mSyncCard.onResume();

        if ( mLinksCard != null )
            mLinksCard.refreshPlusOneView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "tim@datonicgroup.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "User Feedback (" + BuildConfig.VERSION_NAME + ")");
                startActivity(Intent.createChooser(emailIntent, getString(R.string.email_support)));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SyncCard.GDRIVE_REQUEST_AUTHORIZATION:
                mSyncCard.onActivityResult(resultCode);
                return;
            case PasscodeLockCard.ENABLE_PASSLOCK:
            case PasscodeLockCard.DISABLE_PASSLOCK:
            case PasscodeLockCard.CHANGE_PASSWORD:
                mPasscodeCard.onActivityResult(requestCode, resultCode);
                return;
        }
    }

    @Override
    public void onDisableDevOptions() {
        mCardsLayout.removeView(mDeveloperCard);
        mDeveloperCard = null;
        versionTapCount = 0;
    }
}

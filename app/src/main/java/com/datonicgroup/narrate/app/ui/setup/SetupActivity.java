package com.datonicgroup.narrate.app.ui.setup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.datonicgroup.narrate.app.BuildConfig;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.base.BaseActivity;
import com.datonicgroup.narrate.app.util.SettingsUtil;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.viewpagerindicator.CirclePageIndicator;

import static com.datonicgroup.narrate.app.models.User.ACCOUNT_TYPE;

public class SetupActivity extends BaseActivity {


    /**
     * Views
     */
    private ViewPager mPager;
    private Adapter mAdapter;
    private ImageView mImage;
    private ImageView mAnimationImage;
    private ImageView mRightArrow;
    private ImageView mLeftArrow;
    private CirclePageIndicator mCirclePageIndicator;
    private final Handler mHandler = new Handler();

    private TextView mButton;
    private static final int REQUEST_CODE_EMAIL = 1;

    private int[] imgs = {
            R.drawable.setup_1,
            R.drawable.setup_2,
            R.drawable.setup_3,
            R.drawable.setup_4
    };

    private ValueAnimator anim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);

        mImage = (ImageView) findViewById(R.id.image_one);
        mAnimationImage = (ImageView) findViewById(R.id.image_two);
        mCirclePageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);

        mButton.setText(getString(R.string.get_started).toUpperCase());
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                            new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                    startActivityForResult(intent, REQUEST_CODE_EMAIL);
                } catch (ActivityNotFoundException e) {
                    // TODO
                }
            }
        });

        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new Adapter(
                new int[]{
                        R.string.setup_1,
                        R.string.setup_2,
                        R.string.setup_3,
                        R.string.setup_4
                },
                new int[] {
                        R.string.setup_subtitle_1,
                        R.string.setup_subtitle_2,
                        R.string.setup_subtitle_3
                });
        mPager.setAdapter(mAdapter);
        mCirclePageIndicator.setViewPager(mPager);
        mCirclePageIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(final int position) {

                if (anim != null)
                    anim.cancel();

                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (position == 3) {
                            mButton.setVisibility(View.VISIBLE);
                            mButton.animate().alpha(1f).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mButton.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            }).start();
                        } else if (mButton.getAlpha() == 1f) {
                            mButton.animate().alpha(0f).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mButton.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            }).start();
                        }

                        mAnimationImage.setAlpha(0f);
                        mAnimationImage.setVisibility(View.VISIBLE);
                        mAnimationImage.setImageResource(imgs[position]);
                        anim = ValueAnimator.ofFloat(0, 1);
                        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                mImage.setAlpha(1.0f - animation.getAnimatedFraction());
                                mAnimationImage.setAlpha(animation.getAnimatedFraction());
                            }
                        });
                        anim.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mImage.setImageResource(imgs[position]);
                                mImage.setAlpha(1f);
                                mAnimationImage.setVisibility(View.GONE);
                                mAnimationImage.setImageBitmap(null);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        anim.start();
                    }
                }, 150);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void assignViews() {
        super.assignViews();
        mButton = (TextView) findViewById(R.id.button);
//        mRightArrow = (ImageView) findViewById(R.id.right_arrow);
//        mLeftArrow = (ImageView) findViewById(R.id.left_arrow);
//        mRightArrow = (ImageView) findViewById(R.id.right_arrow);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EMAIL) {
            if (resultCode == RESULT_OK) {

                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                final String email = accountName;

                Settings.setAppVersion(BuildConfig.VERSION_CODE);
                Settings.setEmail(email);

                final AccountManager accountManager = AccountManager.get(GlobalApplication.getAppContext());
                Account[] accs = accountManager.getAccountsByType(ACCOUNT_TYPE);

                Account acc;

                if (accs != null) {

                    if ( accs.length > 0 ) {
                        for ( int i = 0; i < accs.length; i++ ) {
                            final boolean mLastAccount = i == accs.length-1;
                            accountManager.removeAccount(accs[0], new AccountManagerCallback<Boolean>() {
                                @Override
                                public void run(AccountManagerFuture<Boolean> future) {
                                    if ( mLastAccount ) {
                                        Account acc = new Account(email, ACCOUNT_TYPE);
                                        accountManager.addAccountExplicitly(acc, null, null);
                                    }
                                }
                            }, mHandler);
                        }
                    } else {
                        acc = new Account(email, ACCOUNT_TYPE);
                        accountManager.addAccountExplicitly(acc, null, null);
                    }
                }

                SettingsUtil.setupCompleted(SetupActivity.this);
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    private class Adapter extends PagerAdapter {

        private int[] titles;
        private int[] subtitles;

        private Adapter(int[] titles, int[] subtitles) {
            this.titles = titles;
            this.subtitles = subtitles;
        }

        @Override
        public int getCount() {
            return this.titles.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            View v = getLayoutInflater().inflate(R.layout.setup_viewpager_item, null);
            v.setY(mScreenHeight * .55f);

            TextView title = (TextView) v.findViewById(R.id.title);
            title.setText(titles[position]);

            TextView subtitle = (TextView) v.findViewById(R.id.subtitle);

            if ( position < subtitles.length )
                subtitle.setText(subtitles[position]);

            collection.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

}

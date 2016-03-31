package com.datonicgroup.narrate.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.google.android.gms.plus.PlusOneButton;

/**
 * Created by timothymiko on 1/7/15.
 */
public class LinksCard extends PreferenceCard implements View.OnClickListener {

    private PlusOneButton plusOneButton;

    public LinksCard(Context context) {
        super(context);
    }

    @Override
    protected void init() {
        super.init();

        setTitle(R.string.links);

        ButtonPreference mCommunityLink = new ButtonPreference(getContext());
        ButtonPreference mTaskerLink = new ButtonPreference(getContext());
        ButtonPreference mGithubLink = new ButtonPreference(getContext());

        mCommunityLink.setTitle(R.string.google_plus);
        mTaskerLink.setTitle(R.string.links_tasker_documentation);
        mGithubLink.setTitle(R.string.links_github);

        mCommunityLink.setButtonText(R.string.go);
        mTaskerLink.setButtonText(R.string.go);
        mGithubLink.setButtonText(R.string.go);

        mCommunityLink.setTag("community");
        mTaskerLink.setTag("tasker");
        mGithubLink.setTag("github");

        mCommunityLink.setOnClickListener(this);
        mTaskerLink.setOnClickListener(this);
        mGithubLink.setOnClickListener(this);

        plusOneButton = new PlusOneButton(getContext());
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        lp.leftMargin = getResources().getDimensionPixelOffset(R.dimen.default_height) * 3;
        plusOneButton.setLayoutParams(lp);
        mCommunityLink.addView(plusOneButton);

        addView(mGithubLink);
        addView(mCommunityLink);
        addView(mTaskerLink);
    }

    public void refreshPlusOneView() {
        String url = "https://market.android.com/details?id=com.datonicgroup.narrate.app";
        plusOneButton.initialize(url, null);
    }

    @Override
    public void onClick(View v) {

        String url = null;

        switch ((String)v.getTag()) {
            case "community":
                url = "https://plus.google.com/communities/106662199081610755624";
                break;
            case "github":
                url = "https://github.com/timothymiko/narrate-android";
                break;
            case "tasker":
                url = "https://github.com/timothymiko/narrate-android";
                break;
        }

        if ( url != null ) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            getContext().startActivity(i);
        }
    }
}

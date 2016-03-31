package com.datonicgroup.narrate.app.ui.dialogs;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;

/**
 * Created by timothymiko on 9/4/14.
 */
public class BaseDialog extends AlertDialog {

    public interface OnDismisListener {
        void onDismiss();
    }

    private Context mContext;

    private String title;
    private String body;
    private String negativeButton;
    private String positiveButton;

    private boolean showTitle = true;

    private View.OnClickListener mNegativeListener;
    private View.OnClickListener mPositiveListener;
    private OnDismisListener mDismissListener;

    private int iconResource = -1;

    private View v;

    public BaseDialog(Context context) {
        super(context);
        this.mContext = context;
    }

    public BaseDialog setTitle(String t) {
        this.title = t;
        return this;
    }

    public BaseDialog setBody(String body) {
        this.body = body;
        return this;
    }

    public BaseDialog setNegativeButton(String text) {
        this.negativeButton = text;
        return this;
    }

    public BaseDialog setPositiveButton(String text) {
        this.positiveButton = text;
        return this;
    }

    public BaseDialog setShowTitle(boolean show) {
        this.showTitle = show;
        return this;
    }

    public BaseDialog setNegativeClickListener(View.OnClickListener listener) {
        this.mNegativeListener = listener;
        return this;
    }

    public BaseDialog setPositiveClickListener(View.OnClickListener listener) {
        this.mPositiveListener = listener;
        return this;
    }

    public BaseDialog setDismissListener(OnDismisListener listener) {
        this.mDismissListener = listener;
        return this;
    }

    public BaseDialog setIconResource(int resourceId) {
        this.iconResource = resourceId;
        return this;
    }

    public void build() {

        v = View.inflate(mContext, R.layout.base_dialog, null);

        TextView title = (TextView) v.findViewById(R.id.title);
        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        TextView body = (TextView) v.findViewById(R.id.body);
        TextView negative = (TextView) v.findViewById(R.id.negative);
        TextView positive = (TextView) v.findViewById(R.id.positive);

        if ( showTitle ) {
            title.setText(this.title);
        } else {
            title.setVisibility(View.GONE);
        }

        if ( this.iconResource != -1 )
            icon.setImageResource(iconResource);
        else
            icon.setVisibility(View.GONE);

        body.setText(this.body);

        negative.setText(this.negativeButton);
        positive.setText(this.positiveButton);

        negative.setOnClickListener(mNegativeListener);
        positive.setOnClickListener(mPositiveListener);

        super.setView(v);
    }

    public View getView() {
        return v;
    }

    @Override
    public void dismiss() {
        super.dismiss();

        if ( mDismissListener != null )
            mDismissListener.onDismiss();
    }
}

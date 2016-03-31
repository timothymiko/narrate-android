package com.datonicgroup.narrate.app.ui.reminders;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.util.LogUtil;

import java.util.List;

/**
 * Created by timothymiko on 9/22/14.
 */
public class RemindersAdapter extends BaseAdapter {

    private Context mContext;
    private Resources res;
    private List<Reminder> mItems;

    public RemindersAdapter(Context context, List<Reminder> mItems) {
        this.mContext = context;
        this.res = context.getResources();
        this.mItems = mItems;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Reminder getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LogUtil.log("RemindersAdapter", "getView()");

        View v = convertView;
        Reminder r = getItem(position);

        if ( convertView == null )
            v = View.inflate(mContext, R.layout.reminder_list_item, null);

        ImageView icon = (ImageView) v.findViewById(R.id.icon);
        TextView description = (TextView) v.findViewById(R.id.text);
        TextView time = (TextView) v.findViewById(R.id.time);

        if ( r.recurring ) {
            icon.setBackgroundResource(R.drawable.circle_background_ic1);
            icon.setImageResource(R.drawable.reminder_recurring);
        } else {
            icon.setBackgroundResource(R.drawable.circle_background_ic2);
            icon.setImageResource(R.drawable.reminder_one_time);
        }

        time.setText(Reminder.getOccurence(res, r));
        description.setText(r.description);

        return v;
    }
}

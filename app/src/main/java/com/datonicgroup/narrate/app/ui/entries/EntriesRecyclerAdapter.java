package com.datonicgroup.narrate.app.ui.entries;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.models.Entry;
import com.datonicgroup.narrate.app.ui.GlobalApplication;
import com.datonicgroup.narrate.app.ui.RoundedCornerTransformation;
import com.datonicgroup.narrate.app.ui.SectionListAdapter;
import com.datonicgroup.narrate.app.ui.SectionListHelper;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.util.DateUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Created by timothymiko on 10/17/14.
 */
public class EntriesRecyclerAdapter extends RecyclerView.Adapter<EntriesRecyclerAdapter.ViewHolder> implements SectionListHelper {

    private List<Entry> mItems;
    private int mCount;
    private static SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    private static int mCurrentYear;

    /**
     * onBindViewHolder() helpers
     */
    private Entry entry;
    private Calendar date;
    private String description;
    private List<String> tags;
    private boolean starred;
    private RelativeLayout.LayoutParams lp;
    private int fourdp;
    private final Drawable[] mCircleBackgrounds;
    private Calendar emptyCal;
    private final HashMap<Long, Integer> colorMap = new HashMap<>();
    private final HashSet<Long> colorDates = new HashSet<>();
    private int mBackgroundCount = 0;

    private List<SectionListAdapter.Section> mSectionIndices;

    private SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    private boolean mActionModeVisible;

    private final RoundedCornerTransformation mRoundCornerTransformation;

    protected class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView description;
        TextView dayOfMonth;
        TextView timeOfDay;
        View bookmark;

        public ViewHolder(View v) {
            super(v);

            thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
            title = (TextView) v.findViewById(R.id.title);
            description = (TextView) v.findViewById(R.id.description);
            dayOfMonth = (TextView) v.findViewById(R.id.day_of_month);
            timeOfDay = (TextView) v.findViewById(R.id.time);
            bookmark = v.findViewById(R.id.bookmark);
        }
    }

    public EntriesRecyclerAdapter(List<Entry> mItems) {
        this.mItems = mItems;
        this.mCount = mItems.size();

        fourdp = GlobalApplication.getAppContext().getResources().getDimensionPixelOffset(R.dimen.separator_height);
        mCurrentYear = Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR);


        Resources resources = GlobalApplication.getAppContext().getResources();
        mCircleBackgrounds = new Drawable[] {
                resources.getDrawable(R.drawable.circle_background_purple_dark),
                resources.getDrawable(R.drawable.circle_background_blue_dark),
                resources.getDrawable(R.drawable.circle_background_green_dark),
                resources.getDrawable(R.drawable.circle_background_red_dark),
                resources.getDrawable(R.drawable.circle_background_purple),
                resources.getDrawable(R.drawable.circle_background_blue),
                resources.getDrawable(R.drawable.circle_background_green),
                resources.getDrawable(R.drawable.circle_background_red)};

        emptyCal = Calendar.getInstance();
        emptyCal.set(Calendar.MILLISECOND, 0);
        emptyCal.set(Calendar.SECOND, 0);
        emptyCal.set(Calendar.MINUTE, 0);
        emptyCal.set(Calendar.HOUR, 0);

        this.mRoundCornerTransformation = new RoundedCornerTransformation(GlobalApplication.getAppContext(), R.dimen.eight_dp);
    }

    public void updateTimeFormat() {
        timeFormat = new SimpleDateFormat(DateUtil.getTimeFormatString(Settings.getTwentyFourHourTime()));
        notifyDataSetChanged();
    }

    public void notifyDatasetChanged() {
        mCount = mItems.size();
        super.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, final int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.entry_material_list_item, viewGroup, false);
        return new ViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        viewHolder.itemView.setClickable(true);
        viewHolder.itemView.setActivated(mSelectedItems.get(position, false));

        entry = mItems.get(position);

        date = entry.creationDate;
        description = entry.text;
        tags = entry.tags;
        starred = entry.starred;

        emptyCal.set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH));
        emptyCal.set(Calendar.MONTH, date.get(Calendar.MONTH));
        emptyCal.set(Calendar.YEAR, date.get(Calendar.YEAR));

        viewHolder.title.setText(entry.title);

        if (description != null)
            viewHolder.description.setText(description.substring(0, Math.min(description.length(), 400)));

        viewHolder.timeOfDay.setText(timeFormat.format(date.getTime()));

        if (entry.photos.isEmpty()) {

            viewHolder.thumbnail.setVisibility(View.GONE);
            viewHolder.dayOfMonth.setVisibility(View.VISIBLE);

            viewHolder.dayOfMonth.setText(String.valueOf(entry.creationDate.get(Calendar.DAY_OF_MONTH)));

            if (!colorDates.contains(emptyCal.getTimeInMillis())) {

                int index = mBackgroundCount++ % mCircleBackgrounds.length;

                if (Build.VERSION.SDK_INT >= 16)
                    viewHolder.dayOfMonth.setBackground(mCircleBackgrounds[index]);
                else
                    viewHolder.dayOfMonth.setBackgroundDrawable(mCircleBackgrounds[index]);

                viewHolder.dayOfMonth.setTag(index);

                colorMap.put(emptyCal.getTimeInMillis(), index);
                colorDates.add(emptyCal.getTimeInMillis());
            } else {

                if (Build.VERSION.SDK_INT >= 16)
                    viewHolder.dayOfMonth.setBackground(mCircleBackgrounds[colorMap.get(emptyCal.getTimeInMillis())]);
                else
                    viewHolder.dayOfMonth.setBackgroundDrawable(mCircleBackgrounds[colorMap.get(emptyCal.getTimeInMillis())]);

            }
        } else {
            viewHolder.dayOfMonth.setVisibility(View.GONE);
            viewHolder.thumbnail.setVisibility(View.VISIBLE);

            String path = entry.photos.get(0).path;
            File image = new File(path);
            Glide.with(GlobalApplication.getAppContext())
                    .load(path)
                    .transform(mRoundCornerTransformation)
                    .placeholder(R.color.transparent)
                    .signature(new StringSignature(String.valueOf(image.lastModified())))
                    .into(viewHolder.thumbnail);
        }

        if (starred) {
            viewHolder.bookmark.setVisibility(View.VISIBLE);
        } else {
            viewHolder.bookmark.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public List<SectionListAdapter.Section> getSections() {
        mSectionIndices = new ArrayList<>();

        if ( mItems != null && mItems.size() > 0 ) {

            int curYear = Calendar.getInstance(Locale.getDefault()).get(Calendar.YEAR);

            SimpleDateFormat df = new SimpleDateFormat("MMMM");
            SimpleDateFormat df2 = new SimpleDateFormat("MMMM yyyy");

            Entry e = mItems.get(0);
            int prevMonth = e.creationDate.get(Calendar.MONTH);
            int prevYear = e.creationDate.get(Calendar.YEAR);

            if ( prevYear == curYear )
                mSectionIndices.add(new SectionListAdapter.Section(0, df.format(e.creationDate.getTime())));
            else
                mSectionIndices.add(new SectionListAdapter.Section(0, df2.format(e.creationDate.getTime())));

            for ( int i = 1; i < mItems.size(); i++ ) {
                e = mItems.get(i);
                int month = e.creationDate.get(Calendar.MONTH);
                int year = e.creationDate.get(Calendar.YEAR);

                if ( month != prevMonth || year != prevYear ) {
                    if ( year == curYear )
                        mSectionIndices.add(new SectionListAdapter.Section(i, df.format(e.creationDate.getTime())));
                    else
                        mSectionIndices.add(new SectionListAdapter.Section(i, df2.format(e.creationDate.getTime())));

                    prevMonth = month;
                    prevYear = year;
                }
            }
        }

        return mSectionIndices;
    }

    public void toggleSelection(int pos) {

        int offsetPos = pos - getSectionOffset(pos);

        if (mSelectedItems.get(offsetPos, false)) {
            mSelectedItems.delete(offsetPos);
        } else {
            mSelectedItems.put(offsetPos, true);
        }

        notifyItemChanged(pos);

    }

    public void clearSelections() {
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(mSelectedItems.size());

        for (int i = 0; i < mSelectedItems.size(); i++)
            items.add(mSelectedItems.keyAt(i));

        return items;
    }

    public int getSectionOffset(int pos) {
        int count = 0;
        int i = 0;
        while ( i < mSectionIndices.size() && pos > mSectionIndices.get(i).getSectionedPosition() ) {
            count++;
            i++;
        }

        return count;
    }
}

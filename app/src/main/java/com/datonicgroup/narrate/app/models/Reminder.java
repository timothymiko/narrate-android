package com.datonicgroup.narrate.app.models;

import android.content.res.Resources;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.datonicgroup.narrate.app.util.NumberUtil;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by timothymiko on 9/22/14.
 */
public class Reminder {

    public String uuid;
    public String description;
    public boolean recurring;

    // fields for one time reminders
    public DateTime date;

    // fields for recurring reminders
    public Recurrence recurrence;

    public String occurrenceString;


    /**
     * Helper methods
     */

    private static SimpleDateFormat mDayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault());
    private static SimpleDateFormat mYearlyFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private static SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault());
    private static SimpleDateFormat mTimeFormat = new SimpleDateFormat(DateUtil.getTimeFormatString(Settings.getTwentyFourHourTime()), Locale.getDefault());

    public static String getOccurence(Resources res, Reminder r) {
        return getOccurence(res, r.date, r.recurrence);
    }

    public static String getOccurence(Resources res, DateTime date, Recurrence er) {
        if ( er.getInternalValue() > 0 ) {

            if (er == null)
                return null;

            if (er == Recurrence.Daily)
                return res.getString(R.string.daily_occurrence) + mTimeFormat.format(date.toDate());

            if (er == Recurrence.Weekly)
                return res.getString(R.string.weekly_occurrence) + mDayOfWeek.format(date.toDate()) + res.getString(R.string.at) + mTimeFormat.format(date.toDate());

            if (er == Recurrence.Monthly)
                return NumberUtil.getOrdinalSuffix(date.getDayOfMonth()) + res.getString(R.string.monthly_occurrence);

            if ( er == Recurrence.Yearly)
                return mYearlyFormat.format(date.toDate()) + res.getString(R.string.yearly_occurrence);

            return null;

        } else {

            if ( DateUtil.isToday(date) )
                return res.getString(R.string.today_occurrence) + mTimeFormat.format(date.toDate());

            if ( DateUtil.isTomorrow(date) )
                return res.getString(R.string.tomorrow_occurrence) + mTimeFormat.format(date.toDate());

            return mDateFormat.format(date.toDate()) + res.getString(R.string.at) + mTimeFormat.format(date.toDate());

        }
    }
}

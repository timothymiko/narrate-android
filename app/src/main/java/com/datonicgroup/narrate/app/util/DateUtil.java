package com.datonicgroup.narrate.app.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by timothymiko on 8/3/14.
 */
public class DateUtil {

    public static final int SECOND = 1;
    public static final int SECOND_IN_MILLISECONDS = 1000;

    public static final int MINUTE = 1;
    public static final int MINUTE_IN_SECONDS = 60;
    public static final int MINUTE_IN_MILLISECONDS = MINUTE_IN_SECONDS * SECOND_IN_MILLISECONDS;

    public static final int HOUR = 1;
    public static final int HOUR_IN_MINUTES = 60;
    public static final int HOUR_IN_SECONDS = HOUR_IN_MINUTES * MINUTE_IN_SECONDS;
    public static final int HOUR_IN_MILLISECONDS = HOUR_IN_MINUTES * MINUTE_IN_MILLISECONDS;

    public static final int DAY = 1;
    public static final int DAY_IN_HOURS = 24;
    public static final int DAY_IN_MINUTES = DAY_IN_HOURS * HOUR_IN_MINUTES;
    public static final int DAY_IN_SECONDS = DAY_IN_HOURS * HOUR_IN_SECONDS;
    public static final int DAY_IN_MILLISECONDS = DAY_IN_HOURS * HOUR_IN_MILLISECONDS;

    public static final int WEEK = 1;
    public static final int WEEK_IN_DAYS = 7;
    public static final int WEEK_IN_HOURS = WEEK_IN_DAYS * DAY_IN_HOURS;
    public static final int WEEK_IN_MINUTES = WEEK_IN_DAYS * DAY_IN_MINUTES;
    public static final int WEEK_IN_SECONDS = WEEK_IN_DAYS * DAY_IN_SECONDS;
    public static final int WEEK_IN_MILLISECONDS = WEEK_IN_DAYS * DAY_IN_MILLISECONDS;

    /**
     * Returns true if a given date is anytime today
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime today, false if not
     */
    public static boolean isToday(DateTime date) {
        DateTime todayMidnight = new DateTime().withTimeAtStartOfDay();
        DateTime tomorrowMidnight = new DateTime().plusDays(1).withTimeAtStartOfDay();
        return (todayMidnight.getMillis() == date.getMillis()) || (tomorrowMidnight.isAfter(date.getMillis()) && todayMidnight.isBefore(date.getMillis()));
    }

    /**
     * Returns true if a given date is anytime tomorrow
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime today, false if not
     */
    public static boolean isTomorrow(DateTime date) {
        DateTime tomorrowMidnight = new DateTime().withTimeAtStartOfDay().plusDays(1);
        DateTime twoDaysMidnight = new DateTime().withTimeAtStartOfDay().plusDays(2);
        return ((tomorrowMidnight.getMillis() == date.getMillis() || tomorrowMidnight.isAfter(date.getMillis())) && twoDaysMidnight.isBefore(date.getMillis()));
    }

    /**
     * Returns true if a given date is anytime yesterday
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime yesterday, false if not
     */
    public static boolean isYesterday(DateTime date) {
        DateTime todayMidnight = new DateTime().withTimeAtStartOfDay();
        DateTime yesterdayMidnight = new DateTime().minusDays(1).withTimeAtStartOfDay();
        return (yesterdayMidnight.getMillis() == date.getMillis()) || (yesterdayMidnight.isBefore(date.getMillis()) && todayMidnight.isAfter(date.getMillis()));
    }

    /**
     * Returns true if a given date is anytime in the current
     * week Sun to Sat.
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime this week, false if not
     */
    public static boolean isCurrentWeek(DateTime date) {
        DateTime firstDayOfWeekMidnight = DateTime.now(DateTimeZone.getDefault()).withDayOfWeek(Calendar.getInstance(Locale.getDefault()).getMinimum(Calendar.DAY_OF_WEEK)).withTimeAtStartOfDay();
        DateTime firstDayOfNextWeek = firstDayOfWeekMidnight.plusDays(7);
        return ((firstDayOfWeekMidnight.isEqual(date.getMillis())) || firstDayOfWeekMidnight.isBefore(date.getMillis())) && firstDayOfNextWeek.isAfter(date.getMillis());
    }

    /**
     * Returns true if a given date is anytime this month.
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime this month, false if not
     */
    public static boolean isCurrentMonth(DateTime date) {
        DateTime firstDayOfMonthMidnight = DateTime.now(DateTimeZone.getDefault()).withDayOfMonth(Calendar.getInstance(Locale.getDefault()).getMinimum(Calendar.DAY_OF_MONTH)).withTimeAtStartOfDay();
        DateTime firstDayOfNextMonth = firstDayOfMonthMidnight.plusMonths(1);
        return ((firstDayOfMonthMidnight.isEqual(date.getMillis())) || firstDayOfMonthMidnight.isBefore(date.getMillis())) && firstDayOfNextMonth.isAfter(date.getMillis());
    }

    /**
     * Returns true is a given date is anytime this year
     *
     * @param   date Date to inspect
     * @return  True if the date is sometime this year, false if not
     */
    public static boolean isCurrentYear(DateTime date) {
        return date.getYear() == DateTime.now().getYear();
    }

    /**
     * Returns true if a given date is anytime prior to now
     *
     * @param   date Date to inspect
     * @return  True if the date is some time prior to midnight today, false otherwise
     * @see     #isHistoricalDay(org.joda.time.DateTime)
     */
    public static boolean isHistorical(DateTime date) {
        return DateTime.now().isAfter(date.getMillis());
    }

    /**
     * Returns true if a given date is anytime prior to today
     *
     * @param   date Date to inspect
     * @return  True if the date is some time prior to midnight today, false otherwise
     */
    public static boolean isHistoricalDay(DateTime date) {
        DateTime todayMidnight = new DateTime().withTimeAtStartOfDay();
        return todayMidnight.isAfter(date.getMillis());
    }

    public static String getTimeFormatString(boolean useMilitaryTime) {
        if ( useMilitaryTime )
            return "H:mm";
        else
            return "h:mm a";
    }
}

package com.datonicgroup.narrate.app.ui.dialogs;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.datonicgroup.narrate.app.R;
import com.datonicgroup.narrate.app.dataprovider.Settings;
import com.datonicgroup.narrate.app.models.Recurrence;
import com.datonicgroup.narrate.app.models.Reminder;
import com.datonicgroup.narrate.app.util.DateUtil;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by timothymiko on 9/22/14.
 */
public class ReminderDialog extends MaterialDialogFragment implements DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener/*implements
        CalendarDatePickerDialog.OnDateSetListener,
        RadialTimePickerDialog.OnTimeSetListener,
        DialogInterface.OnDismissListener*/ {

    public interface OnSaveListener {
        public void onSave(Reminder r);
    }

    public interface OnDeleteListener {
        public void onDelete(Reminder r);
    }

    private Reminder r;

    private OnDeleteListener mDeleteListener;
    private OnSaveListener mSaveListener;

    TextView mDescription;
    Spinner mRecurrence;
    RelativeLayout mDate;
    RelativeLayout mTime;
    TextView mDateText;
    TextView mTimeText;

    private DatePickerDialog mDatePickerDialog;
    private TimePickerDialog mTimePickerDialog;

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM d, yy", Locale.getDefault());
    private static final SimpleDateFormat mTimeFormat = new SimpleDateFormat(DateUtil.getTimeFormatString(Settings.getTwentyFourHourTime()), Locale.getDefault());

    private DateTime date = DateTime.now();
    private Recurrence recurrence = Recurrence.Daily;
    private boolean isRecurring;

    /**
     * Internal control
     */
    private boolean mSetDate;
    private boolean mSetTime;
    private boolean editing;
    private boolean mDidSave;

    public ReminderDialog() {
    }

    public void setReminder(Reminder r) {
        this.r = r;
        this.editing = true;
        this.mSetDate = true;
        this.mSetTime = true;
    }

    public void setDeleteListener(OnDeleteListener mListener) {
        this.mDeleteListener = mListener;
    }

    public void setSaveListener(OnSaveListener mListener) {
        this.mSaveListener = mListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if ( getActivity() != null ) {

            setTitle(R.string.reminders_add_new);
            setContentView(R.layout.reminder_dialog);

            final Dialog dialog = super.onCreateDialog(savedInstanceState);

            Button neg = (Button) dialog.findViewById(R.id.dialog_button_negative);
            neg.setText(editing ? getString(R.string.delete_uc) : getString(R.string.cancel_uc));
            neg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();

                    if (editing && mDeleteListener != null)
                        mDeleteListener.onDelete(r);
                }
            });
            Button pos = (Button) dialog.findViewById(R.id.dialog_button_positive);
            pos.setText(R.string.save_uc);
            pos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( mDescription.getText().toString().length() == 0 ) {
                        Toast.makeText(getActivity(), getString(R.string.error_description), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if ( !mSetDate ) {
                        Toast.makeText(getActivity(), getString(R.string.error_date), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if ( !mSetTime ) {
                        Toast.makeText(getActivity(), getString(R.string.error_time), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if ( DateUtil.isHistorical(date) ) {
                        Toast.makeText(getActivity(), getString(R.string.error_historical), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mDidSave = true;

                    if ( r == null ) {
                        r = new Reminder();
                        r.uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                    }

                    r.description = mDescription.getText().toString();
                    r.date = date;
                    r.recurring = isRecurring;
                    r.recurrence = recurrence;
                    r.occurrenceString = Reminder.getOccurence(getResources(), r);

                    dismiss();

                    if (mSaveListener != null)
                        mSaveListener.onSave(r);
                }
            });

            mDescription = (TextView) dialog.findViewById(R.id.description);
            mRecurrence = (Spinner) dialog.findViewById(R.id.recurrence);
            mDate = (RelativeLayout) dialog.findViewById(R.id.date);
            mTime = (RelativeLayout) dialog.findViewById(R.id.time);
            mDateText = (TextView) dialog.findViewById(R.id.date_text);
            mTimeText = (TextView) dialog.findViewById(R.id.time_text);

            mDate.getBackground().setColorFilter(Color.parseColor("#838383"), PorterDuff.Mode.MULTIPLY);
            mTime.getBackground().setColorFilter(Color.parseColor("#838383"), PorterDuff.Mode.MULTIPLY);

            Resources res = getResources();
            mDescription.setHint(res.getString(R.string.description));
            mDateText.setHint("  " + res.getString(R.string.date));
            mTimeText.setHint("  " + res.getString(R.string.time));

            mDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDatePickerDialog.show(getActivity().getFragmentManager(), "DatePicker");
                }
            });

            mTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTimePickerDialog.show(getActivity().getFragmentManager(), "TimePicker");
                }
            });

            mDatePickerDialog = DatePickerDialog
                    .newInstance(this, date.getYear(), date.getMonthOfYear()-1,
                            date.getDayOfMonth());

            mTimePickerDialog = TimePickerDialog.
                    newInstance(this, date.plusMinutes(1).getHourOfDay(), date.plusMinutes(1).getMinuteOfHour(),
                            Settings.getTwentyFourHourTime()
                    );

            ArrayAdapter<Recurrence> mSpinnerAdapter = new ArrayAdapter<Recurrence>(getActivity(), android.R.layout.simple_spinner_dropdown_item, Recurrence.values());
            mRecurrence.setAdapter(mSpinnerAdapter);

            mRecurrence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    recurrence = Recurrence.lookup(position);
                    isRecurring = position > 0;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            if ( r != null ) {
                mDescription.setText(r.description);
                mDateText.setText("  " + mDateFormat.format(r.date.toDate()));
                mTimeText.setText("  " + mTimeFormat.format(r.date.toDate()));

                date = new DateTime(r.date);

                recurrence = r.recurrence;
                mRecurrence.setSelection(r.recurrence.getInternalValue());

                mSetDate = true;
                mSetTime = true;
            }

            return dialog;

        } else
            return null;
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        mSetDate = true;
        date = date.withDate(year, monthOfYear+1, dayOfMonth);
        if (DateUtil.isToday(date))
            mDateText.setText("  " + getString(R.string.today));
        else if (DateUtil.isTomorrow(date))
            mDateText.setText("  " + getString(R.string.tomorrow));
        else
            mDateText.setText("  " + mDateFormat.format(date.toDate()));
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        mSetTime = true;
        date = date.withTime(hourOfDay, minute, 0, 0);
        mTimeText.setText("  " + mTimeFormat.format(date.toDate()));
        mTimePickerDialog.setStartTime(hourOfDay, minute);
    }
}

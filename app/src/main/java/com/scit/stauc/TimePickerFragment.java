package com.scit.stauc;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimePickerFragment extends DialogFragment {

    public static final String EXTRA_TIME_HOUR = "timepickerfragment.time.hour";
    public static final String EXTRA_TIME_MINUTE = "timepickerfragment.time.minute";
    private TimePicker mTimePicker;

    public static TimePickerFragment newInstance(){
        return new TimePickerFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_time, null);
        mTimePicker = v.findViewById(R.id.dialog_time_time_picker);
        mTimePicker.setIs24HourView(true);

        return new AlertDialog.Builder(getActivity()).setTitle(R.string.date_picker_title)
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int hour = mTimePicker.getCurrentHour();
                        int minute = mTimePicker.getCurrentMinute();
                        sendResult(Activity.RESULT_OK, hour, minute);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .create();
    }

    private void sendResult(int resultCode, int hour, int minute){
        if (getTargetFragment() == null){
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_TIME_HOUR, hour);
        intent.putExtra(EXTRA_TIME_MINUTE, minute);
        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}

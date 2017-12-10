package com.aplus.pillreminder.fragment;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.aplus.pillreminder.AlarmNotificationReceiver;
import com.aplus.pillreminder.model.Pill;
import com.aplus.pillreminder.model.RemindTime;

import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 */
public class AddPillFragment extends PillInfoFragment {

    private Pill pill;

    public AddPillFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pill = new Pill();
    }

    @Override
    void onActionConfirm() {
        insertPillWithTimes();
    }

    @Override
    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
        super.onClick(dialog, selectedColor, allColors);
        pill.setColor(selectedColor);
    }

    private void insertPillWithTimes() {
        pill.setName(actvName.getText().toString());
        pill.setDescribe(etDescribe.getText().toString());
        try {
            pill.setQuantity(Integer.parseInt(etQuantity.getText().toString()));
            pill.setDose(Integer.parseInt(etDose.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Please enter quantity.", Toast.LENGTH_LONG).show();
            return;
        }

        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... voids) {
                return db.pillDao().insert(pill);
            }

            @Override
            protected void onPostExecute(final Long aLong) {
                super.onPostExecute(aLong);

                for (RemindTime remindTime : timeList) {
                    new AsyncTask<RemindTime, Void, Void>() {
                        @Override
                        protected Void doInBackground(RemindTime... remindTimes) {
                            remindTimes[0].setPillId(aLong.intValue());
                            db.remindTimeDao().insert(remindTimes[0]);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
//                            setAlarm(aLong.intValue(), remindTime.getHour(), remindTime.getMinute(), false, pill);
                            listener.onActionConfirmPressed();
                        }
                    }.execute(remindTime);
                }
            }
        }.execute();
    }

    private void setAlarm(int uniqueId, int hour, int minute, boolean isRepeat, Pill pill){

        AlarmManager alarmManager = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);

//        int importance = NotificationManager.IMPORTANCE_HIGH;
//        NotificationChannel mChannel = new NotificationChannel(String.valueOf(uniqueId), "testtttt", importance);
//        mChannel.setDescription("Welcome to Notification Service");
//        mChannel.enableLights(true);
//        mChannel.enableVibration(true);
//        mChannel.setShowBadge(true);
//        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

        Intent myIntent = new Intent(getActivity(), AlarmNotificationReceiver.class);
//        myIntent.putExtra("notificationChannel", mChannel);
        myIntent.putExtra("id", uniqueId);
        myIntent.putExtra("pill", pill);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), uniqueId, myIntent, 0);

        //cancel alarm
//        myIntent = new Intent(SetActivity.this, AlarmActivity.class);
//        pendingIntent = PendingIntent.getActivity(CellManageAddShowActivity.this,
//                id, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        pendingIntent.cancel();
//        alarmManager.cancel(pendingIntent);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if(!isRepeat){
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),3000, pendingIntent);
        }
    }
}

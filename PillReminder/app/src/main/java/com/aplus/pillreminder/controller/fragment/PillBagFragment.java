package com.aplus.pillreminder.controller.fragment;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aplus.pillreminder.R;
import com.aplus.pillreminder.adapter.PillInfoAdapter;
import com.aplus.pillreminder.database.DatabaseManager;
import com.aplus.pillreminder.database.PillReminderDb;
import com.aplus.pillreminder.model.EatLog;
import com.aplus.pillreminder.model.Pill;
import com.aplus.pillreminder.model.PillWithRemindTime;
import com.aplus.pillreminder.model.RemindTime;
import com.aplus.pillreminder.receiver.AlarmNotificationReceiver;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PillBagFragment extends Fragment implements SwipeMenuListView.OnMenuItemClickListener {

    public static String TAG = PillBagFragment.class.getSimpleName();
    private PillReminderDb db;
    private List<PillWithRemindTime> pillWithRemindTimes;
    private PillInfoAdapter adapter;
    public SwipeMenuListView listView;
    private OnSwipeMenuClickListener listener;

    public interface OnSwipeMenuClickListener {
        void onEditClicked(PillWithRemindTime pillWithRemindTime);
    }

    public PillBagFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DatabaseManager.getInstance().getDb();
        pillWithRemindTimes = new ArrayList<>();
        adapter = new PillInfoAdapter(getActivity(), R.layout.item_pill_info_list, pillWithRemindTimes);
        listener = (OnSwipeMenuClickListener) getActivity();

        loadPills();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pill_bag, container, false);

        setup(view);

        return view;
    }

    @Override
    public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
        PillWithRemindTime pillWithRemindTime = pillWithRemindTimes.get(position);

        switch (index) {
            case 0:
                // edit pill
                listener.onEditClicked(pillWithRemindTime);
                break;
            case 1:
                // delete pill
                Pill pill = pillWithRemindTime.getPill();
                deletePill(pill);
                deleteAllNotTakenLogs(pill);
                cancelAlarms(pillWithRemindTime.getRemindTimeList());
                break;
        }
        // false : close the menu; true : not close the menu
        return true;
    }

    public void loadPills() {
        new AsyncTask<Void, Void, List<PillWithRemindTime>>() {
            @Override
            protected List<PillWithRemindTime> doInBackground(Void... voids) {
                return db.pillDao().loadPillsWithRemindTimes();
            }

            @Override
            protected void onPostExecute(List<PillWithRemindTime> pillWithRemindTimes) {
                adapter.clear();
                adapter.addAll(pillWithRemindTimes);
            }
        }.execute();
    }

    private void setup(View view) {
        listView = view.findViewById(R.id.listView);
        listView.setAdapter(adapter);
        createSwipeMenu();
    }

    private void createSwipeMenu() {
        SwipeMenuCreator creator = new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {
                // create "edit" item
                SwipeMenuItem editItem = new SwipeMenuItem(getActivity());
                editItem.setBackground(android.R.color.darker_gray);
                editItem.setWidth(dp2px(90));
                editItem.setIcon(R.drawable.ic_edit);
                menu.addMenuItem(editItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(getActivity());
                deleteItem.setBackground(android.R.color.holo_red_light);
                deleteItem.setWidth(dp2px(90));
                deleteItem.setIcon(R.drawable.ic_delete);
                menu.addMenuItem(deleteItem);
            }
        };

        // set creator
        listView.setMenuCreator(creator);
        listView.setOnMenuItemClickListener(this);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private void deletePill(final Pill pill) {
        new AsyncTask<Void, Void, Pill>() {
            @Override
            protected Pill doInBackground(Void... voids) {
                db.pillDao().delete(pill);

                return null;
            }

            @Override
            protected void onPostExecute(Pill pill) {
                loadPills();
            }
        }.execute();
    }

    private void deleteAllNotTakenLogs(Pill pill) {
        new AsyncTask<Pill, Void, List<EatLog>>() {
            @Override
            protected List<EatLog> doInBackground(Pill... pills) {
                Calendar calendar = Calendar.getInstance();

                // Date at 00:00.00
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                Date dayStart = calendar.getTime();

                // Date at 23:59.59
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                Date dayEnd = calendar.getTime();

                return db.eatLogDao().getNotTakenLogs(pills[0].getId(), dayStart, dayEnd);
            }

            @Override
            protected void onPostExecute(List<EatLog> eatLogs) {
                new AsyncTask<List<EatLog>, Void, Void>() {
                    @Override
                    protected Void doInBackground(List<EatLog>[] lists) {
                        db.eatLogDao().delete(lists[0]);
                        return null;
                    }
                }.execute(eatLogs);
            }
        }.execute(pill);
    }

    private void cancelAlarms(List<RemindTime> remindTimes) {
        AlarmManager alarmManager =
                (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);

        Intent intent =
                new Intent(getActivity(), AlarmNotificationReceiver.class);

        for (RemindTime r : remindTimes) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), r.getId(), intent, 0);
            alarmManager.cancel(pendingIntent);
        }
    }
}

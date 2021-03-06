package com.aplus.pillreminder.controller.fragment;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aplus.pillreminder.R;
import com.aplus.pillreminder.adapter.PillAdapter;
import com.aplus.pillreminder.database.DatabaseManager;
import com.aplus.pillreminder.database.PillReminderDb;
import com.aplus.pillreminder.model.Pill;
import com.aplus.pillreminder.model.PillWithRemindTime;
import com.aplus.pillreminder.model.RemindTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    public static String TAG = HomeFragment.class.getSimpleName();
    private PillReminderDb db;
    @BindView(R.id.gridMorning) RecyclerView gridMorning;
    @BindView(R.id.gridAfternoon) RecyclerView gridAfternoon;
    @BindView(R.id.gridEvening) RecyclerView gridEvening;
    @BindView(R.id.gridNight) RecyclerView gridNight;
    private PillAdapter adapterMorning, adapterAfternoon, adapterEvening, adapterNight;
    private List<Pill> pillsMorning, pillsAfternoon, pillsEvening, pillsNight;

    public HomeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = DatabaseManager.getInstance().getDb();

        pillsMorning = new ArrayList<>();
        pillsAfternoon = new ArrayList<>();
        pillsEvening = new ArrayList<>();
        pillsNight = new ArrayList<>();

        adapterMorning = new PillAdapter();
        adapterAfternoon = new PillAdapter();
        adapterEvening = new PillAdapter();
        adapterNight = new PillAdapter();

        loadPillWithRemindTime();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ButterKnife.bind(this, view);

        setup();

        return view;
    }

    public void loadPillWithRemindTime() {
        new AsyncTask<Void, Void, List<PillWithRemindTime>>() {
            @Override
            protected List<PillWithRemindTime> doInBackground(Void... voids) {
                return db.pillDao().loadPillsWithRemindTimes();
            }

            @Override
            protected void onPostExecute(List<PillWithRemindTime> pillWithRemindTimes) {
                super.onPostExecute(pillWithRemindTimes);

                pillsMorning.clear();
                pillsAfternoon.clear();
                pillsEvening.clear();
                pillsNight.clear();

                for (PillWithRemindTime pillWithRemindTime : pillWithRemindTimes) {
                    Pill pill = pillWithRemindTime.getPill();
                    for (RemindTime remindTime : pillWithRemindTime.getRemindTimeList()) {
                        int hour = remindTime.getHour();
                        if (4 <= hour && hour < 11) { // 04:00 - 10:59
                            pillsMorning.add(pill);
                        } else if (11 <= hour && hour < 17) { // 11:00 - 16:59
                            pillsAfternoon.add(pill);
                        } else if (17 <= hour && hour < 22) { // 17:00 - 21:59
                            pillsEvening.add(pill);
                        } else { // 22:00 - 03:59
                            pillsNight.add(pill);
                        }
                    }
                }

                adapterMorning.setData(pillsMorning);
                adapterAfternoon.setData(pillsAfternoon);
                adapterEvening.setData(pillsEvening);
                adapterNight.setData(pillsNight);

                adapterMorning.notifyDataSetChanged();
                adapterAfternoon.notifyDataSetChanged();
                adapterEvening.notifyDataSetChanged();
                adapterNight.notifyDataSetChanged();
            }
        }.execute();
    }

    private void setup() {
        gridMorning.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        gridAfternoon.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        gridEvening.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        gridNight.setLayoutManager(new GridLayoutManager(getActivity(), 4));

        gridMorning.setAdapter(adapterMorning);
        gridAfternoon.setAdapter(adapterAfternoon);
        gridEvening.setAdapter(adapterEvening);
        gridNight.setAdapter(adapterNight);
    }
}

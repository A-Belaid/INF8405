package com.example.embroa.wifisearcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class BatteryActivity extends AppCompatActivity {
    private ListView batteryView;
    private ListView bandwidthView;

    private ArrayList<String> batteryHistory;
    private ArrayAdapter<String> batteryAdapter;
    private ArrayList<String> bandwidthHistory;
    private ArrayAdapter<String> bandwidthAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery);

        final IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BroadcastReceiver batteryLevelReceiver;
        BatteryHistory.initHistory("BatteryActivity");
        batteryLevelReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                BatteryHistory.callbackOnReceive(intent, getProjectDB());
            }
        };
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);

        this.setTitle("Historique Ressources");

        batteryView = (ListView) findViewById(R.id.batteryView);
        bandwidthView = (ListView) findViewById(R.id.bandwidthView);

        batteryHistory = new ArrayList<String>();
        batteryAdapter = null;

        bandwidthHistory = new ArrayList<String>();
        bandwidthAdapter = null;

        updateBatteryView();
        updateBandwidthView();
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        BatteryHistory.initHistory("BatteryActivity");
        updateBatteryView();
    }

    public SQLiteDatabase getProjectDB() {
        return openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
    }

    public void updateBatteryView() {
        batteryHistory.clear();
        ArrayList<String> dbHistory = BatteryHistory.getHistory(getProjectDB());

        for(String iter: dbHistory)
            batteryHistory.add(iter);

        if(batteryView.getAdapter() == null) {
            batteryAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, batteryHistory);
            batteryView.setAdapter(batteryAdapter);
        } else {
            int listEndPos = batteryView.getLastVisiblePosition();
            batteryAdapter.notifyDataSetChanged();
            batteryView.setVerticalScrollbarPosition(listEndPos);
        }
    }

    public void clearBatteryHistory(View view) {
        BatteryHistory.clearHistory(getProjectDB());
        updateBatteryView();
    }

    public void updateBandwidthView() {
        bandwidthHistory.clear();
        ArrayList<String> dbHistory = BandwidthHistory.getHistory(getProjectDB());

        for(String iter: dbHistory)
            bandwidthHistory.add(iter);

        if(bandwidthView.getAdapter() == null) {
            bandwidthAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bandwidthHistory);
            bandwidthView.setAdapter(bandwidthAdapter);
        } else {
            int listEndPos = bandwidthView.getLastVisiblePosition();
            bandwidthAdapter.notifyDataSetChanged();
            bandwidthView.setVerticalScrollbarPosition(listEndPos);
        }
    }

    public void clearBandwidthHistory(View view) {
        BandwidthHistory.clearHistory(getProjectDB());
        updateBandwidthView();
    }
}

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

    private ArrayList<String> history;
    private ArrayAdapter<String> adapter;

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

        this.setTitle("Historique Batterie");

        batteryView = (ListView) findViewById(R.id.batteryView);

        history = new ArrayList<String>();
        adapter = null;

        updateBatteryView();
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
        history.clear();
        ArrayList<String> dbHistory = BatteryHistory.getHistory(getProjectDB());

        for(String iter: dbHistory)
            history.add(iter);

        if(batteryView.getAdapter() == null) {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, history);
            batteryView.setAdapter(adapter);
        } else {
            int listEndPos = batteryView.getLastVisiblePosition();
            adapter.notifyDataSetChanged();
            batteryView.setVerticalScrollbarPosition(listEndPos);
        }
    }

    public void clearHistory(View view) {
        BatteryHistory.clearHistory(getProjectDB());
        updateBatteryView();
    }
}

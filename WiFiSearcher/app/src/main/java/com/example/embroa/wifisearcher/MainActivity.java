package com.example.embroa.wifisearcher;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        ArrayAdapter<String> adapter;
        if (wifi.isWifiEnabled() == false)
        {
            wifi.setWifiEnabled(true);
        }

        //BroadcastReceiver SCAN_RESULTS_AVAILABLE_ACTION

        List<ScanResult> results = wifi.getScanResults();
        String result = results.get(0).SSID;
        ArrayList<String> listSSID = new ArrayList<String>();
        for (int i = 0; i < results.size(); i++)
            listSSID.add(results.get(i).SSID);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listSSID);
        listView.setAdapter(adapter);

    }
}

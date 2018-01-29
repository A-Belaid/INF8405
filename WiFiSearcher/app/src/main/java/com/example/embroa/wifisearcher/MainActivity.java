package com.example.embroa.wifisearcher;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ListView scanListView;
    TextView scanNumView;

    WifiManager wifi;
    Receiver wifiReceiver;
    List<ScanResult> wifiResults;
    ArrayAdapter<String> adapter;
    private final Handler scanHandler = new Handler();
    static final int LOCATION_PERMISSION_REQUEST = 1;
    Integer scanDelay = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanListView = (ListView) findViewById(R.id.scanListView);
        scanNumView = (TextView) findViewById(R.id.scanNumView);
        scanNumView.setTextSize(24);
        adapter = null;

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);

        wifiReceiver = new Receiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //Check for location permission (wifi scan doesn't work otherwise)
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) //Ask for permission if not granted
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        else //Scan for wifi otherwise
            scanWifi();
    }

    //Check the request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                // If the request is accepted, we scan for wifi
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    scanWifi();
                else { //Otherwise...
                    /*TODO*/
                }
                return;
            }
        }
    }

    //Scan for wifi every scanDelay milliseconds
    public void scanWifi() {
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                wifi.startScan();
                scanWifi();
            }
        }, scanDelay);
    }

    //When the app stop, we unregister the receiver to stop the wifi scan
    @Override
    protected void onStop()
    {
        unregisterReceiver(wifiReceiver);
        super.onStop();
    }

    //When the app restart, we register the receiver and restart the wifi scan
    @Override
    protected void onRestart()
    {
        super.onRestart();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        scanWifi();
    }

    class Receiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            ArrayList<String> listSSID = new ArrayList<String>();

            //Get available wifi
            wifiResults = wifi.getScanResults();

            for (int i = 0; i < wifiResults.size(); i++)
               listSSID.add(wifiResults.get(i).SSID);

            int scanNum = listSSID.size();
            String scanNumStr = scanNum > 0 ? Integer.toString(scanNum) : "Aucun";
            scanNumView.setText(scanNumStr + (scanNum > 1 ? " réseaux trouvés" : " réseau trouvé"));

            if(scanListView.getAdapter() == null) {
                adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, listSSID);
                scanListView.setAdapter(adapter);
            } else {
                int scanListPos = scanListView.getFirstVisiblePosition();
                adapter.notifyDataSetChanged();
                scanListView.setVerticalScrollbarPosition(scanListPos);
            }
        }
    }
}

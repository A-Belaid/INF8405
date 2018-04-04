package com.example.embroa.wifisearcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;

import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.androidfung.geoip.IpApiService;
import com.androidfung.geoip.ServicesManager;
import com.androidfung.geoip.model.GeoIpResponseModel;
import com.google.android.gms.maps.model.LatLng;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {
    ListView scanListView;
    TextView scanNumView;
    Button conDetailsBtn;

    WifiManager wifi;
    Receiver wifiReceiver;

    static List<ScanResult> wifiResults;
    ArrayList<String> listSSID;
    ArrayAdapter<String> adapter;
    JSONArray hotspotsArray;
    private final Handler scanHandler = new Handler();
    static final int LOCATION_PERMISSION_REQUEST = 1;
    Integer scanDelay = 1000;
    GeoData geoData;
    boolean isFirstScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanListView = (ListView) findViewById(R.id.scanListView);
        scanNumView = (TextView) findViewById(R.id.scanNumView);
        scanNumView.setTextSize(24);
        conDetailsBtn = (Button) findViewById(R.id.conDetailsBtn);
        conDetailsBtn.setEnabled(false);

        listSSID = new ArrayList<String>();
        adapter = null;
        isFirstScan = true;

        scanListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                displayOneScan(position);
            }
        });

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);

        wifiReceiver = new Receiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        else
        {
            geoData = new GeoData();
            setGeoData();
            conDetailsBtn.setEnabled(true);

            try {
                requestGeoLocation();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            scanWifi();
        }

        final IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BroadcastReceiver batteryLevelReceiver;

        final MainActivity thisActivity = this;
        batteryLevelReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent){
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float battPct = (level/(float)scale) * 100;
                thisActivity.setTitle("Wifi Searcher (" + String.valueOf(battPct) + "%)");
            }
        };

        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }

    //Check the request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    scanWifi();
            }
        }
    }

    //Scan for wifi every 1000 milliseconds
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

    //Displays an alert containing all relevant infos related to a selected scanned network
    //Capabilities: "Describes the authentication, key management, and encryption schemes supported by the access point."
    public void displayOneScan(int position){
        boolean isRecentOs = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
        ScanResult selectedResult = wifiResults.get(position);
        String alertTitle = listSSID.get(position);
        String alertMessage = "<b>MAC Address: </b>" + selectedResult.BSSID + "<br/>" +
                "<b>Signal Level: </b>" + selectedResult.level + "dBm (" + WifiManager.calculateSignalLevel(selectedResult.level, 10) + "/10) <br/>" +
                "<b>Capabilities: </b>" + selectedResult.capabilities;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alertTitle).setMessage(isRecentOs ? Html.fromHtml(alertMessage, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(alertMessage))
                .setPositiveButton("OK", null);
        builder.create().show();
    }

    //Displays an alert containing data about the current connection
    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void displayCurrentConDetails(View view) {
        final MainActivity thisActivity = this;
        final String netName = wifi.getConnectionInfo().getSSID();
        String alertMessage = "<b>Country: </b>" + geoData.getCountry() + " (" + geoData.getCountryCode() +  ")" + "<br/>" +
                "<b>City: </b>" + geoData.getCity() + ", " + geoData.getRegion() + "<br/>" +
                "<b>Latitude: </b>" + geoData.getLatitudeStr() + "°<br/>" +
                "<b>Longitude: </b>" + geoData.getLongitudeStr() + "°<br/>" +
                "<b>Fuseau horaire: </b>" + geoData.getTimezone() + "<br/>" +
                "<b>ISP: </b>" + geoData.getISP() + "<br/></br>";
        boolean isRecentOs = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(netName)
                .setMessage(isRecentOs ? Html.fromHtml(alertMessage, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(alertMessage))
                .setPositiveButton("Localiser", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent toMapIntent = new Intent(thisActivity, MapsActivity.class);
                        double latitude = geoData.getLatitude();
                        double longitude = geoData.getLongitude();
                        ArrayList<LatLng> locations = new ArrayList();
                        toMapIntent.putExtra("NAME", netName);
                        toMapIntent.putExtra("LAT", latitude);
                        toMapIntent.putExtra("LONG", longitude);
                        startActivity(toMapIntent);
                    }
                })
                .setNegativeButton("Retour", null);
        builder.create().show();
    }

    //Saves tne current connection's data, based on its IP
    public void setGeoData() {
        final MainActivity thisActivity = this;

        IpApiService ipApiService = ServicesManager.getGeoIpService();
        ipApiService.getGeoIp().enqueue(new Callback<GeoIpResponseModel>() {
            @Override
            public void onResponse(Call<GeoIpResponseModel> call, retrofit2.Response<GeoIpResponseModel> response) {
                thisActivity.geoData.setCountry(response.body().getCountry());
                thisActivity.geoData.setCity(response.body().getCity());
                thisActivity.geoData.setCountryCode(response.body().getCountryCode());
                thisActivity.geoData.setLatitude(response.body().getLatitude());
                thisActivity.geoData.setLongitude(response.body().getLongitude());
                thisActivity.geoData.setRegion(response.body().getRegion());
                thisActivity.geoData.setTimezone(response.body().getTimezone());
                thisActivity.geoData.setIsp(response.body().getIsp());
            }

            @Override
            public void onFailure(Call<GeoIpResponseModel> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Saves tne current connection geolocation, using the Google Maps Geolocation API
    public void requestGeoLocation() throws JSONException {
        final MainActivity thisActivity = this;

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyDHYnfgIctmnEUuXmqx24tm0KNLBidA78Q";

        JSONObject requestObject = new JSONObject();
        requestObject.put("considerIp", true);

        requestObject.put("wifiAccessPoints", hotspotsArray);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, requestObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject locationObject = response.getJSONObject("location");
                            geoData.setLatitude(locationObject.getDouble("lat"));
                            geoData.setLongitude(locationObject.getDouble("lng"));

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                    }
                });

        queue.add(jsObjRequest);
    }

    //Updates the list of available networks after each scan (every 1000ms)
    class Receiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            listSSID.clear();

            //Get available wifi
            wifiResults = wifi.getScanResults();
            filterWifiResults();

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

            if(isFirstScan) {
                try {
                    setHotspotsArray();
                    requestGeoLocation();
                    isFirstScan = false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Removes duplicate network names, and nameless networks
    public void filterWifiResults() {
        ArrayList<String> wifiNames = new ArrayList<String>();
        ArrayList<ScanResult> scansToRemove = new ArrayList<ScanResult>();
        for(ScanResult wifiResult: wifiResults) {
            String wifiName = wifiResult.SSID;
            if(wifiNames.contains(wifiName) || wifiName.equals("") || wifiName == null)
                scansToRemove.add(wifiResult);
            else
                wifiNames.add(wifiName);
        }
        for(ScanResult scanToRemove: scansToRemove)
            wifiResults.remove(scanToRemove);
    }

    //Saves the mac addresses of all detected wifis in a JSON array (for geolocalisation purposes)
    public void setHotspotsArray() throws JSONException {
        hotspotsArray = new JSONArray();

        for(ScanResult scanResult : wifiResults) {
            String macAddress = scanResult.BSSID;

            JSONObject hotspotInstance = new JSONObject();
            hotspotInstance.put("macAddress", macAddress);
            hotspotsArray.put(hotspotInstance);
        }
    }
}

package com.example.embroa.wifisearcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.ScanResult;
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

import com.androidfung.geoip.IpApiService;
import com.androidfung.geoip.ServicesManager;
import com.androidfung.geoip.model.GeoIpResponseModel;

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
    List<ScanResult> wifiResults;
    ArrayList<String> listSSID;
    ArrayAdapter<String> adapter;
    private final Handler scanHandler = new Handler();
    static final int LOCATION_PERMISSION_REQUEST = 1;
    Integer scanDelay = 1000;
    GeoData geoData;

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

        //Check for location permission (wifi scan doesn't work otherwise)
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) //Ask for permission if not granted
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        else //Scan for wifi otherwise
        {
            geoData = new GeoData();
            setGeoData();
            conDetailsBtn.setEnabled(true);

            scanWifi();
        }
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

    //Displays an alert containing (for now) the capabilities of a selected scanned network
    //Capabilities: "Describes the authentication, key management, and encryption schemes supported by the access point."
    public void displayOneScan(int position){
        String alertTitle = listSSID.get(position);
        String alertMessage = wifiResults.get(position).capabilities;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alertTitle).setMessage(alertMessage).setPositiveButton("OK", null);
        builder.create().show();
    }

    //Displays an alert containing (for now) the geolocation of the current connection
    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void displayCurrentConDetails(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Détails sur la connexion en cours")
                .setMessage(Html.fromHtml("<b>Country: </b>" + geoData.getCountry() + " (" + geoData.getCountryCode() +  ")" + "<br/>" +
                        "<b>City: </b>" + geoData.getCity() + ", " + geoData.getRegion() + "<br/>" +
                        "<b>Latitude: </b>" + geoData.getLatitudeStr() + "°<br/>" +
                        "<b>Longitude: </b>" + geoData.getLongitudeStr() + "°<br/>" +
                        "<b>Fuseau horaire: </b>" + geoData.getTimezone() + "<br/>" +
                        "<b>ISP: </b>" + geoData.getISP(), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null);
        builder.create().show();
    }

    class Receiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            listSSID.clear();

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

    //Saves tne current connection's geolocation, based on its IP
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
}

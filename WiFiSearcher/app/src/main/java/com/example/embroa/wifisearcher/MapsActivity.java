package com.example.embroa.wifisearcher;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String netName;
    private double latitude;
    private double longitude;
    private ArrayList<LatLng> locations = new ArrayList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        netName = intent.getStringExtra("NAME");
        latitude = intent.getDoubleExtra("LAT", 0);
        longitude = intent.getDoubleExtra("LONG", 0);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng currentNet = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(currentNet).title(netName).icon(BitmapDescriptorFactory.fromResource(R.drawable.current_wifi)));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentNet));

        for(ScanResult wifiResult: MainActivity.wifiResults) {

            latitude += (Math.random()*2-1)/1000;
            longitude += (Math.random()*2-1)/500;
            LatLng netcoords = new LatLng(latitude, longitude);
            boolean isNetworkPrivate = wifiResult.capabilities.contains("WPA") || wifiResult.capabilities.contains("TKIP");
            int signalLevel = WifiManager.calculateSignalLevel(wifiResult.level, 3);
            mMap.addMarker(new MarkerOptions().position(netcoords).title(wifiResult.SSID).icon(BitmapDescriptorFactory.fromResource(getMarkerColor(isNetworkPrivate, signalLevel))));
        }
    }

    public int getMarkerColor(boolean isLocked, int signalLevel) {
        if(isLocked) return getLockedMarkerColor(signalLevel);
        else return getUnlockedMarkerColor(signalLevel);
    }

    public int getLockedMarkerColor(int signalLevel) {
        switch(signalLevel) {
            case 3: return R.drawable.private_wifi_3;
            case 2:return R.drawable.private_wifi_2;
            default: return R.drawable.private_wifi_1;
        }
    }

    public int getUnlockedMarkerColor(int signalLevel) {
        switch(signalLevel) {
            case 3: return R.drawable.free_wifi_3;
            case 2: return R.drawable.free_wifi_2;
            default: return R.drawable.free_wifi_1;
        }
    }
}

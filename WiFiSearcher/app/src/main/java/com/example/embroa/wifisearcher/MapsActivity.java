package com.example.embroa.wifisearcher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String netName;
    private double latitude;
    private double longitude;
    private ArrayList<LatLng> locations = new ArrayList();
    private JSONArray favorites;

    final private String FAV_SCANS_FILE = "FavScans.json";

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

        favorites = new JSONArray();
        //TODO
        try {
            /*File dir = getFilesDir();
            File favFile = new File(dir.getAbsolutePath() + '\\' + FAV_SCANS_FILE);
            if(!favFile.exists())
                favFile.createNewFile();
            FileInputStream fileInputStream = openFileInput(dir.getAbsolutePath() + '/' + FAV_SCANS_FILE);//Crash ici
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            String jsonContent = "";
            while ((line = bufferedReader.readLine()) != null) jsonContent += line;
            favorites = new JSONArray(jsonContent.equals("") ? "[]" : jsonContent);
            bufferedReader.close();
            inputStreamReader.close();
            fileInputStream.close();*/
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {

        }
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
        MarkerOptions currentMarker = new MarkerOptions().position(currentNet).title(netName);
        currentMarker.icon(BitmapDescriptorFactory.fromResource(isMarkerOptionsFavorite(currentMarker) ? R.drawable.fav_wifi : R.drawable.current_wifi));
        mMap.addMarker(currentMarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentNet));

        double randLat = latitude;
        double randLng = longitude;
        for(ScanResult wifiResult: MainActivity.wifiResults) {
            String wifiName = "\"" + wifiResult.SSID + "\"";
            if(!wifiName.equals(netName)) {
                randLat += (Math.random()*2-1)/1000;
                randLng += (Math.random()*2-1)/500;
                LatLng netcoords = new LatLng(randLat, randLng);
                boolean isNetworkPrivate = wifiResult.capabilities.contains("WPA") || wifiResult.capabilities.contains("TKIP");
                int signalLevel = WifiManager.calculateSignalLevel(wifiResult.level, 3);
                mMap.addMarker(new MarkerOptions().position(netcoords).title(wifiName).icon(BitmapDescriptorFactory.fromResource(getMarkerColor(isNetworkPrivate, signalLevel))));
            }
        }

        for(int i = 0; i < favorites.length(); i++) {
            try {
                JSONObject obj = (JSONObject) favorites.get(i);
                String wifiName = "\"" + obj.getString("Name") + "\"";
                if(!wifiName.equals(netName)) {
                    LatLng netcoords = new LatLng(obj.getDouble("Lat"), obj.getDouble("Lng"));
                    mMap.addMarker(new MarkerOptions().position(netcoords).title(wifiName).icon(BitmapDescriptorFactory.fromResource(R.drawable.fav_wifi)));
                }
            } catch (JSONException e) {

            }
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker){
                getMarkerAlert(marker).create().show();
            }
        });
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

    public AlertDialog.Builder getMarkerAlert(final Marker marker) {
        boolean isRecentOs = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
        String alertTitle = "";
        String alertMessage = "";

        if(isMarkerFavorite(marker)) {
            try {
                JSONObject obj = (JSONObject) favorites.get(getFavIndex(marker.getTitle()));
                alertMessage = "<b>MAC Address: </b>" + obj.getString("Mac") + "<br/>" +
                        "<b>Capabilities: </b>" + obj.get("Caps");
            } catch (JSONException e) {

            }
        } else {
            ScanResult selectedMarkerScan = getSelectedMarkerScan(marker);
            alertMessage = "<b>MAC Address: </b>" + selectedMarkerScan.BSSID + "<br/>" +
                    "<b>Capabilities: </b>" + selectedMarkerScan.capabilities;
        }

        alertTitle = marker.getTitle();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(alertTitle).setMessage(isRecentOs ? Html.fromHtml(alertMessage, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(alertMessage))
                .setNegativeButton("OK", null);

        if(alertTitle.equals(netName)) {
            if(isMarkerFavorite(marker)) {
                builder.setPositiveButton("Remove from Favs.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favorites.remove(getFavIndex(marker.getTitle()));
                        updateFavs();
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.current_wifi));
                    }
                });
            } else {
                builder.setPositiveButton("Add to Favs.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            JSONObject newFav = new JSONObject();
                            newFav.put("Name", getSelectedMarkerScan(marker).SSID);
                            newFav.put("Mac", getSelectedMarkerScan(marker).BSSID);
                            newFav.put("Caps", getSelectedMarkerScan(marker).capabilities);
                            newFav.put("Lat", latitude);
                            newFav.put("Lng", longitude);
                            favorites.put(newFav);
                            updateFavs();
                            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.fav_wifi));
                        } catch (JSONException e) {

                        }

                    }
                });
            }
        } else if(isMarkerFavorite(marker)) {
            builder.setPositiveButton("Remove from Favs.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    favorites.remove(getFavIndex(marker.getTitle()));
                    updateFavs();
                }
            });
        }

        return builder;
    }

    public ScanResult getSelectedMarkerScan(Marker marker) {
        for(ScanResult scanResult: MainActivity.wifiResults) {
            String name = "\"" + scanResult.SSID + "\"";
            if(marker.getTitle().equals(name))
                return scanResult;
        }

        return null;
    }

    public int getFavIndex(String name1) {
        try {
            for(int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name2 = "\"" + obj.getString("Name") + "\"";
                if(name1.equals(name2))
                    return i;
            }
        } catch (JSONException e) {

        }

        return -1;
    }

    public JSONObject getSelectedMarkerDetailedScan(Marker marker) {
        //for(JSONObject scanResult: favorites) {
        try {
            for(int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name = "\"" + obj.getString("Name") + "\"";
                if(marker.getTitle().equals(name))
                    return obj;
            }
        } catch (JSONException e) {}

        return null;
    }

    public boolean isMarkerFavorite(Marker marker) {
        try {
            for(int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name = "\"" + obj.getString("Name") + "\"";
                if(marker.getTitle().equals(name))
                    return true;
            }
        } catch (JSONException e) {

        }

        return false;
    }

    public boolean isMarkerOptionsFavorite(MarkerOptions marker) {
        try{
            for(int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name = "\"" + obj.getString("Name") + "\"";
                if(marker.getTitle().equals(name))
                    return true;
            }
        } catch (JSONException e) {

        }

        return false;
    }

    public void updateFavs() {
        //TODO
        /*try {
            File dir = getFilesDir();
            FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsolutePath() + '/' + FAV_SCANS_FILE);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
            bufferedWriter.write(favorites.toString());
            bufferedWriter.close();
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }*/
    }
}

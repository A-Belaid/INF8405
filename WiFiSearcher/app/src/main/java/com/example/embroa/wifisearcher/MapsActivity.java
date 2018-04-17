package com.example.embroa.wifisearcher;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.widget.EditText;
import android.widget.Toast;
import android.database.sqlite.*;

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
        initFavDatabase();
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

        //Place a marker for the current Network
        LatLng currentNet = new LatLng(latitude, longitude);
        MarkerOptions currentMarker = new MarkerOptions().position(currentNet).title(netName);
        currentMarker.icon(BitmapDescriptorFactory.fromResource(isMarkerOptionsFavorite(currentMarker) ? R.drawable.fav_wifi : R.drawable.current_wifi));
        mMap.addMarker(currentMarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentNet));

        //Place markers for the other networks
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

        //Place markers for the favorite networks
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

    //Creates an alert containing the data related to the network of the selected marker,
    //as well as options depending on the nature of the network (Current Network, Favorite Network, Other Network)
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

        final MapsActivity thisActivity = this;
        alertTitle = marker.getTitle();

        final AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
        builder.setTitle(alertTitle).setMessage(isRecentOs ? Html.fromHtml(alertMessage, Html.FROM_HTML_MODE_LEGACY) : Html.fromHtml(alertMessage))
                .setNegativeButton("OK", null);

        if(alertTitle.equals(netName)) {
            builder.setNegativeButton("Partager", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean hasSMSPermission = (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
                    if (!hasSMSPermission) {
                        ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.SEND_SMS}, 0);
                    }

                    hasSMSPermission = (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
                    if(hasSMSPermission) {
                        final EditText phoneNumberTxt = new EditText(thisActivity);

                        AlertDialog.Builder smsBuilder = new AlertDialog.Builder(thisActivity);
                        smsBuilder.setTitle("Partager").setMessage("Entrez le numéro de la personne avec laquelle vous souhaitez partager ce réseau:")
                                .setView(phoneNumberTxt)
                                .setNegativeButton("Annuler", null)
                                .setPositiveButton("Valider", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String smsMessage = "Viens voir! Je viens de trouver le wifi suivant!\n" +
                                                "Nom: " + marker.getTitle() +
                                                "\nLatitude: " + String.valueOf(latitude) +
                                                "\nLongitude: " + String.valueOf(longitude);

                                        SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.sendTextMessage(phoneNumberTxt.getText().toString(), null, smsMessage, null, null);
                                    }
                                });
                        smsBuilder.create().show();
                    }
                }
            });
            if(isMarkerFavorite(marker)) {
                builder.setPositiveButton("Retirer des Favs.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeFromFavs(getFavIndex(marker.getTitle()));
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.current_wifi));
                    }
                });
            } else {
                builder.setPositiveButton("Ajouter aux Favs.", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            JSONObject newFav = new JSONObject();
                            newFav.put("Name", getSelectedMarkerScan(marker).SSID);
                            newFav.put("Mac", getSelectedMarkerScan(marker).BSSID);
                            newFav.put("Caps", getSelectedMarkerScan(marker).capabilities);
                            newFav.put("Lat", latitude);
                            newFav.put("Lng", longitude);
                            addToFavs(newFav);
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
                }
            });
        }

        return builder;
    }

    //Gets the data related to the network of the selected marker
    public ScanResult getSelectedMarkerScan(Marker marker) {
        for(ScanResult scanResult: MainActivity.wifiResults) {
            String name = "\"" + scanResult.SSID + "\"";
            if(marker.getTitle().equals(name))
                return scanResult;
        }

        return null;
    }

    //Gets the index of a network's data in the favorites array
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

    public void initFavDatabase() {
        SQLiteDatabase db = openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS FAVORITES(NAME VARCHAR(25)," +
                "MAC VARCHAR(25)," +
                "CAPS VARCHAR(255)," +
                "LAT DOUBLE," +
                "LNG DOUBLE," +
                "CONSTRAINT PK_COORDS PRIMARY KEY (LAT, LNG))");

        Cursor favs = db.rawQuery("SELECT * FROM FAVORITES", null);
        favs.moveToFirst();
        while(!favs.isAfterLast()) {
            try {
                JSONObject oneFav = new JSONObject();
                oneFav.put("Name", favs.getString(favs.getColumnIndex("NAME")));
                oneFav.put("Mac", favs.getString(favs.getColumnIndex("MAC")));
                oneFav.put("Caps", favs.getString(favs.getColumnIndex("CAPS")));
                oneFav.put("Lat", favs.getDouble(favs.getColumnIndex("LAT")));
                oneFav.put("Lng", favs.getDouble(favs.getColumnIndex("LNG")));
                favorites.put(oneFav);
                favs.moveToNext();
            } catch(JSONException e) {}
        }

        db.close();
    }

    public void addToFavs(JSONObject oneFav) {
        SQLiteDatabase db = openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
        try {
            db.execSQL("INSERT INTO FAVORITES VALUES('" + oneFav.getString("Name") + "','" +
                    oneFav.getString("Mac") + "','" +
                    oneFav.getString("Caps") + "'," +
                    String.valueOf(oneFav.getDouble("Lat")) + "," +
                    String.valueOf(oneFav.getDouble("Lng")) + ")");

            favorites.put(oneFav);
        } catch (JSONException e) {}

        db.close();
    }

    public void removeFromFavs(int favIndex) {
        try {
            JSONObject favToDelete = (JSONObject) favorites.get(favIndex);
            double lat = favToDelete.getDouble("Lat");
            double lng = favToDelete.getDouble("Lng");

            SQLiteDatabase db = openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
            db.execSQL("DELETE FROM FAVORITES WHERE LAT = " + String.valueOf(lat) + " AND LNG = " + String.valueOf(lng) + "");
            favorites.remove(favIndex);
            db.close();
        } catch(JSONException e) {}
    }
}

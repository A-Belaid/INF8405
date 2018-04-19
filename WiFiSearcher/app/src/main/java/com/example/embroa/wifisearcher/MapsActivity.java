package com.example.embroa.wifisearcher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.widget.EditText;
import android.database.sqlite.*;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.os.AsyncTask;
import android.graphics.Color;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import com.google.android.gms.location.LocationServices;

import android.location.Location;

import com.google.android.gms.tasks.*;

import android.support.annotation.NonNull;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private String netName;
    private double latitude;
    private double longitude;
    private ArrayList<LatLng> locations = new ArrayList();
    private JSONArray favorites;

    final private String FAV_SCANS_FILE = "FavScans.json";

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLastKnownLocation;
    private Marker userMarker;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Marker selectedMarker;
    private Polyline polyLinePath;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BroadcastReceiver batteryLevelReceiver;

        BatteryHistory.initHistory("MapsActivity");
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryHistory.callbackOnReceive(intent, getProjectDB());
            }
        };

        registerReceiver(batteryLevelReceiver, batteryLevelFilter);

        Intent intent = getIntent();
        netName = intent.getStringExtra("NAME");
        latitude = intent.getDoubleExtra("LAT", 0);
        longitude = intent.getDoubleExtra("LONG", 0);

        favorites = new JSONArray();
        initFavDatabase();

        selectedMarker = null;
        polyLinePath = null;
        userMarker = null;
        mLastKnownLocation = null;
        // Construct a FusedLocationProviderClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Location request
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); // PRIORITY_HIGH_ACCURACY crashed the app, ACCESS_FINE_LOCATION

        //Location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    if (mLastKnownLocation != location) {
                        mLastKnownLocation = location;
                        userMarker.setPosition(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
                    }
                }
            }
        };

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        BatteryHistory.initHistory("MapsActivity");
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
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Place a marker for the current Network
        LatLng currentNet = new LatLng(latitude, longitude);
        MarkerOptions currentMarker = new MarkerOptions().position(currentNet).title(netName);
        currentMarker.icon(BitmapDescriptorFactory.fromResource(isMarkerOptionsFavorite(currentMarker) ? R.drawable.fav_wifi : R.drawable.current_wifi));
        mMap.addMarker(currentMarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentNet, 15));

        //Place markers for the other networks
        double randLat = latitude;
        double randLng = longitude;
        for (ScanResult wifiResult : MainActivity.wifiResults) {
            String wifiName = "\"" + wifiResult.SSID + "\"";
            if (!wifiName.equals(netName)) {
                randLat += (Math.random() * 2 - 1) / 1000;
                randLng += (Math.random() * 2 - 1) / 500;
                LatLng netcoords = new LatLng(randLat, randLng);
                boolean isNetworkPrivate = wifiResult.capabilities.contains("WPA") || wifiResult.capabilities.contains("TKIP");
                int signalLevel = WifiManager.calculateSignalLevel(wifiResult.level, 3);
                mMap.addMarker(new MarkerOptions().position(netcoords).title(wifiName).icon(BitmapDescriptorFactory.fromResource(getMarkerColor(isNetworkPrivate, signalLevel))));
            }
        }

        //Place markers for the favorite networks
        for (int i = 0; i < favorites.length(); i++) {
            try {
                JSONObject obj = (JSONObject) favorites.get(i);
                String wifiName = "\"" + obj.getString("Name") + "\"";
                if (!wifiName.equals(netName)) {
                    LatLng netcoords = new LatLng(obj.getDouble("Lat"), obj.getDouble("Lng"));
                    mMap.addMarker(new MarkerOptions().position(netcoords).title(wifiName).icon(BitmapDescriptorFactory.fromResource(R.drawable.fav_wifi)));
                }
            } catch (JSONException e) {

            }
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                getMarkerAlert(marker).create().show();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker){
                //If user clicks on an already selected marker
                if(marker.equals(selectedMarker)) {
                    selectedMarker = null;

                    polyLinePath.remove();
                    polyLinePath = null;

                    marker.hideInfoWindow();
                }//If a marker is already selected and user clicks on another marker
                else if (polyLinePath != null) {
                    polyLinePath.remove();
                    polyLinePath = null;
                    selectedMarker.hideInfoWindow();

                    selectedMarker = marker;
                    drawPath(selectedMarker);

                    marker.showInfoWindow();

                }//If no marker is already selected
                else if(!marker.equals(selectedMarker)) {
                    selectedMarker = marker;
                    drawPath(selectedMarker);

                    marker.showInfoWindow();
                }
                return true;
            }
        });
        // Get the current location of the device and set the position of the map.
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mLastKnownLocation = location;
                            MarkerOptions userMarkerOptions = new MarkerOptions().position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
                            userMarker = mMap.addMarker(userMarkerOptions);
                        }
                    }
                });

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null /* Looper */);
    }

    public int getMarkerColor(boolean isLocked, int signalLevel) {
        if (isLocked) return getLockedMarkerColor(signalLevel);
        else return getUnlockedMarkerColor(signalLevel);
    }

    public int getLockedMarkerColor(int signalLevel) {
        switch (signalLevel) {
            case 3:
                return R.drawable.private_wifi_3;
            case 2:
                return R.drawable.private_wifi_2;
            default:
                return R.drawable.private_wifi_1;
        }
    }

    public int getUnlockedMarkerColor(int signalLevel) {
        switch (signalLevel) {
            case 3:
                return R.drawable.free_wifi_3;
            case 2:
                return R.drawable.free_wifi_2;
            default:
                return R.drawable.free_wifi_1;
        }
    }

    //Creates an alert containing the data related to the network of the selected marker,
    //as well as options depending on the nature of the network (Current Network, Favorite Network, Other Network)
    public AlertDialog.Builder getMarkerAlert(final Marker marker) {
        boolean isRecentOs = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
        String alertTitle = "";
        String alertMessage = "";

        if (isMarkerFavorite(marker)) {
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

        if (alertTitle.equals(netName)) {
            builder.setNegativeButton("Partager", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    boolean hasSMSPermission = (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
                    if (!hasSMSPermission) {
                        ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.SEND_SMS}, 0);
                    }

                    hasSMSPermission = (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED);
                    if (hasSMSPermission) {
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
            if (isMarkerFavorite(marker)) {
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
        } else if (isMarkerFavorite(marker)) {
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
        for (ScanResult scanResult : MainActivity.wifiResults) {
            String name = "\"" + scanResult.SSID + "\"";
            if (marker.getTitle().equals(name))
                return scanResult;
        }

        return null;
    }

    //Gets the index of a network's data in the favorites array
    public int getFavIndex(String name1) {
        try {
            for (int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name2 = "\"" + obj.getString("Name") + "\"";
                if (name1.equals(name2))
                    return i;
            }
        } catch (JSONException e) {

        }

        return -1;
    }

    public boolean isMarkerFavorite(Marker marker) {
        try {
            for (int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name = "\"" + obj.getString("Name") + "\"";
                if (marker.getTitle().equals(name))
                    return true;
            }
        } catch (JSONException e) {

        }

        return false;
    }

    public boolean isMarkerOptionsFavorite(MarkerOptions marker) {
        try {
            for (int i = 0; i < favorites.length(); i++) {
                JSONObject obj = (JSONObject) favorites.get(i);
                String name = "\"" + obj.getString("Name") + "\"";
                if (marker.getTitle().equals(name))
                    return true;
            }
        } catch (JSONException e) {

        }

        return false;
    }

    public void initFavDatabase() {
        SQLiteDatabase db = getProjectDB();
        db.execSQL("CREATE TABLE IF NOT EXISTS FAVORITES(NAME VARCHAR(25)," +
                "MAC VARCHAR(25)," +
                "CAPS VARCHAR(255)," +
                "LAT DOUBLE," +
                "LNG DOUBLE," +
                "CONSTRAINT PK_COORDS PRIMARY KEY (LAT, LNG))");

        Cursor favs = db.rawQuery("SELECT * FROM FAVORITES", null);
        favs.moveToFirst();
        while (!favs.isAfterLast()) {
            try {
                JSONObject oneFav = new JSONObject();
                oneFav.put("Name", favs.getString(favs.getColumnIndex("NAME")));
                oneFav.put("Mac", favs.getString(favs.getColumnIndex("MAC")));
                oneFav.put("Caps", favs.getString(favs.getColumnIndex("CAPS")));
                oneFav.put("Lat", favs.getDouble(favs.getColumnIndex("LAT")));
                oneFav.put("Lng", favs.getDouble(favs.getColumnIndex("LNG")));
                favorites.put(oneFav);
                favs.moveToNext();
            } catch (JSONException e) {
            }
        }

        db.close();
    }

    public void addToFavs(JSONObject oneFav) {
        SQLiteDatabase db = getProjectDB();
        try {
            db.execSQL("INSERT INTO FAVORITES VALUES('" + oneFav.getString("Name") + "','" +
                    oneFav.getString("Mac") + "','" +
                    oneFav.getString("Caps") + "'," +
                    String.valueOf(oneFav.getDouble("Lat")) + "," +
                    String.valueOf(oneFav.getDouble("Lng")) + ")");

            favorites.put(oneFav);
        } catch (JSONException e) {
        }

        db.close();
    }

    public void removeFromFavs(int favIndex) {
        try {
            JSONObject favToDelete = (JSONObject) favorites.get(favIndex);
            double lat = favToDelete.getDouble("Lat");
            double lng = favToDelete.getDouble("Lng");

            SQLiteDatabase db = getProjectDB();
            db.execSQL("DELETE FROM FAVORITES WHERE LAT = " + String.valueOf(lat) + " AND LNG = " + String.valueOf(lng) + "");
            favorites.remove(favIndex);
            db.close();
        } catch (JSONException e) {
        }
    }

    public SQLiteDatabase getProjectDB() {
        return openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
    }

    /************** Direction path **************/
    public void drawPath(Marker selectedMarker) {
        String url = getDirectionsUrl(new LatLng(mLastKnownLocation.getLatitude(),
                mLastKnownLocation.getLongitude()), selectedMarker.getPosition());

        ReadTask downloadTask = new ReadTask();
        downloadTask.execute(url);
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=walking";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(2);
                polyLineOptions.color(Color.BLUE);
            }

            polyLinePath = mMap.addPolyline(polyLineOptions);
        }
    }

    public String readUrl(String mapsApiDirectionsUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(mapsApiDirectionsUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception while reading url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}

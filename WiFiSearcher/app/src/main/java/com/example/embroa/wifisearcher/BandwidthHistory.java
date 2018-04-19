package com.example.embroa.wifisearcher;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class BandwidthHistory {
    private static final long KB_TO_BYTE = 1024;
    private static final long MB_TO_BYTE = (long) Math.pow(1024, 2);

    private BandwidthHistory() {

    }

    public static long initHistory(SQLiteDatabase projectDB, String activity, long tx, long rx) {
        long startStamp = System.currentTimeMillis();

        projectDB.execSQL("INSERT INTO BANDWIDTH VALUES(" + String.valueOf(startStamp) + ",'" +
                activity + "'," +
                String.valueOf(tx) + "," +
                String.valueOf(rx) + ")");

        projectDB.close();

        return startStamp;
    }

    public static void endHistory(SQLiteDatabase projectDB, long startStamp, long tx, long rx) {
        Cursor cursor = projectDB.rawQuery("SELECT DELTA_TX, DELTA_RX FROM BANDWIDTH WHERE TIMESTAMP=" + String.valueOf(startStamp), null);
        cursor.moveToFirst();

        long deltaTx = tx - cursor.getLong(0);
        long deltaRx = rx - cursor.getLong(1);

        projectDB.execSQL("UPDATE BANDWIDTH SET DELTA_TX=" + String.valueOf(deltaTx) + ",DELTA_RX=" + String.valueOf(deltaRx) +
                " WHERE TIMESTAMP=" + String.valueOf(startStamp));

        projectDB.close();
    }

    public static void clearHistory(SQLiteDatabase projectDB) {
        projectDB.execSQL("DELETE FROM BANDWIDTH");
        projectDB.close();
    }

    public static ArrayList<String> getHistory(SQLiteDatabase projectDB) {
        long currentStamp = System.currentTimeMillis();
        ArrayList<String> history = new ArrayList<String>();

        Cursor hist = projectDB.rawQuery("SELECT * FROM BANDWIDTH", null);
        hist.moveToFirst();

        while (!hist.isAfterLast()) {
            long stampDelta = currentStamp - hist.getLong(hist.getColumnIndex("TIMESTAMP"));
            String stampDeltaStr = BatteryHistory.convertDeltaStamp(stampDelta);
            String activity = hist.getString(hist.getColumnIndex("ACTIVITY"));
            String deltaTx = convertBandwidth(hist.getLong(hist.getColumnIndex("DELTA_TX")));
            String deltaRx = convertBandwidth(hist.getLong(hist.getColumnIndex("DELTA_RX")));

            history.add(stampDeltaStr + ": " + activity + "(" + deltaTx + " ↑ " + deltaRx + " ↓)");

            hist.moveToNext();
        }

        projectDB.close();
        return history;
    }

    private static String convertBandwidth(long band) {
        long convertedBand = band;

        if(band >= MB_TO_BYTE) {
            convertedBand = (long) Math.floor(band / MB_TO_BYTE);
            return String.valueOf(convertedBand) + " Mo";
        }

        if(band >= KB_TO_BYTE) {
            convertedBand = (long) Math.floor(band / KB_TO_BYTE);
            return String.valueOf(convertedBand) + " ko";
        }

        return String.valueOf(convertedBand) + " o";
    }
}

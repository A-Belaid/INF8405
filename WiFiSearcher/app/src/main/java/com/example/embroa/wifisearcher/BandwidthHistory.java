package com.example.embroa.wifisearcher;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BandwidthHistory {
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
}

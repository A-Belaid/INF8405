package com.example.embroa.wifisearcher;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.BatteryManager;

import java.util.ArrayList;

public class BatteryHistory {
    private long startStamp;
    private float startLevel;
    private float currentLevel;
    private String currentActivity;

    private final static long MILLISECS_IN_SEC = 1000;
    private final static long MILLISECS_IN_MIN = 60000;
    private final static long MILLISECS_IN_HOUR = 3600000;
    private final static long MILLISECS_IN_DAY = 86400000;

    private static BatteryHistory battery = new BatteryHistory();

    private BatteryHistory() {
        startLevel = -1;
        currentLevel = -1;
    }

    public static void initHistory(String activityName) {
        battery.startStamp = System.currentTimeMillis();
        battery.startLevel = -1;
        battery.currentLevel = -1;
        battery.currentActivity = activityName;
    }

    public static void updateLevel(SQLiteDatabase projectDB, float level) {
        if(battery.startLevel < 0) {
            battery.startLevel = level;
            projectDB.execSQL("INSERT INTO BATTERY VALUES(" + String.valueOf(battery.startStamp) + ",'" +
                    battery.currentActivity + "', 0)");
        }

        battery.currentLevel = level;

        float delta = battery.startLevel - battery.currentLevel;
        if(delta < 0) delta = 0;

        projectDB.execSQL("UPDATE BATTERY SET DELTA= " + String.valueOf(delta) +
                " WHERE TIMESTAMP=" + String.valueOf(battery.startStamp) +
                " AND ACTIVITY='" + battery.currentActivity + "'");

        projectDB.close();
    }

    public static ArrayList<String> getHistory(SQLiteDatabase projectDB) {
        long currentStamp = System.currentTimeMillis();
        ArrayList<String> history = new ArrayList<String>();

        Cursor hist = projectDB.rawQuery("SELECT * FROM BATTERY", null);
        hist.moveToFirst();

        while(!hist.isAfterLast()) {
            long stampDelta = currentStamp - hist.getLong(hist.getColumnIndex("TIMESTAMP"));
            String stampDeltaStr = battery.convertDeltaStamp(stampDelta);
            String activity = hist.getString(hist.getColumnIndex("ACTIVITY"));
            String btrDelta = String.valueOf(hist.getFloat(hist.getColumnIndex("DELTA")));

            history.add(stampDeltaStr + ": " + activity + " (" + btrDelta + "%)");

            hist.moveToNext();
        }

        return history;
    }

    public static void clearHistory(SQLiteDatabase projectDB) {
        projectDB.execSQL("DELETE FROM BATTERY");
        projectDB.close();
    }

    public static String convertDeltaStamp(long deltaStamp) {
        if(deltaStamp >= MILLISECS_IN_DAY) {
            long deltaDays = deltaStamp / MILLISECS_IN_SEC;
            return "Il y a " + String.valueOf(deltaDays) + " j";
        }

        if(deltaStamp >= MILLISECS_IN_HOUR) {
            long deltaHours = deltaStamp / MILLISECS_IN_HOUR;
            return "Il y a " + String.valueOf(deltaHours) + " h";
        }

        if(deltaStamp >= MILLISECS_IN_MIN) {
            long deltaMins = deltaStamp / MILLISECS_IN_MIN;
            return "Il y a " + String.valueOf(deltaMins) + " mn";
        }

        if(deltaStamp >= MILLISECS_IN_SEC) {
            long deltaSecs = deltaStamp / MILLISECS_IN_SEC;
            return "Il y a " + String.valueOf(deltaSecs) + " s";
        }

        return "Il y a 0 s";
    }

    public static float callbackOnReceive(Intent intent, SQLiteDatabase projectDB) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float battPct = (level/(float)scale) * 100;
        battery.updateLevel(projectDB, battPct);
        return battPct;
    }
}

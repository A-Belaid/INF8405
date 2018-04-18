package com.example.embroa.wifisearcher;

import android.database.sqlite.*;

import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;

public class BatteryHistory {
    private long startStamp;
    private float startLevel;
    private float currentLevel;
    private String currentActivity;

    private static BatteryHistory battery = new BatteryHistory();

    private BatteryHistory() {

    }

    public static void initHistory(String activityName) {
        battery.startStamp = System.currentTimeMillis();
        battery.startLevel = -1;
        battery.currentLevel = -1;
        battery.currentActivity = activityName;
    }

    public static void updateLevel(float level) {
        if(battery.startLevel < 0) {}
            battery.startLevel = level;

        battery.currentLevel = level;
    }

    public static void endHistory(SQLiteDatabase projectDB) {
        float delta = battery.startLevel - battery.currentLevel;
        if(delta < 0) delta = 0;

        projectDB.execSQL("INSERT INTO BATTERY VALUES(" + String.valueOf(battery.startStamp) + ",'" +
                battery.currentActivity + "'," +
                String.valueOf(delta) + ")");

        projectDB.close();
    }
}

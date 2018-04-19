

/**
 * Created by User on 2018-04-04.
 */

package com.example.embroa.wifisearcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class StepCounterActivity extends Activity {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView textView;
    private float stepOffset;

    private boolean isStepSensorSupported(){
        // Require at least Android KitKat
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        // Check that the device supports the step counter and detector sensors
        PackageManager packageManager = getPackageManager();
        return currentApiVersion >= android.os.Build.VERSION_CODES.KITKAT
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);
        textView = (TextView) findViewById(R.id.text_stepCount);
        textView.setText("");
        findViewById(R.id.stopBtn).setVisibility(View.INVISIBLE);
        //findViewById(R.id.resetBtn).setVisibility(View.INVISIBLE);

        final IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BroadcastReceiver batteryLevelReceiver;

        BatteryHistory.initHistory("StepCounterActivity");
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BatteryHistory.callbackOnReceive(intent, getProjectDB());
            }
        };

        registerReceiver(batteryLevelReceiver, batteryLevelFilter);

        if(isStepSensorSupported()) {
            findViewById(R.id.startBtn).setVisibility(View.VISIBLE);
        } else {
            textView.setText("Cette application n'est pas supportée.");
            findViewById(R.id.startBtn).setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.startBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCounter();
            }
        });

        findViewById(R.id.stopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCounter();
            }
        });

        /*findViewById(R.id.resetBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetCounter();
            }
        });*/
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        BatteryHistory.initHistory("StepCounterActivity");
    }


    protected void startCounter() {
        textView.setText("");
        findViewById(R.id.stopBtn).setVisibility(View.VISIBLE);
        //findViewById(R.id.resetBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.startBtn).setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);

        stepOffset = 0;
    }

    protected void stopCounter() {
        sensorManager.unregisterListener(sensorEventListener, stepSensor);

        //findViewById(R.id.stopBtn).setVisibility(View.INVISIBLE);
        //findViewById(R.id.resetBtn).setVisibility(View.INVISIBLE);
        //findViewById(R.id.startBtn).setVisibility(View.VISIBLE);

        // measure heart rate after moving
        Intent myIntent = new Intent(this, HeartRateMonitorActivity.class);
        myIntent.putExtra("isLast", true);
        /*MainActivity.this.*/startActivity(myIntent);
    }

    /*protected void resetCounter() {
        sensorManager.unregisterListener(sensorEventListener, stepSensor);
        startCounter();
    }*/

    public SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (stepOffset == 0) {
                stepOffset = event.values[0];
            }
            textView.setText(Float.toString(event.values[0] - stepOffset));
        }
    };

    public SQLiteDatabase getProjectDB() {
        return openOrCreateDatabase("INF8405", MODE_PRIVATE, null);
    }
}

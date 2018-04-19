package com.example.embroa.wifisearcher;

/**
 * Created by Emilie on 2018-04-13.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class HeartRateMonitorActivity extends Activity {
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private TextView textView;
    private int timesChanged = 0;
    private boolean isDone = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_monitor);
        textView = (TextView) findViewById(R.id.text_heartRate);
        textView.setText("");
        startMonitor();
        findViewById(R.id.okBtn).setVisibility(View.INVISIBLE);

        findViewById(R.id.okBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopMonitor();
            }
        });
    }
    protected void startMonitor() {
        textView.setText("");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if(heartRateSensor != null)
            textView.setText("Appuyer votre doigt sur le capteur près de la caméra arrière pour mesurer votre rythme cardiaque.");
        else
            textView.setText("Erreur: capteur indisponible.");

        sensorManager.registerListener(sensorEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);

    }
    protected void stopMonitor() {
        sensorManager.unregisterListener(sensorEventListener, heartRateSensor);
        Intent nextIntent = null;
        Intent previousIntent = getIntent();

        Boolean isLast = previousIntent.getBooleanExtra("isLast", false);
        if (isLast){
            // Go back to main menu after last measure
            nextIntent = new Intent(this, MainActivity.class);
        } else {
            // Start StepCounter after first measure
            nextIntent = new Intent(this, StepCounterActivity.class);
        }

        /*MainActivity.this.*/startActivity(nextIntent);
    }

    public SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                float heartRate = event.values[0];
                if(!isDone) {
                    if (heartRate == 0) {
                        textView.setText("Calcul en cours. Gardez votre doigt sur le capteur...");
                        timesChanged = 0;
                    } else {
                        textView.setText("Rythme cardiaque: " + (int) heartRate);
                        timesChanged++;

                        if (timesChanged > 10) {
                            isDone = true;
                            findViewById(R.id.okBtn).setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }
    };
}

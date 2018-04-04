

/**
 * Created by User on 2018-04-04.
 */

package com.example.embroa.wifisearcher;

import android.app.Activity;
import android.content.pm.PackageManager;
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
        findViewById(R.id.resetBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.resumeBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.pauseBtn).setVisibility(View.INVISIBLE);

        if(isStepSensorSupported()) {
            findViewById(R.id.startBtn).setVisibility(View.VISIBLE);
        } else {
            textView.setText("Cette application n'est pas supportée.");
            findViewById(R.id.startBtn).setVisibility(View.INVISIBLE);
        }
    }
    protected void startCounter() {
        textView.setText("");
        findViewById(R.id.stopBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.resetBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.pauseBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.resumeBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.startBtn).setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    protected void resumeCounter() {
        sensorManager.registerListener(sensorEventListener, stepSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        findViewById(R.id.pauseBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.resumeBtn).setVisibility(View.INVISIBLE);
    }

    protected void pauseCounter() {
        sensorManager.unregisterListener(sensorEventListener);

        findViewById(R.id.pauseBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.resumeBtn).setVisibility(View.VISIBLE);

    }

    protected void stopCounter() {
        sensorManager.unregisterListener(sensorEventListener);

        findViewById(R.id.stopBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.resetBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.pauseBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.resumeBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.startBtn).setVisibility(View.VISIBLE);
    }

    protected void resetCounter() {
        sensorManager.unregisterListener(sensorEventListener);
        startCounter();
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        private float stepOffset;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (stepOffset == 0) {
                stepOffset = event.values[0];
            }
            textView.setText(Float.toString(event.values[0] - stepOffset));
        }
    };
}

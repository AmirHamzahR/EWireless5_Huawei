package com.example.datacollectionapp_afp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Static string to transfer the intent on another class
    public static final String DATA_TRANSFER = "data_transfer";

    // Sensors that are used
    private SensorManager sm;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Sensor magnetoSensor;
    private Sensor baroSensor;
    private Sensor lightSensor;
    private Sensor proxSensor;

    // later we add these
    // add location
    // add wifi

    // Layout Views
    TextView tv;
    ImageButton startButton;
    ImageButton stopButton;

    // Graph variables
    boolean plotNow = false;
    int timestamp = 0;
    LineGraphSeries<DataPoint> series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        initialization();
        // askPermission
        askPermission();
    }

    // Ask permissions
    private void initialization() {
        // Initialize sensors
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm == null)
            throw new AssertionError();
        accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetoSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        baroSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT); // not sure why we need light sensor lol
        proxSensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Load views
        GraphView graph = (GraphView) findViewById(R.id.graph);
    }

    private void askPermission(){
        // Ask permission for Wifi
        // Ask permission for Location
    }

    // Sensor data recording functionality method:

    // 1st
    // Basic UI to start/stop recording method

    private void onClickStartButton(){
        plotNow = true;
    }

    private void onClickEndButton(){
        plotNow = false;
    }



    // Record the timeframe of received measurements so we can synchronise the events

    // Send data to other class
    // Receive data from other class

    // Implementing the methods for sensor accuracy, resume, pause
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                break;
            case Sensor.TYPE_GYROSCOPE:
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                break;
            case Sensor.TYPE_PRESSURE:
                break;
            case Sensor.TYPE_LIGHT:
                break;
            case Sensor.TYPE_PROXIMITY:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // for sensors with 100 samples in microseconds
        int rate100S = (int) (1/1e-6)/100;
        sm.registerListener(this,accelSensor,rate100S);
        sm.registerListener(this, gyroSensor,rate100S);
        sm.registerListener(this,magnetoSensor,rate100S);
        // for sensors with 1 sample
        int rate1S = (int) (1/1e-6)/1;
        sm.registerListener(this,baroSensor,rate1S);
        sm.registerListener(this,lightSensor,rate1S);
        sm.registerListener(this,proxSensor,rate1S);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this,accelSensor);
        sm.unregisterListener(this, gyroSensor);
        sm.unregisterListener(this,magnetoSensor);
        sm.unregisterListener(this,baroSensor);
        sm.unregisterListener(this,lightSensor);
        sm.unregisterListener(this,proxSensor);
    }
}
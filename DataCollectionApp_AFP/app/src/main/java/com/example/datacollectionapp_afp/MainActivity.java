package com.example.datacollectionapp_afp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
    private LineGraphSeries<DataPoint> accelX;
    private LineGraphSeries<DataPoint> accelY;
    private LineGraphSeries<DataPoint> accelZ;
    private long lastUpdateTimestamp = 0;
    private long startTime = -1;
    private double lastX = 0;

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

        graphViews();

    }

    private void graphViews(){
        // Load views
        GraphView accelGraph = findViewById(R.id.graph);

        // Configure viewport settings
        accelGraph.getViewport().setXAxisBoundsManual(true);
        accelGraph.getViewport().setMinX(0);
        accelGraph.getViewport().setMaxX(1); // Adjust this value to set the viewport width
        accelGraph.getViewport().setScrollable(true); // Enables horizontal scrolling

        // data from accelerometer
        accelX = new LineGraphSeries<>();
        accelX.setColor(Color.RED);
        accelX.setThickness(8);
        accelGraph.addSeries(accelX);

        accelY = new LineGraphSeries<>();
        accelY.setColor(Color.GREEN);
        accelY.setThickness(8);
        accelGraph.addSeries(accelY);

        accelZ = new LineGraphSeries<>();
        accelZ.setColor(Color.BLUE);
        accelZ.setThickness(8);
        accelGraph.addSeries(accelZ);
    }

    // add random data to graph
    private void addEntry(double elapsedTime, float x, float y, float z) {
        accelX.appendData(new DataPoint(elapsedTime, x), true, 100);
        accelY.appendData(new DataPoint(elapsedTime, y), true, 100);
        accelZ.appendData(new DataPoint(elapsedTime, z), true, 100);
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

    private int samplesCount = 0;

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();


        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:// Desired interval in milliseconds (10ms for 100 samples per second
                double elapsedTimeInSeconds = (currentTime - startTime)  / 1000.0;
                // Update the graph
                lastX += (1.0 / 100.0); // Increment lastX by 1/100 for each sample
                addEntry(lastX, event.values[0], event.values[1], event.values[2]);
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
        // real time data with thread that append data to the graph
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                // we add 100 new entries
//                for(int i =0; i<100; i++){
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            addEntry();
//                        }
//                    });
//
//                    // sleep to slow down the add of entries
//                    try {
//                        Thread.sleep(600);
//                    } catch (InterruptedException e) {
//                        // manage error ...
//                        // Now we can see the result on emulator
//                    }
//                }
//            }
//        }).start();

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
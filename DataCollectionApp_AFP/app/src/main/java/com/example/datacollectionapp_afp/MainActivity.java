package com.example.datacollectionapp_afp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    // For recording the sensor values
    private boolean sensorRunning = false;

    // Layout Views
    private long lastUpdate = 0;
    TextView tv;
    TextView accelData;
    TextView gyroData;
    TextView magnetoData;
    TextView baroData;
    TextView lightData;
    TextView proxData;
    Button startButton;
    Button stopButton;

    // Strings for Views
    String accelStr;
    String gyroStr;
    String magnetoStr;
    String baroStr;
    String lightStr;
    String proxStr;

    // Graph variables
    // accelerometer
    private LineGraphSeries<DataPoint> accelX;
    private LineGraphSeries<DataPoint> accelY;
    private LineGraphSeries<DataPoint> accelZ;
    // gyroscope
    private LineGraphSeries<DataPoint> gyroX;
    private LineGraphSeries<DataPoint> gyroY;
    private LineGraphSeries<DataPoint> gyroZ;
    // magnetometer
    private LineGraphSeries<DataPoint> magnetoX;
    private LineGraphSeries<DataPoint> magnetoY;
    private LineGraphSeries<DataPoint> magnetoZ;
    // barometer
    private LineGraphSeries<DataPoint> baroSeries;
    // ambient light sensor
    private LineGraphSeries<DataPoint> lightSeries;
    // proximity sensor
    private LineGraphSeries<DataPoint> proxSeries;

    // Timestamps for each sensors (they are the same but the difference is due to graph plotting)
    private Chronometer mChronometer;
    private double accelTimestamp= 0.0;
    private double gyroTimestamp = 0.0;
    private double magnetoTimestamp = 0.0;
    private double baroTimestamp = 0.0;
    private double lightTimestamp = 0.0;
    private double proxTimestamp = 0.0;



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

        //Views
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        accelData = findViewById(R.id.accelData);
        gyroData = findViewById(R.id.gyroData);
        magnetoData = findViewById(R.id.magnetoData);
        baroData = findViewById(R.id.baroData);
        lightData = findViewById(R.id.lightData);
        proxData = findViewById(R.id.proxData);

        //Time settings
        mChronometer = findViewById(R.id.chronometer);

        sensorsInformation();
        graphViews();
    }

    // Checks the permission to be used
    public void checkBodyActivityPermissions(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, Sensor.TYPE_STEP_COUNTER);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BODY_SENSORS}, Sensor.TYPE_GYROSCOPE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void sensorsInformation(){
    // Accelerometer sensor information
        if (accelSensor != null) {
            accelStr =" Accelerometer: "+accelSensor.getName() +
                    " \n Manufacturer: "+accelSensor.getVendor()
                    +", Version: "+accelSensor.getVersion()
                    +", Type:"+accelSensor.getType()
                    +", Resolution: "+accelSensor.getResolution()+" m/s^2"
                    +", Max. Range: "+accelSensor.getMaximumRange()+" m/s^2"
                    +", Power consumption: "+accelSensor.getPower()+" mA"
                    +", Min. Delay: "+accelSensor.getMinDelay();
        } else {
            accelStr =" No Accelerometer detected";
            accelData.setBackgroundColor(0xFFFF0000);  // red color
        }
        accelData.setText(accelStr);

        // Gyroscope sensor information
        if (gyroSensor != null) {
            gyroStr = " Gyroscope: " + gyroSensor.getName() +
                    " \n Manufacturer: " + gyroSensor.getVendor()
                    + ", Version: " + gyroSensor.getVersion()
                    + ", Type:" + gyroSensor.getType()
                    + ", Resolution: " + gyroSensor.getResolution() + " rad/s"
                    + ", Max. Range: " + gyroSensor.getMaximumRange() + " rad/s"
                    + ", Power consumption: " + gyroSensor.getPower() + " mA"
                    + ", Min. Delay: " + gyroSensor.getMinDelay();
        } else {
            gyroStr = " No Gyroscope detected";
            gyroData.setBackgroundColor(0xFFFF0000); // red color
        }
        gyroData.setText(gyroStr);

        // Magnetometer sensor information
        if (magnetoSensor != null) {
            magnetoStr = " Magnetometer: " + magnetoSensor.getName() +
                    " \n Manufacturer: " + magnetoSensor.getVendor()
                    + ", Version: " + magnetoSensor.getVersion()
                    + ", Type:" + magnetoSensor.getType()
                    + ", Resolution: " + magnetoSensor.getResolution() + " uT"
                    + ", Max. Range: " + magnetoSensor.getMaximumRange() + " uT"
                    + ", Power consumption: " + magnetoSensor.getPower() + " mA"
                    + ", Min. Delay: " + magnetoSensor.getMinDelay();
        } else {
            magnetoStr = " No Magnetometer detected";
            magnetoData.setBackgroundColor(0xFFFF0000); // red color
        }
        magnetoData.setText(magnetoStr);

        // Barometer sensor information
        if (baroSensor != null) {
            baroStr = " Barometer: " + baroSensor.getName() +
                    " \n Manufacturer: " + baroSensor.getVendor()
                    + ", Version: " + baroSensor.getVersion()
                    + ", Type:" + baroSensor.getType()
                    + ", Resolution: " + baroSensor.getResolution() + " hPa"
                    + ", Max. Range: " + baroSensor.getMaximumRange() + " hPa"
                    + ", Power consumption: " + baroSensor.getPower() + " mA"
                    + ", Min. Delay: " + baroSensor.getMinDelay();
        } else {
            baroStr = " No Barometer detected";
            baroData.setBackgroundColor(0xFFFF0000); // red color
        }
        baroData.setText(baroStr);

        // Light sensor information
        if (lightSensor != null) {
            lightStr =" Light Sensor: "+lightSensor.getName() +
                    " \n Manufacturer: "+lightSensor.getVendor()
                    +", Version: "+lightSensor.getVersion()
                    +", Type:"+lightSensor.getType()
                    +", Resolution: "+lightSensor.getResolution()+" lx"
                    +", Max. Range: "+lightSensor.getMaximumRange()+" lx"
                    +", Power consumption: "+lightSensor.getPower()+" mA"
                    +", Min. Delay: "+lightSensor.getMinDelay();
        } else {
            lightStr =" No Light Sensor detected";
            lightData.setBackgroundColor(0xFFFF0000); // red color
        }
        lightData.setText(lightStr);

        // Proximity sensor information
        if (proxSensor != null) {
            proxStr =" Proximity Sensor: "+proxSensor.getName() +
                    " \n Manufacturer: "+proxSensor.getVendor()
                    +", Version: "+proxSensor.getVersion()
                    +", Type:"+proxSensor.getType()
                    +", Resolution: "+proxSensor.getResolution()+" cm"
                    +", Max. Range: "+proxSensor.getMaximumRange()+" cm"
                    +", Power consumption: "+proxSensor.getPower()+" mA"
                    +", Min. Delay: "+proxSensor.getMinDelay();
        } else {
            proxStr =" No Proximity Sensor detected";
            proxData.setBackgroundColor(0xFFFF0000); // red color
        }
        proxData.setText(proxStr);
    }

    private void graphViews(){
        // Load views
        GraphView accelGraph = findViewById(R.id.accelGraph);
        GraphView gyroGraph = findViewById(R.id.gyroGraph);
        GraphView magnetoGraph = findViewById(R.id.magnetoGraph);
        GraphView baroGraph = findViewById(R.id.baroGraph);
        GraphView lightGraph = findViewById(R.id.lightGraph);
        GraphView proxGraph = findViewById(R.id.proxGraph);

        // Configure viewport settings
        graphSettings(accelGraph);
        graphSettings(gyroGraph);
        graphSettings(magnetoGraph);
        graphSettings(baroGraph);
        graphSettings(lightGraph);
        graphSettings(proxGraph);

        // data from accelerometer
        accelX = new LineGraphSeries<>();
        accelY = new LineGraphSeries<>();
        accelZ = new LineGraphSeries<>();

        // Adding accelerometer values into the graph
        dataSettings(accelX, "accelX", Color.RED, 8, accelGraph);
        dataSettings(accelY, "accelY", Color.GREEN, 8, accelGraph);
        dataSettings(accelZ, "accelZ", Color.BLUE, 8, accelGraph);

        // Data from gyroscope
        gyroX = new LineGraphSeries<>();
        gyroY = new LineGraphSeries<>();
        gyroZ = new LineGraphSeries<>();

        // Adding gyroscope values into the graph
        dataSettings(gyroX, "gyroX", Color.RED, 8, gyroGraph);
        dataSettings(gyroY, "gyroY", Color.GREEN, 8, gyroGraph);
        dataSettings(gyroZ, "gyroZ", Color.BLUE, 8, gyroGraph);

        // Data from magnetometer
        magnetoX = new LineGraphSeries<>();
        magnetoY = new LineGraphSeries<>();
        magnetoZ = new LineGraphSeries<>();

        // Adding magnetometer values into the graph
        dataSettings(magnetoX, "magnetoX", Color.RED, 8, magnetoGraph);
        dataSettings(magnetoY, "magnetoY", Color.GREEN, 8, magnetoGraph);
        dataSettings(magnetoZ, "magnetoZ", Color.BLUE, 8, magnetoGraph);

        // Data from barometer
        baroSeries = new LineGraphSeries<>();

        // Adding barometer values into the graph
        dataSettings(baroSeries, "baroSeries", Color.RED, 8, baroGraph);

        // Data from light sensor
        lightSeries = new LineGraphSeries<>();

        // Adding light sensor values into the graph
        dataSettings(lightSeries, "lightSeries", Color.RED, 8, lightGraph);

        // Data from proximity sensor
        proxSeries = new LineGraphSeries<>();

        // Adding proximity sensor values into the graph
        dataSettings(proxSeries, "proxSeries", Color.RED, 8, proxGraph);
    }

    private void graphSettings(GraphView graph){
        // Configure Viewport default graph settings for 1 second real time analysis
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1); // Adjust this value to set the viewport width
        graph.getViewport().setScrollable(true); // Enables horizontal scrolling
    }

    private void dataSettings(LineGraphSeries data, String str, int color, int thickness, GraphView graph){
        data.setColor(color);
        data.setThickness(thickness);
        graph.addSeries(data);
        data.setTitle(str);
        graph.getLegendRenderer().setVisible(true);
    }

    private void askPermission(){
        // ask for activity recognition
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, Sensor.TYPE_ACCELEROMETER);
        }
        // ask for body sensors
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BODY_SENSORS}, Sensor.TYPE_GYROSCOPE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Ask permission for Wifi
        // Ask permission for Location
    }

    // Sensor data recording functionality method:

    // 1st
    // Basic UI to start/stop recording method

    public void onClickStartButton(View view){
        mChronometer.setBase(SystemClock.elapsedRealtime()); // reset to 0
        mChronometer.start();
        sensorRunning = true;

        // resetting the timestamps
        accelTimestamp = 0.0;
        gyroTimestamp = 0.0;
        magnetoTimestamp = 0.0;
        baroTimestamp = 0.0;
        lightTimestamp = 0.0;
        proxTimestamp = 0.0;

        // Clear the acceleration graph data
        accelX.resetData(new DataPoint[]{});
        accelY.resetData(new DataPoint[]{});
        accelZ.resetData(new DataPoint[]{});

        // Clear the gyroscope graph data
        gyroX.resetData(new DataPoint[]{});
        gyroY.resetData(new DataPoint[]{});
        gyroZ.resetData(new DataPoint[]{});

        // Clear the magnetometer graph data
        magnetoX.resetData(new DataPoint[]{});
        magnetoY.resetData(new DataPoint[]{});
        magnetoZ.resetData(new DataPoint[]{});

        // Clear the barometer graph data
        baroSeries.resetData(new DataPoint[]{});

        // Clear the proximity sensor graph data
        proxSeries.resetData(new DataPoint[]{});

        // Clear the ambient light sensor graph data
        lightSeries.resetData(new DataPoint[]{});

        // Unregister the sensor listener if it's already registered
        sm.unregisterListener(this);

        // Register the sensor listener
        registeringListeners();
    }

    public void onClickStopButton(View view){
        mChronometer.stop();
        // Unregister the sensor listener
        unregisteringListeners();
        if(sensorRunning){
            // Creating accelerator data
            String fileName = "accel_data";
            List<LineGraphSeries<DataPoint>> seriesList = Arrays.asList(accelX, accelY, accelZ);
            List<String> headers = Arrays.asList("Timestamp,AccelX", "Timestamp,AccelY", "Timestamp,AccelZ");
            writeDataToCsv(fileName, seriesList, headers);

            // Creating gyroscope data
            String gyroFileName = "gyro_data";
            List<LineGraphSeries<DataPoint>> gyroSeriesList = Arrays.asList(gyroX, gyroY, gyroZ);
            List<String> gyroHeaders = Arrays.asList("Timestamp,GyroX", "Timestamp,GyroY", "Timestamp,GyroZ");
            writeDataToCsv(gyroFileName, gyroSeriesList, gyroHeaders);

            // Creating magnetic field data
            String magnetoFileName = "magneto_data";
            List<LineGraphSeries<DataPoint>> magnetoSeriesList = Arrays.asList(magnetoX, magnetoY, magnetoZ);
            List<String> magnetoHeaders = Arrays.asList("Timestamp,MagnetoX", "Timestamp,MagnetoY", "Timestamp,MagnetoZ");
            writeDataToCsv(magnetoFileName, magnetoSeriesList, magnetoHeaders);

            // Creating pressure data
            String baroFileName = "baro_data";
            List<LineGraphSeries<DataPoint>> baroSeriesList = Collections.singletonList(baroSeries);
            List<String> baroHeaders = Collections.singletonList("Timestamp,Pressure");
            writeDataToCsv(baroFileName, baroSeriesList, baroHeaders);

            // Creating light data
            String lightFileName = "light_data";
            List<LineGraphSeries<DataPoint>> lightSeriesList = Collections.singletonList(lightSeries);
            List<String> lightHeaders = Collections.singletonList("Timestamp,Light");
            writeDataToCsv(lightFileName, lightSeriesList, lightHeaders);

            // Creating proximity data
            String proxFileName = "prox_data";
            List<LineGraphSeries<DataPoint>> proxSeriesList = Collections.singletonList(proxSeries);
            List<String> proxHeaders = Collections.singletonList("Timestamp,Proximity");
            writeDataToCsv(proxFileName, proxSeriesList, proxHeaders);

        }
        sensorRunning = false;
    }

    // Saving data to be stored in local
    private void writeDataToCsv(String fileName, List<LineGraphSeries<DataPoint>> seriesList, List<String> headers) {
        if (seriesList.size() != headers.size()) {
            Toast.makeText(this, "Error: Series and headers lists must have the same size", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(getExternalFilesDir(null), fileName + ".csv");
            FileWriter writer = new FileWriter(file);

            // Loop through the series list and write data points
            for (int i = 0; i < seriesList.size(); i++) {
                LineGraphSeries<DataPoint> series = seriesList.get(i);
                String header = headers.get(i);

                // Write header line
                writer.append(header);

                double minX = series.getLowestValueX(); // Get the minimum x-axis value
                double maxX = series.getHighestValueX(); // Get the maximum x-axis value

                // Get the data points as an array
                Iterator<DataPoint> dataPoints = series.getValues(minX, maxX);

                // Write the data points to the file
                while (dataPoints.hasNext()) {
                    DataPoint dataPoint = dataPoints.next();
                    writer.append(String.format(Locale.getDefault(), "%.3f,%f\n", dataPoint.getX(), dataPoint.getY()));
                }

                // Write an empty line to separate data from different sensors
                writer.append("\n");
            }

            writer.flush();
            writer.close();
            Toast.makeText(this, "Data saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving data to file", Toast.LENGTH_SHORT).show();
        }
    }

    // Implementing the methods for sensor accuracy, resume, pause
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // set to 5 hours of data points for limit sake
        int maxDataPoints = 1_800_000;
        if (sensorRunning) {
            long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
            double elapsedSeconds = elapsedMillis / 1000.0;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                        accelTimestamp = elapsedSeconds;
                        accelX.appendData(new DataPoint(accelTimestamp, event.values[0]), true,maxDataPoints);
                        accelY.appendData(new DataPoint(accelTimestamp, event.values[1]), true, maxDataPoints);
                        accelZ.appendData(new DataPoint(accelTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                        gyroTimestamp = elapsedSeconds;
                        gyroX.appendData(new DataPoint(gyroTimestamp, event.values[0]), true, maxDataPoints);
                        gyroY.appendData(new DataPoint(gyroTimestamp, event.values[1]), true, maxDataPoints);
                        gyroZ.appendData(new DataPoint(gyroTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                        magnetoTimestamp = elapsedSeconds;
                        magnetoX.appendData(new DataPoint(magnetoTimestamp, event.values[0]), true, maxDataPoints);
                        magnetoY.appendData(new DataPoint(magnetoTimestamp, event.values[1]), true, maxDataPoints);
                        magnetoZ.appendData(new DataPoint(magnetoTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_PRESSURE:
                        baroTimestamp = elapsedSeconds;
                        baroSeries.appendData(new DataPoint(baroTimestamp, event.values[0]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_LIGHT:
                        lightTimestamp = elapsedSeconds;
                        lightSeries.appendData(new DataPoint(lightTimestamp, event.values[0]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_PROXIMITY:
                        proxTimestamp = elapsedSeconds;
                        proxSeries.appendData(new DataPoint(proxTimestamp, event.values[0]), true, maxDataPoints);
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(sensorRunning) {
            registeringListeners();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisteringListeners();
    }

    private void registeringListeners(){
        // Sensors with sampling rate of 100 samples per second
        sm.registerListener(this, accelSensor, 10000);
        sm.registerListener(this, gyroSensor,10000);
        sm.registerListener(this, magnetoSensor, 10000);

        // Sensors with sampling rate of 1 sample per second
        sm.registerListener(this, baroSensor, 1000_000);
        sm.registerListener(this, lightSensor, 1000_000);
        sm.registerListener(this, proxSensor, 1000_000);
    }

    private void unregisteringListeners(){
        sm.unregisterListener(this,accelSensor);
        sm.unregisterListener(this, gyroSensor);
        sm.unregisterListener(this,magnetoSensor);
        sm.unregisterListener(this,baroSensor);
        sm.unregisterListener(this,lightSensor);
        sm.unregisterListener(this,proxSensor);
    }
}
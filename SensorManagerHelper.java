package com.example.datacollectionapp_afp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.widget.Chronometer;

import androidx.core.app.ActivityCompat;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Locale;

public class SensorManagerHelper implements SensorEventListener{
    private SensorManager sensorManager;
    private Context context;
    protected LineGraphSeries<DataPoint> accelX, accelY, accelZ;
    protected LineGraphSeries<DataPoint> gyroX, gyroY, gyroZ;
    protected LineGraphSeries<DataPoint> magnetoX, magnetoY, magnetoZ;
    protected LineGraphSeries<DataPoint> baroSeries, lightSeries, proxSeries;
    protected boolean sensorRunning = false;
    protected double accelTimestamp= 0.0;
    protected double gyroTimestamp = 0.0;
    protected double magnetoTimestamp = 0.0;
    protected double baroTimestamp = 0.0;
    protected double lightTimestamp = 0.0;
    protected double proxTimestamp = 0.0;
    protected double lastBaroTimestamp = 0;
    protected double lastLightTimestamp = 0;
    protected double lastProxTimestamp = 0;
    private Activity activity;
    private GnssStatus.Callback gnssStatusCallback;
    private LocationManager locationManager;
    private LocationListener locationListener;
    protected StringBuilder accelCsv = new StringBuilder();
    protected StringBuilder gyroCsv = new StringBuilder();
    protected StringBuilder magnetoCsv = new StringBuilder();
    protected StringBuilder baroCsv = new StringBuilder();
    protected StringBuilder lightCsv = new StringBuilder();
    protected StringBuilder proxCsv = new StringBuilder();



    public SensorManagerHelper(Context context, Activity activity, GnssStatus.Callback gnssStatusCallback, LocationListener locationListener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.context = context;
        this.activity = activity;
        this.gnssStatusCallback = gnssStatusCallback;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.locationListener = locationListener;
    }

    public void registerListeners() {
        Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor magnetoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        sensorManager.registerListener((SensorEventListener) this, accelSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, gyroSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, magnetoSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, baroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener((SensorEventListener) this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener((SensorEventListener) this, proxSensor, SensorManager.SENSOR_DELAY_FASTEST);

        // Register the GnssStatusCallback
        try {
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        // Register the LocationListener with default minTime and minDistance values
        if (locationManager != null) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListener);
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public void unregisterListeners() {
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        }

    }

    public Sensor getSensor(int sensorType) {
        return sensorManager.getDefaultSensor(sensorType);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // set to 100 for graph update to not consume too much RAM on the background
        int maxDataPoints = 100;
        if (sensorRunning) {
            long elapsedMillis = SystemClock.elapsedRealtime() - MainActivity.mChronometer.getBase();
            double elapsedSeconds = elapsedMillis / 1000.0;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accelTimestamp = elapsedSeconds;
                    accelCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", accelTimestamp, event.values[0], event.values[1], event.values[2]));
                    accelX.appendData(new DataPoint(accelTimestamp, event.values[0]), true,maxDataPoints);
                    accelY.appendData(new DataPoint(accelTimestamp, event.values[1]), true, maxDataPoints);
                    accelZ.appendData(new DataPoint(accelTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroTimestamp = elapsedSeconds;
                    gyroCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", gyroTimestamp, event.values[0], event.values[1], event.values[2]));
                    gyroX.appendData(new DataPoint(gyroTimestamp, event.values[0]), true, maxDataPoints);
                    gyroY.appendData(new DataPoint(gyroTimestamp, event.values[1]), true, maxDataPoints);
                    gyroZ.appendData(new DataPoint(gyroTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magnetoTimestamp = elapsedSeconds;
                    magnetoCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", magnetoTimestamp, event.values[0], event.values[1], event.values[2]));
                    magnetoX.appendData(new DataPoint(magnetoTimestamp, event.values[0]), true, maxDataPoints);
                    magnetoY.appendData(new DataPoint(magnetoTimestamp, event.values[1]), true, maxDataPoints);
                    magnetoZ.appendData(new DataPoint(magnetoTimestamp, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_PRESSURE:
                    if (elapsedSeconds - lastBaroTimestamp >= 1) {
                        baroTimestamp = elapsedSeconds;
                        baroCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", baroTimestamp, event.values[0]));
                        baroSeries.appendData(new DataPoint(baroTimestamp, event.values[0]), true, maxDataPoints);
                        lastBaroTimestamp = elapsedSeconds;
                    }
                    break;
                case Sensor.TYPE_LIGHT:
                    if (elapsedSeconds - lastLightTimestamp >= 1) {
                        lightTimestamp = elapsedSeconds;
                        lightCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", lightTimestamp, event.values[0]));
                        lightSeries.appendData(new DataPoint(lightTimestamp, event.values[0]), true, maxDataPoints);
                        lastLightTimestamp = elapsedSeconds;
                    }
                    break;
                case Sensor.TYPE_PROXIMITY:
                    if (elapsedSeconds - lastProxTimestamp >= 1) {
                        proxTimestamp = elapsedSeconds;
                        proxCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", proxTimestamp, event.values[0]));
                        proxSeries.appendData(new DataPoint(proxTimestamp, event.values[0]), true, maxDataPoints);
                        lastProxTimestamp = elapsedSeconds;
                    }
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

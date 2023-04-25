package DataCollection;

import static com.example.pdr_test.MainActivity.selectedBuilding;
import static com.example.pdr_test.MainActivity.startPDR;

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
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.pdr_test.FloorChangeHandler;
import com.example.pdr_test.IMUCalculations;
import com.example.pdr_test.MainActivity;
import com.example.pdr_test.TrajectoryView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Locale;

public class SensorManagerHelper implements SensorEventListener{
    // Initialize the class that are used
    public IMUCalculations imu_calculations;

    public FloorChangeHandler floorChangeHandler;
    public TrajectoryView trajectoryView;

    public SensorManager sensorManager;
    public Context context;

    public LineGraphSeries<DataPoint> accelX, accelY, accelZ;
    public LineGraphSeries<DataPoint> accelXUncalibrated, accelYUncalibrated, accelZUncalibrated;
    public LineGraphSeries<DataPoint> gyroX, gyroY, gyroZ;
    public LineGraphSeries<DataPoint> gyroXUncalibrated, gyroYUncalibrated, gyroZUncalibrated;
    public LineGraphSeries<DataPoint> magnetoX, magnetoY, magnetoZ;
    public LineGraphSeries<DataPoint> magnetoXUncalibrated, magnetoYUncalibrated, magnetoZUncalibrated;

    public LineGraphSeries<DataPoint> baroSeries, lightSeries, proxSeries;
    public boolean sensorRunning = false;
    public double accelTimestamp= 0.0;
    public double accelTimestamp1 = 0.0;
    public double accelTimestamp2 = 0.0;
    public double gyroTimestamp = 0.0;
    public double gyroTimestamp1 = 0.0;
    public double magnetoTimestamp = 0.0;
    public double magnetoTimestamp1 = 0.0;
    public double baroTimestamp = 0.0;
    public double lightTimestamp = 0.0;
    public double proxTimestamp = 0.0;
    public double lastBaroTimestamp = 0;
    public double lastLightTimestamp = 0;
    public double lastProxTimestamp = 0;
    public double accelTimestampUncalibrated = 0;
    public double accelTimestampUncalibrated1 = 0;
    public double gyroTimestampUncalibrated = 0;
    public double gyroTimestampUncalibrated1 = 0;
    public double magnetoTimestampUncalibrated = 0;
    public double magnetoTimestampUncalibrated1 = 0;
    private float previousBarometerReading = -1;
    // Saving the sensor values
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float[] aVal = new float[3];
    private float[] rVal = new float[3];
    private float[] mVal = new float[3];
    float[] filteredValues = new float[3];
    public Activity activity;
    public GnssStatus.Callback gnssStatusCallback;
    public LocationManager locationManager;
    public LocationListener locationListener;
    public StringBuilder positionVal = new StringBuilder();
    public StringBuilder accelCsv = new StringBuilder();
    public StringBuilder gyroCsv = new StringBuilder();
    public StringBuilder magnetoCsv = new StringBuilder();
    public StringBuilder baroCsv = new StringBuilder();
    public StringBuilder lightCsv = new StringBuilder();
    public StringBuilder proxCsv = new StringBuilder();
    public StringBuilder accelUncalibratedCsv = new StringBuilder();
    public StringBuilder gyroUncalibratedCsv = new StringBuilder();
    public StringBuilder magnetoUncalibratedCsv = new StringBuilder();
    private MainActivity mainActivity;


    public SensorManagerHelper(Context context, Activity activity, IMUCalculations imu_calculations, FloorChangeHandler floorChangeHandler, GnssStatus.Callback gnssStatusCallback, LocationListener locationListener) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.imu_calculations = imu_calculations;
        this.floorChangeHandler = floorChangeHandler;
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
        Sensor accelUncalibratedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        Sensor gyroUncalibratedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        Sensor magnetoUncalibratedSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        Sensor rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener((SensorEventListener) this, accelSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, gyroSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, magnetoSensor, 10000);
        sensorManager.registerListener(this, accelUncalibratedSensor, 10000);
        sensorManager.registerListener(this, gyroUncalibratedSensor, 10000);
        sensorManager.registerListener(this, magnetoUncalibratedSensor, 10000);
        sensorManager.registerListener(this, rotationVectorSensor, 10000);
        sensorManager.registerListener((SensorEventListener) this, baroSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener((SensorEventListener) this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener((SensorEventListener) this, proxSensor, SensorManager.SENSOR_DELAY_GAME);

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
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY));
        sensorManager.unregisterListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));

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
                    accelTimestamp = event.timestamp/1000000;
                    accelTimestamp1 = elapsedSeconds;
                    accelTimestamp2 = accelTimestamp1*1000;

                    accelCsv.append(String.format(Locale.getDefault(), "%f,%.3f,%f,%f,%f\n", accelTimestamp2, accelTimestamp,  event.values[0], event.values[1], event.values[2]));
//                    accelX.appendData(new DataPoint(accelTimestamp1, event.values[0]), true,maxDataPoints);
//                    accelY.appendData(new DataPoint(accelTimestamp1, event.values[1]), true, maxDataPoints);
//                    accelZ.appendData(new DataPoint(accelTimestamp1, event.values[2]), true, maxDataPoints);
                    // Detect steps
                    if(startPDR) {
                        positionVal.append(String.format(Locale.getDefault(),"%.3f,%d,", accelTimestamp2, floorChangeHandler.getFloorNumberForSV())+selectedBuilding+"\n");
                        aVal = imu_calculations.lowPassFilter(event.values, aVal);
                        imu_calculations.detectStep(aVal);
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    gyroTimestamp = event.timestamp/1000000;
                    gyroTimestamp1 = elapsedSeconds;
                    gyroCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", gyroTimestamp, event.values[0], event.values[1], event.values[2]));
//                    gyroX.appendData(new DataPoint(gyroTimestamp1, event.values[0]), true, maxDataPoints);
//                    gyroY.appendData(new DataPoint(gyroTimestamp1, event.values[1]), true, maxDataPoints);
//                    gyroZ.appendData(new DataPoint(gyroTimestamp1, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magnetoTimestamp = event.timestamp/1000000;
                    magnetoTimestamp1 = elapsedSeconds;
                    magnetoCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", magnetoTimestamp, event.values[0], event.values[1], event.values[2]));
//                    magnetoX.appendData(new DataPoint(magnetoTimestamp1, event.values[0]), true, maxDataPoints);
//                    magnetoY.appendData(new DataPoint(magnetoTimestamp1, event.values[1]), true, maxDataPoints);
//                    magnetoZ.appendData(new DataPoint(magnetoTimestamp1, event.values[2]), true, maxDataPoints);
                    break;
                case Sensor.TYPE_PRESSURE:
                    if (elapsedSeconds - lastBaroTimestamp >= 1) {
                        baroTimestamp = event.timestamp/1000000;
                        baroCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", baroTimestamp, event.values[0]));
//                        baroSeries.appendData(new DataPoint(baroTimestamp, event.values[0]), true, maxDataPoints);
                        lastBaroTimestamp = elapsedSeconds;

                        float currentReading = event.values[0];
                        if (previousBarometerReading != -1) {
                            float threshold = 0.5f; // Adjust this value based on your needs
                            floorChangeHandler.detectChangeFloor(currentReading, previousBarometerReading, threshold);
                        }

                        previousBarometerReading = currentReading;
                    }
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    if (IMUCalculations.stepCount > 0 && startPDR) {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                        SensorManager.getOrientation(rotationMatrix, orientationAngles);
                        // Update position in the IMUCalculations class
                        imu_calculations.updatePosition( orientationAngles);
                        imu_calculations.stepCount = 0; // Reset step count after updating position
                    }
                    break;
                case Sensor.TYPE_LIGHT:
                    if (elapsedSeconds - lastLightTimestamp >= 1) {
                        lightTimestamp = event.timestamp/1000000;
                        lightCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", lightTimestamp, event.values[0]));
//                        lightSeries.appendData(new DataPoint(lightTimestamp, event.values[0]), true, maxDataPoints);
                        lastLightTimestamp = elapsedSeconds;
                    }
                    break;
                case Sensor.TYPE_PROXIMITY:
                    if (elapsedSeconds - lastProxTimestamp >= 1) {
                        proxTimestamp = event.timestamp/1000000;
                        proxCsv.append(String.format(Locale.getDefault(), "%.3f,%f\n", proxTimestamp, event.values[0]));
//                        proxSeries.appendData(new DataPoint(proxTimestamp, event.values[0]), true, maxDataPoints);
                        lastProxTimestamp = elapsedSeconds;
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                    accelTimestampUncalibrated = event.timestamp/1000000;
                    accelTimestampUncalibrated1 = elapsedSeconds;
                    accelUncalibratedCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", accelTimestampUncalibrated, event.values[0], event.values[1], event.values[2]));
                    if(accelXUncalibrated != null){
//                        accelXUncalibrated.appendData(new DataPoint(accelTimestampUncalibrated1, event.values[0]), true, maxDataPoints);
                    }
                    if(accelYUncalibrated != null){
//                        accelYUncalibrated.appendData(new DataPoint(accelTimestampUncalibrated1, event.values[1]), true, maxDataPoints);
                    }
                    if(accelZUncalibrated != null){
//                        accelZUncalibrated.appendData(new DataPoint(accelTimestampUncalibrated1, event.values[2]), true, maxDataPoints);
                    }
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    gyroTimestampUncalibrated = event.timestamp/1000000;
                    gyroTimestampUncalibrated1 = elapsedSeconds;
                    gyroUncalibratedCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", gyroTimestampUncalibrated, event.values[0], event.values[1], event.values[2]));
                    if(gyroXUncalibrated != null) {
//                        gyroXUncalibrated.appendData(new DataPoint(gyroTimestampUncalibrated1, event.values[0]), true, maxDataPoints);
                    }
                    if(gyroYUncalibrated != null) {
//                        gyroYUncalibrated.appendData(new DataPoint(gyroTimestampUncalibrated1, event.values[1]), true, maxDataPoints);
                    }
                    if(gyroZUncalibrated != null) {
//                        gyroZUncalibrated.appendData(new DataPoint(gyroTimestampUncalibrated1, event.values[2]), true, maxDataPoints);
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    magnetoTimestampUncalibrated = event.timestamp/1000000;
                    magnetoTimestampUncalibrated1 = elapsedSeconds;
                    magnetoUncalibratedCsv.append(String.format(Locale.getDefault(), "%.3f,%f,%f,%f\n", magnetoTimestampUncalibrated, event.values[0], event.values[1], event.values[2]));
                    if(magnetoXUncalibrated != null) {
//                        magnetoXUncalibrated.appendData(new DataPoint(magnetoTimestampUncalibrated1, event.values[0]), true, maxDataPoints);
                    }
                    if(magnetoYUncalibrated != null) {
//                        magnetoYUncalibrated.appendData(new DataPoint(magnetoTimestampUncalibrated1, event.values[1]), true, maxDataPoints);
                    }
                    if(magnetoZUncalibrated != null) {
//                        magnetoZUncalibrated.appendData(new DataPoint(magnetoTimestampUncalibrated1, event.values[2]), true, maxDataPoints);
                    }
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

package com.example.pdr_test;



import static com.example.pdr_test.IMUCalculations.STEP_LENGTH;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import DataCollection.PermissionHelper;
import DataCollection.SensorManagerHelper;
import DataCollection.WifiManagerHelper;

public class MainActivity extends AppCompatActivity implements FloorChangeHandler.OnFloorChangeListener, DataCollection.WifiScanResultHandler{
    public static final int REQUEST_WRITE_STORAGE = 112;

    // Initialize the class that are used
    public IMUCalculations imu_calculations;
    public SensorManagerHelper sensorManagerHelper;
    public WifiManagerHelper wifiManagerHelper;
    public TrajectoryView trajectoryView;
    public static String selectedBuilding = "";

    private float prevAccelMagnitude = 0;
    private boolean isPeakDetected = false;
    private int stepCounter = 0;

    // Sensors declaration
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private Sensor rotationVectorSensor;


    private int previousStepCount = -1;


    // Saving the sensor values
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private float[] aVal = new float[3];
    private float[] rVal = new float[3];
    private float[] mVal = new float[3];
    float[] filteredValues = new float[3];



    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int REQUEST_LOCATION_PERMISSION = 101;

    private StringBuilder locationDataSB = new StringBuilder();
    // Declare a boolean variable outside the onLocationChanged method
    private boolean firstLocationUpdate = true;

    // add location
    public LocationManager locationManager;
    public GnssStatus.Callback gnssStatusCallback = new GnssStatusCallback();
    private TextView gnssStatusView, gnssInfoView;
    private String gnssFeaturesText;


    TextView accelData;
    TextView gyroData;
    TextView magnetoData;
    TextView baroData;
    TextView lightData;
    TextView proxData;
    Button startButton;
    Button stopButton;

    private int numAPs;

    // Timestamps for each sensors (they are the same but the difference is due to graph plotting)
    public static Chronometer mChronometer;

    // Views

    private Button toggleMoveMarkerButton;
    private boolean moveMarkerEnabled = false;
    public static boolean startPDR = false;

    private float previousBarometerReading = -1;
    private FloorChangeHandler floorChangeHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UI_initialization();
        setContentView(R.layout.activity_main);

        initialization();
        askForUserHeight();
    }

    private void UI_initialization(){
        // Get the selected building from the intent
        Intent intent = getIntent();
        selectedBuilding = intent.getStringExtra("building");
    }

    private void initialization() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Time settings
        mChronometer = findViewById(R.id.chronometer);

        // Views
        trajectoryView = findViewById(R.id.trajectory_view);

        floorChangeHandler = new FloorChangeHandler(this);
        imu_calculations = new IMUCalculations(trajectoryView);
        sensorManagerHelper = new SensorManagerHelper(this, this, imu_calculations, floorChangeHandler, gnssStatusCallback, locationListener);
        wifiManagerHelper = new WifiManagerHelper(this, this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        // askPermission

        PermissionHelper.askPermission(this,locationManager, gnssStatusCallback, locationListener);

        // Set the OnFloorChangeListener
        floorChangeHandler.setOnFloorChangeListener(new FloorChangeHandler.OnFloorChangeListener() {
            @Override
            public void onFloorChange(int floorResourceId, String building) {
                // Update the floorPlan in your TrajectoryView
                trajectoryView.changeFloor(floorResourceId, building);

                // Refresh the TrajectoryView (if necessary)
                trajectoryView.invalidate();

                // Update any other UI elements or data that depend on the floor
                // For example, if you have a TextView displaying the current floor, you can update it here.
            }
        });


        // Call the methods from the SortingViews class to access all the Views
        SortingViews.setupProcessNoiseSlider(this, imu_calculations);
        SortingViews.setupMeasurementNoiseSlider(this, imu_calculations);
        SortingViews.setupToggleMoveMarkerButton(this, trajectoryView, imu_calculations, sensorManagerHelper, wifiManagerHelper);
        SortingViews.setupResetButton(this, trajectoryView, imu_calculations, sensorManagerHelper, wifiManagerHelper, floorChangeHandler);
        SortingViews.setupChooseFloorButton(this, floorChangeHandler);
    }

    private class GnssStatusCallback extends GnssStatus.Callback {
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            int satellitesInView = status.getSatelliteCount();
            int satellitesInUse = 0;
            StringBuilder gpsStats = new StringBuilder();

            for (int i = 0; i < satellitesInView; i++) {
                gpsStats.append("\n\t- PRN:").append(status.getSvid(i)).append(", Used:").append(status.usedInFix(i)).append(", SNR:")
                        .append(status.getCn0DbHz(i)).append(", Az:").append(status.getAzimuthDegrees(i)).append("º,\n\t   Elev: ").append(status.getElevationDegrees(i))
                        .append("º");
                if (status.usedInFix(i)) {
                    satellitesInUse++;
                }
            }
//            String text = "\tSatellites in View: " + satellitesInView + ", Satellites in Use: " + satellitesInUse + gpsStats;
//            TextView satelliteDataTextView = findViewById(R.id.satellitesData);
//            satelliteDataTextView.setText(text);

        }
    }

    @Override
    public void onWifiScanResultReceived(List<ScanResult> scanResults) {
        numAPs = scanResults.size();
        String wifiStr = "";

        // Get the elapsed time from the chronometer in milliseconds
        for (ScanResult result : scanResults) {
            String SSID = result.SSID, BSSID = result.BSSID;
            int frequency = result.frequency;
            double wifitimestamp = result.timestamp/1000;
            int RSS = result.level;
            wifiStr +=  "\n\t- " + SSID + ",\t" + BSSID + ",\tRSS:" + RSS + " dBm";
            // Updating the wifi data for CSV
            wifiManagerHelper.wifiCsvData.append(String.format(Locale.US, "%d, %.5f,%s,%s,%d,%d\n", numAPs, wifitimestamp, SSID, BSSID, RSS, frequency));
        }

//        String text = "\n" + " Number of Wifi APs: " + numAPs + wifiStr;
////        wifiManagerHelper.wifiTextView.setText(text);
//
//        wifiManagerHelper.measuredWifiFreq = (float) (0.8 * wifiManagerHelper.measuredWifiFreq + 0.2 / (wifiManagerHelper.timestamp - wifiManagerHelper.lastWifiTimestamp));
//        wifiManagerHelper.lastWifiTimestamp = wifiManagerHelper.timestamp;

//        WifiInfo wifiInfo = wifiManagerHelper.wifiManager.getConnectionInfo();
//        if (wifiInfo.getBSSID() != null) {
//            String displayString = String.format(Locale.US, "\n" + " Connected to: %s\n\tBSSID: %s\n\tRSSI: %d dBm \n\tLinkSpeed: %d Mbps\n\t\t\t\t\t\t\t\tFreq: %5.1f Hz", wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getRssi(), wifiInfo.getLinkSpeed(), wifiManagerHelper.measuredWifiFreq);
//            wifiManagerHelper.wifiConnectionView.setText(displayString);
//        } else {
//            wifiManagerHelper.wifiConnectionView.setText(" No connection detected");
//        }
    }

    public LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            double latitude = location.getLatitude(), longitude = location.getLongitude(), altitude = location.getAltitude();
            float bearing = location.getBearing(), accuracy = location.getAccuracy(), speed = location.getSpeed();
            String provider = location.getProvider();
            Calendar calendar = Calendar.getInstance();
            double utc = calendar.getTimeInMillis();
            double realtime = SystemClock.elapsedRealtime();

            // Calculate the elapsed time in seconds
            long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
            double elapsedSeconds = elapsedMillis / 1000.0;

            StringBuilder cadena_display = new StringBuilder(String.format(Locale.US, "\tLatitude: \t%10.6f \tdegrees\n\tLongitude: \t%10.6f \tdegrees\n", latitude, longitude));
            cadena_display.append(location.hasAltitude() ? String.format(Locale.US, "\tAltitude: \t%6.1f \t m\n", altitude) : "\tAltitude: \t\t? \tm\n");
            cadena_display.append(location.hasAccuracy() ? String.format(Locale.US, "\tAccuracy: \t%8.3f \tm\n", accuracy) : "\tAccuracy: \t\t? \tm\n");
            cadena_display.append(location.hasBearing() ? String.format(Locale.US, "\tBearing: \t\t%8.3f \tdegrees\n", bearing) : "\tBearing: \t\t? \tdegrees\n");
            cadena_display.append(location.hasSpeed() ? String.format(Locale.US, "\tSpeed: \t%8.3f \tm\n", speed) : "\tSpeed: \t\t? \tm\n");
            cadena_display.append(String.format(Locale.US, "\tTime: \t%8.3f \ts\n", elapsedSeconds));
            cadena_display.append(String.format(Locale.US, "\t(Provider: \t%s\n", provider.toUpperCase()));

            // Append the location data values as CSV
            locationDataSB.append(String.format(Locale.US, "%.6f,%.6f,%.1f,%.3f,%.3f,%.3f,%.3f,%s,%.6f,%.9f\n",
                    elapsedSeconds, latitude, longitude, altitude, accuracy, bearing, speed, provider.toUpperCase(),utc,realtime));

            // Display the location data on the screen
//            TextView obj_txtView = findViewById(R.id.locationChangedData); // rename to locationChangedData
//            obj_txtView.setText(cadena_display.toString());
        }

        @Override
        // Methods required by LocationListener
        public void onProviderDisabled(String provider) {
////            TextView obj_txtView = findViewById(R.id.locationChangedData);
//            obj_txtView.setText("GNSS Provider Disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
//            TextView obj_txtView = findViewById(R.id.locationChangedData);
//            obj_txtView.setText("GNSS Provider Enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
//            TextView obj_txtView = findViewById(R.id.locationChangedData);
//            obj_txtView.setText("GNSS Provider Status: " + status);
        }
    };

    public void setLocationScanHandler(){
        // Display general GNSS data:
        if (locationManager != null) {
            LocationProvider provider = null;

            gnssFeaturesText = "";
//            gnssStatusView.setText("\n"+" GNSS: Location Service (GPS/Network)");

            //List<String> providerList = locationManager.getAllProviders();
            List<String> providerList = locationManager.getProviders(true);

            for (String providerStr : providerList) {
                int providerIndex = providerList.indexOf(providerStr);
                try {
                    provider = locationManager.getProvider(providerStr);
                } catch (Exception e) {
//                    Log.i("OnCreate", "getProvider not responding properly");
                    gnssFeaturesText = " GNSS: No Location Providers";
//                    gnssStatusView.setBackgroundColor(0xFFFF0000); // red color
                }
                if (provider != null) {
                    gnssFeaturesText = gnssFeaturesText + " -Location Provider" + providerIndex + ": " + providerStr.toUpperCase() +
                            ", Accuracy: " + provider.getAccuracy() + ", \n  Supports Altitude: " + provider.supportsAltitude() +
                            ", Power Cons.: " + provider.getPowerRequirement() + " mA" + "\n";
                }
            }
        } else {
//            gnssStatusView.setText(" GNSS: No LOCATION system detected");
            gnssFeaturesText = "No Features";
//            gnssStatusView.setBackgroundColor(0xFFFF0000); // red color
        }
//        gnssInfoView.setText(gnssFeaturesText);
    }

//    public void onClickStartButton(View view){
//        // askPermission
//        PermissionHelper.askPermission(this, locationManager, gnssStatusCallback, locationListener);
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            // Register the GnssStatusCallback
//            try {
//                locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
//            } catch (SecurityException e) {
//                e.printStackTrace();
//            }
//
//            // Register the LocationListener with default minTime and minDistance values
//            try {
//                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, locationListener);
//            } catch (SecurityException e) {
//                e.printStackTrace();
//            }
//        }
//
//        mChronometer.setBase(SystemClock.elapsedRealtime()); // reset to 0
//        mChronometer.start();
//
//        sensorManagerHelper.sensorRunning = true;
//
//        // resetting the timestamps
//        sensorManagerHelper.accelTimestamp = 0.0;
//        sensorManagerHelper.accelTimestampUncalibrated = 0.0;
//        sensorManagerHelper.gyroTimestamp = 0.0;
//        sensorManagerHelper.gyroTimestampUncalibrated = 0.0;
//        sensorManagerHelper.magnetoTimestamp = 0.0;
//        sensorManagerHelper.magnetoTimestampUncalibrated = 0.0;
//        sensorManagerHelper.baroTimestamp = 0.0;
//        sensorManagerHelper.lightTimestamp = 0.0;
//        sensorManagerHelper.proxTimestamp = 0.0;
//
//        // Clear the acceleration graph data
////        sensorManagerHelper.accelX.resetData(new DataPoint[]{});
////        sensorManagerHelper.accelY.resetData(new DataPoint[]{});
////        sensorManagerHelper.accelZ.resetData(new DataPoint[]{});
////
////        // Clear the Uncalibrated acceleration graph data
////        if(sensorManagerHelper.accelXUncalibrated != null){
////            sensorManagerHelper.accelXUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.accelYUncalibrated != null){
////            sensorManagerHelper.accelYUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.accelZUncalibrated != null){
////            sensorManagerHelper.accelZUncalibrated.resetData(new DataPoint[]{});
////        }
////
////        // Clear the gyroscope graph data
////        sensorManagerHelper.gyroX.resetData(new DataPoint[]{});
////        sensorManagerHelper.gyroY.resetData(new DataPoint[]{});
////        sensorManagerHelper.gyroZ.resetData(new DataPoint[]{});
////
////        // Clear the Uncalibrated gyroscope graph data
////        if(sensorManagerHelper.gyroXUncalibrated != null) {
////            sensorManagerHelper.gyroXUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.gyroYUncalibrated != null) {
////            sensorManagerHelper.gyroYUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.gyroZUncalibrated != null) {
////            sensorManagerHelper.gyroZUncalibrated.resetData(new DataPoint[]{});
////        }
////
////        // Clear the magnetometer graph data
////        sensorManagerHelper.magnetoX.resetData(new DataPoint[]{});
////        sensorManagerHelper.magnetoY.resetData(new DataPoint[]{});
////        sensorManagerHelper.magnetoZ.resetData(new DataPoint[]{});
////
////        // Clear the Uncalibrated magnetometer graph data
////        if(sensorManagerHelper.magnetoXUncalibrated != null) {
////            sensorManagerHelper.magnetoXUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.magnetoYUncalibrated != null) {
////            sensorManagerHelper.magnetoYUncalibrated.resetData(new DataPoint[]{});
////        }
////        if(sensorManagerHelper.magnetoZUncalibrated != null) {
////        if(sensorManagerHelper.magnetoZUncalibrated != null) {
////            sensorManagerHelper.magnetoZUncalibrated.resetData(new DataPoint[]{});
////        }
////
////        // Clear the barometer graph data
////        sensorManagerHelper.baroSeries.resetData(new DataPoint[]{});
////
////        // Clear the proximity sensor graph data
////        sensorManagerHelper.proxSeries.resetData(new DataPoint[]{});
////
////        // Clear the ambient light sensor graph data
////        sensorManagerHelper.lightSeries.resetData(new DataPoint[]{});
//
//        wifiManagerHelper.setWiFiScanHandler();
//
//        setLocationScanHandler();
//        // Unregister the sensor listener if it's already registered
//        sensorManagerHelper.unregisterListeners();
//
//        // Register the sensor listener
//        sensorManagerHelper.registerListeners();
//        wifiManagerHelper.registerWifiReceiver();
//    }

//    public void onClickStopButton(View view){
//        mChronometer.stop();
//
//        if(sensorManagerHelper.sensorRunning){
//            // Unregister the sensor listener
//            sensorManagerHelper.unregisterListeners();
//
//            // Unregister the WiFi broadcast receiver
//            //wifiManagerHelper.unregisterWifiReceiver();
//
//            // Stop the WiFi scanning timer
//            wifiManagerHelper.timerWifi.cancel();
//            wifiManagerHelper.timerWifi.purge();
//
//            showSaveDialogAllData();
//        }
//        sensorManagerHelper.sensorRunning = false;
//    }

    public void showFileNameDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Enter file name for the trajectory:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString();
                // Add log statements to check data
                Log.d("SortingViews", "Points count: " + trajectoryView.points.size());
                Log.d("SortingViews", "PositionVal: " + sensorManagerHelper.positionVal.toString());
                CSVExporter.savePointsToCSV(trajectoryView.pointsCsv, sensorManagerHelper.positionVal, fileName, MainActivity.this);

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showSaveDialogAllData() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Enter a file name for all data:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (!fileName.isEmpty()) {
                saveAllDataToFile(fileName);
            } else {
                Toast.makeText(this, "Please enter a valid file name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveAllDataToFile(String fileName) {
        List<List<String>> allData = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        List<String> combinedData = new ArrayList<>();
        List<Integer> headerCounts = new ArrayList<>();
        int maxLines;

        // Helper function to split lines and add data
        BiConsumer<StringBuilder, List<String>> splitAndAddData = (sb, targetList) -> {
            if (sb != null && sb.length() > 0) {
                targetList.addAll(Arrays.asList(sb.toString().split("\n")));
            }
        };

        // Process each sensor data type and store it in a list
        List<String> accelLines = new ArrayList<>();
        List<String> baroLines = new ArrayList<>();
        List<String> lightLines = new ArrayList<>();
        List<String> proxLines = new ArrayList<>();
        List<String> gyroLines = new ArrayList<>();
        List<String> magnetoLines = new ArrayList<>();
        List<String> wifiLines = new ArrayList<>();
        List<String> locationLines = new ArrayList<>();
        List<String> accelUncalibratedLines = new ArrayList<>();
        List<String> gyroUncalibratedLines = new ArrayList<>();
        List<String> magnetoUncalibratedLines = new ArrayList<>();

        if(sensorManagerHelper.accelUncalibratedCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.accelUncalibratedCsv, accelUncalibratedLines);
        }
        if(sensorManagerHelper.gyroUncalibratedCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.gyroUncalibratedCsv, gyroUncalibratedLines);
        }
        if(sensorManagerHelper.magnetoUncalibratedCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.magnetoUncalibratedCsv, magnetoUncalibratedLines);
        }
        if (sensorManagerHelper.accelCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.accelCsv, accelLines);
        }
        if (sensorManagerHelper.baroCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.baroCsv, baroLines);
        }
        if (sensorManagerHelper.lightCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.lightCsv, lightLines);
        }
        if (sensorManagerHelper.proxCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.proxCsv, proxLines);
        }
        if (sensorManagerHelper.gyroCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.gyroCsv, gyroLines);
        }
        if (sensorManagerHelper.magnetoCsv != null) {
            splitAndAddData.accept(sensorManagerHelper.magnetoCsv, magnetoLines);
        }
        if (wifiManagerHelper.wifiCsvData != null) {
            splitAndAddData.accept(wifiManagerHelper.wifiCsvData, wifiLines);
        }
        if (locationDataSB != null) {
            splitAndAddData.accept(locationDataSB, locationLines);
        }

        allData.addAll(Arrays.asList(accelLines, accelUncalibratedLines, baroLines, lightLines, proxLines, gyroLines, gyroUncalibratedLines, magnetoLines, magnetoUncalibratedLines, wifiLines, locationLines));
        headers.addAll(Arrays.asList("timestamp_global[ms]","AccelTimestamp", "acc_bias_x[m/s^2]", "acc_bias_y[m/s^2]", "acc_bias_z[m/s^2]","AccelTimestampUncalibrated", "acc_uncal_x[m/s^2]", "acc_uncal_y[m/s^2]", "acc_uncal_z[m/s^2]", "BaroTimestamp", "Baro", "LightTimestamp", "ambient_brightness[lux]", "ProxTimestamp", "Proximity", "GyroTimestamp", "ang_vel_bias_x[rad/s]", "ang_vel_bias_y[rad/s]", "ang_vel_bias_z[rad/s]", "GyroTimestampUncalibrated", "ang_vel_uncal_x[rad/s]", "ang_vel_uncal_y[rad/s]", "ang_vel_uncal_z[rad/s]","MagnetoTimestamp", "mfield_bias_x[uT]", "mfield_bias_y[uT]", "mfield_bias_z[uT]", "MagnetoTimestampUncalibrated", "mfield_uncal_x[uT]", "mfield_uncal_y[uT]", "mfield_uncal_z[uT]", "wifinum", "WiFiTimestamp", "SSID", "BSSID", "RSS", "frequency", "LocationTimestamp", "lat (deg)", "long (deg)", "altitude (m above sea level)", "accuracy (m)", "bearing (degrees)", "speed (m/s over ground)", "Provider","UTC","realtime"));
        headerCounts.addAll(Arrays.asList(5, 4, 2, 2, 2, 4, 4, 4, 4, 6, 10));

        // Calculate the maximum number of lines from all sensor data lists
        maxLines = allData.stream().mapToInt(List::size).max().orElse(0);

        // Combine sensor data and skip empty columns where needed
        for (int i = 0; i < maxLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int dataIndex = 0; dataIndex < allData.size(); dataIndex++) {
                List<String> lines = allData.get(dataIndex);
                int skipColumns = 0;
                if (i < lines.size()) {
                    String[] values = lines.get(i).split(",");
                    for (int j = 0; j < values.length; j++) {
                        line.append(values[j]);
                        line.append(",");
                    }
                } else {
                    int columnCount = headerCounts.get(dataIndex);
                    skipColumns = columnCount;
                }
                // Append commas to skip empty columns
                for (int j = 0; j < skipColumns; j++) {
                    line.append("0,");
                }
            }
            combinedData.add(line.toString());
        }
        writeDataToCsv(fileName, combinedData, headers);
    }

    private void writeDataToCsv(String fileName, List<String> allData, List<String> headers) {
        File directory = getExternalFilesDir(null);
        File file = new File(directory, "RawData_" + fileName + ".csv");

        // Use try-with-resources to ensure the writer is closed automatically
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))) {

            // Write data lines
            for (String line : allData) {
                String[] values = line.split(",");
                csvPrinter.printRecord((Object[]) values);
            }

            csvPrinter.flush();
            Toast.makeText(this, "Data saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving data to file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied, show an explanation to the user or disable the related functionality
        }
        if (requestCode == REQUEST_WRITE_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, register the GnssStatusCallback
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
            } else {
                // Permission is denied, show an appropriate message or handle the case accordingly
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationEnabled();
        if(sensorManagerHelper.sensorRunning) {
            sensorManagerHelper.registerListeners();
            // Register the GnssStatusCallback
            try {
                locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location service is off");
            builder.setMessage("Please enable the location service to use this app.");
            builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }

    @Override
    public void onFloorChange(int floorResourceId, String Building) {
        trajectoryView.changeFloor(floorResourceId, Building);
    }


    private void askForUserHeight() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your height (m)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                float userHeight = Float.parseFloat(input.getText().toString());
                STEP_LENGTH = IMUCalculations.estimateStrideLength(userHeight);
                // Store the stride length and use it in your calculations
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(sensorManagerHelper.sensorRunning){
            sensorManagerHelper.unregisterListeners();
        }
        if(wifiManagerHelper != null){
            wifiManagerHelper.unregisterWifiReceiver();
        }
    }

}


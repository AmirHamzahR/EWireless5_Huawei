package com.example.datacollectionapp_afp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class MainActivity extends AppCompatActivity implements WifiScanResultHandler {

    private SensorManagerHelper sensorManagerHelper;
    private WifiManagerHelper wifiManagerHelper;

    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final int REQUEST_LOCATION_PERMISSION = 101;

    private StringBuilder locationDataSB = new StringBuilder();
    // Declare a boolean variable outside the onLocationChanged method
    private boolean firstLocationUpdate = true;

    // add location
    private LocationManager locationManager;
    private GnssStatus.Callback gnssStatusCallback = new GnssStatusCallback();
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
    protected static Chronometer mChronometer;

    // onCreate and other UI-related methods ...
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        initialization();
        sensorsInformation();
        graphViews();
    }

    private void initialization() {

        sensorManagerHelper = new SensorManagerHelper(this, this, gnssStatusCallback, locationListener);
        wifiManagerHelper = new WifiManagerHelper(this, this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //Views
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        accelData = findViewById(R.id.accelData);
        gyroData = findViewById(R.id.gyroData);
        magnetoData = findViewById(R.id.magnetoData);
        baroData = findViewById(R.id.baroData);
        lightData = findViewById(R.id.lightData);
        proxData = findViewById(R.id.proxData);

        gnssStatusView = findViewById(R.id.gnssStatusView);
        gnssInfoView = findViewById(R.id.gnssInfoView);

        wifiManagerHelper.wifiInfoView = findViewById(R.id.wifiInfoView);
        wifiManagerHelper.wifiStatusView = findViewById(R.id.wifiStatusView);
        wifiManagerHelper.wifiConnectionView = findViewById(R.id.wifiConnectionView);
        wifiManagerHelper.wifiTextView = findViewById(R.id.wifiTextView);

        //Time settings
        mChronometer = findViewById(R.id.chronometer);

    }

    private void sensorsInformation() {
        accelData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_ACCELEROMETER), "m/s^2", "No Accelerometer detected"));
        gyroData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_GYROSCOPE), "rad/s", "No Gyroscope detected"));
        magnetoData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_MAGNETIC_FIELD), "uT", "No Magnetometer detected"));
        baroData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_PRESSURE), "hPa", "No Barometer detected"));
        lightData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_LIGHT), "lx", "No Light Sensor detected"));
        proxData.setText(SensorInformationHelper.getSensorInfo(sensorManagerHelper.getSensor(Sensor.TYPE_PROXIMITY), "cm", "No Proximity Sensor detected"));

        // Handle the background color for sensor TextViews
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_ACCELEROMETER), accelData);
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_GYROSCOPE), gyroData);
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_MAGNETIC_FIELD), magnetoData);
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_PRESSURE), baroData);
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_LIGHT), lightData);
        setSensorDataBackgroundColor(sensorManagerHelper.getSensor(Sensor.TYPE_PROXIMITY), proxData);
    }

    private void setSensorDataBackgroundColor(Sensor sensor, TextView sensorData) {
        if (sensor == null) {
            sensorData.setBackgroundColor(0xFFFF0000); // red color
        }
    }

    private void graphViews() {
        // Load views
        GraphView accelGraph = findViewById(R.id.accelGraph);
        GraphView gyroGraph = findViewById(R.id.gyroGraph);
        GraphView magnetoGraph = findViewById(R.id.magnetoGraph);
        GraphView baroGraph = findViewById(R.id.baroGraph);
        GraphView lightGraph = findViewById(R.id.lightGraph);
        GraphView proxGraph = findViewById(R.id.proxGraph);

        // Configure viewport settings
        GraphViewHelper.graphSettings(accelGraph);
        GraphViewHelper.graphSettings(gyroGraph);
        GraphViewHelper.graphSettings(magnetoGraph);
        GraphViewHelper.graphSettings(baroGraph);
        GraphViewHelper.graphSettings(lightGraph);
        GraphViewHelper.graphSettings(proxGraph);

        // Initialize series
        sensorManagerHelper.accelX = new LineGraphSeries<>();
        sensorManagerHelper.accelY = new LineGraphSeries<>();
        sensorManagerHelper.accelZ = new LineGraphSeries<>();
        sensorManagerHelper.gyroX = new LineGraphSeries<>();
        sensorManagerHelper.gyroY = new LineGraphSeries<>();
        sensorManagerHelper.gyroZ = new LineGraphSeries<>();
        sensorManagerHelper.magnetoX = new LineGraphSeries<>();
        sensorManagerHelper.magnetoY = new LineGraphSeries<>();
        sensorManagerHelper.magnetoZ = new LineGraphSeries<>();
        sensorManagerHelper.baroSeries = new LineGraphSeries<>();
        sensorManagerHelper.lightSeries = new LineGraphSeries<>();
        sensorManagerHelper.proxSeries = new LineGraphSeries<>();

        // Configure series settings and add to graphs
        GraphViewHelper.dataSettings(sensorManagerHelper.accelX, "accelX", Color.RED, 8, accelGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.accelY, "accelY", Color.GREEN, 8, accelGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.accelZ, "accelZ", Color.BLUE, 8, accelGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.gyroX, "gyroX", Color.RED, 8, gyroGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.gyroY, "gyroY", Color.GREEN, 8, gyroGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.gyroZ, "gyroZ", Color.BLUE, 8, gyroGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.magnetoX, "magnetoX", Color.RED, 8, magnetoGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.magnetoY, "magnetoY", Color.GREEN, 8, magnetoGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.magnetoZ, "magnetoZ", Color.BLUE, 8, magnetoGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.baroSeries, "baroSeries", Color.RED, 8, baroGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.lightSeries, "lightSeries", Color.RED, 8, lightGraph);
        GraphViewHelper.dataSettings(sensorManagerHelper.proxSeries, "proxSeries", Color.RED, 8, proxGraph);
    }

    private void setLocationScanHandler(){
        // Display general GNSS data:
        if (locationManager != null) {
            LocationProvider provider = null;

            gnssFeaturesText = "";
            gnssStatusView.setText("\n"+" GNSS: Location Service (GPS/Network)");

            //List<String> providerList = locationManager.getAllProviders();
            List<String> providerList = locationManager.getProviders(true);

            for (String providerStr : providerList) {
                int providerIndex = providerList.indexOf(providerStr);
                try {
                    provider = locationManager.getProvider(providerStr);
                } catch (Exception e) {
//                    Log.i("OnCreate", "getProvider not responding properly");
                    gnssFeaturesText = " GNSS: No Location Providers";
                    gnssStatusView.setBackgroundColor(0xFFFF0000); // red color
                }
                if (provider != null) {
                    gnssFeaturesText = gnssFeaturesText + " -Location Provider" + providerIndex + ": " + providerStr.toUpperCase() +
                            ", Accuracy: " + provider.getAccuracy() + ", \n  Supports Altitude: " + provider.supportsAltitude() +
                            ", Power Cons.: " + provider.getPowerRequirement() + " mA" + "\n";
                }
            }
        } else {
            gnssStatusView.setText(" GNSS: No LOCATION system detected");
            gnssFeaturesText = "No Features";
            gnssStatusView.setBackgroundColor(0xFFFF0000); // red color
        }
        gnssInfoView.setText(gnssFeaturesText);
    }

    @Override
    public void onWifiScanResultReceived(List<ScanResult> scanResults) {
        numAPs = scanResults.size();
        String wifiStr = "";

        // Get the elapsed time from the chronometer in milliseconds
        long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();

        // Convert the elapsed time to seconds
        double elapsedSeconds = elapsedMillis / 1000.0;

        for (ScanResult result : scanResults) {
            String SSID = result.SSID, BSSID = result.BSSID;
            int frequency = result.frequency;
            int RSS = result.level;
            wifiStr +=  "\n\t- " + SSID + ",\t" + BSSID + ",\tRSS:" + RSS + " dBm";
            // Updating the wifi data for CSV
            wifiManagerHelper.wifiCsvData += String.format(Locale.US, "%.5f,%s,%s,%d,%d\n", elapsedSeconds, SSID, BSSID, RSS, frequency);
        }

        String text = "\n" + " Number of Wifi APs: " + numAPs + wifiStr;
        wifiManagerHelper.wifiTextView.setText(text);

        wifiManagerHelper.measuredWifiFreq = (float) (0.8 * wifiManagerHelper.measuredWifiFreq + 0.2 / (wifiManagerHelper.timestamp - wifiManagerHelper.lastWifiTimestamp));
        wifiManagerHelper.lastWifiTimestamp = wifiManagerHelper.timestamp;

        if (wifiManagerHelper.wifiManager.isWifiEnabled()) {
            wifiManagerHelper.wifiStatusView.setBackgroundColor(0xFF00FF00);
            wifiManagerHelper.wifiStatusView.setText("\n" + " WIFI: Switched ON");
            wifiManagerHelper.wifiFeaturesText = "\n" + " WiFi MAC address: " + wifiManagerHelper.wifiManager.getConnectionInfo().getMacAddress();
            wifiManagerHelper.wifiInfoView.setText(wifiManagerHelper.wifiFeaturesText);
        }

        WifiInfo wifiInfo = wifiManagerHelper.wifiManager.getConnectionInfo();
        if (wifiInfo.getBSSID() != null) {
            String displayString = String.format(Locale.US, "\n" + " Connected to: %s\n\tBSSID: %s\n\tRSSI: %d dBm \n\tLinkSpeed: %d Mbps\n\t\t\t\t\t\t\t\tFreq: %5.1f Hz", wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getRssi(), wifiInfo.getLinkSpeed(), wifiManagerHelper.measuredWifiFreq);
            wifiManagerHelper.wifiConnectionView.setText(displayString);
        } else {
            wifiManagerHelper.wifiConnectionView.setText(" No connection detected");
        }
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
            String text = "\tSatellites in View: " + satellitesInView + ", Satellites in Use: " + satellitesInUse + gpsStats;
            TextView satelliteDataTextView = findViewById(R.id.satellitesData);
            satelliteDataTextView.setText(text);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied, show an explanation to the user or disable the related functionality
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

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            double latitude = location.getLatitude(), longitude = location.getLongitude(), altitude = location.getAltitude();
            float bearing = location.getBearing(), accuracy = location.getAccuracy(), speed = location.getSpeed();
            //double sensorTimestamp = location.getTime() / 1000.0;
            String provider = location.getProvider();

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
            locationDataSB.append(String.format(Locale.US, "%.6f,%.6f,%.1f,%.3f,%.3f,%.3f,%.3f,%s\n",
                    elapsedSeconds, latitude, longitude, altitude, accuracy, bearing, speed, provider.toUpperCase()));

            // Display the location data on the screen
            TextView obj_txtView = findViewById(R.id.locationChangedData); // rename to locationChangedData
            obj_txtView.setText(cadena_display.toString());
        }

        @Override
        // Methods required by LocationListener
        public void onProviderDisabled(String provider) {
            TextView obj_txtView = findViewById(R.id.locationChangedData);
            obj_txtView.setText("GNSS Provider Disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            TextView obj_txtView = findViewById(R.id.locationChangedData);
            obj_txtView.setText("GNSS Provider Enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            TextView obj_txtView = findViewById(R.id.locationChangedData);
            obj_txtView.setText("GNSS Provider Status: " + status);
        }
    };

    public void onClickStartButton(View view){
        // askPermission
        PermissionHelper.askPermission(this, locationManager, gnssStatusCallback, locationListener);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Register the GnssStatusCallback
            try {
                locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            // Register the LocationListener with default minTime and minDistance values
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        mChronometer.setBase(SystemClock.elapsedRealtime()); // reset to 0
        mChronometer.start();

        sensorManagerHelper.sensorRunning = true;

        // resetting the timestamps
        sensorManagerHelper.accelTimestamp = 0.0;
        sensorManagerHelper.gyroTimestamp = 0.0;
        sensorManagerHelper.magnetoTimestamp = 0.0;
        sensorManagerHelper.baroTimestamp = 0.0;
        sensorManagerHelper.lightTimestamp = 0.0;
        sensorManagerHelper.proxTimestamp = 0.0;

        // Clear the acceleration graph data
        sensorManagerHelper.accelX.resetData(new DataPoint[]{});
        sensorManagerHelper.accelY.resetData(new DataPoint[]{});
        sensorManagerHelper.accelZ.resetData(new DataPoint[]{});

        // Clear the gyroscope graph data
        sensorManagerHelper.gyroX.resetData(new DataPoint[]{});
        sensorManagerHelper.gyroY.resetData(new DataPoint[]{});
        sensorManagerHelper.gyroZ.resetData(new DataPoint[]{});

        // Clear the magnetometer graph data
        sensorManagerHelper.magnetoX.resetData(new DataPoint[]{});
        sensorManagerHelper.magnetoY.resetData(new DataPoint[]{});
        sensorManagerHelper.magnetoZ.resetData(new DataPoint[]{});

        // Clear the barometer graph data
        sensorManagerHelper.baroSeries.resetData(new DataPoint[]{});

        // Clear the proximity sensor graph data
        sensorManagerHelper.proxSeries.resetData(new DataPoint[]{});

        // Clear the ambient light sensor graph data
        sensorManagerHelper.lightSeries.resetData(new DataPoint[]{});

        wifiManagerHelper.setWiFiScanHandler();

        setLocationScanHandler();
        // Unregister the sensor listener if it's already registered
        sensorManagerHelper.unregisterListeners();

        // Register the sensor listener
        sensorManagerHelper.registerListeners();
        wifiManagerHelper.registerWifiReceiver();
    }

    public void onClickStopButton(View view){
        mChronometer.stop();

        if(sensorManagerHelper.sensorRunning){
            // Unregister the sensor listener
            sensorManagerHelper.unregisterListeners();

            // Unregister the WiFi broadcast receiver
            //wifiManagerHelper.unregisterWifiReceiver();

            // Stop the WiFi scanning timer
            wifiManagerHelper.timerWifi.cancel();
            wifiManagerHelper.timerWifi.purge();

            showSaveDialogAllData();
        }
        sensorManagerHelper.sensorRunning = false;
    }

    private void showSaveDialogAllData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            splitAndAddData.accept(new StringBuilder(wifiManagerHelper.wifiCsvData), wifiLines);
        }
        if (locationDataSB != null) {
            splitAndAddData.accept(locationDataSB, locationLines);
        }

        allData.addAll(Arrays.asList(accelLines, baroLines, lightLines, proxLines, gyroLines, magnetoLines, wifiLines, locationLines));
        headers.addAll(Arrays.asList("AccelTimestamp", "AccelX", "AccelY", "AccelZ", "BaroTimestamp", "Baro", "LightTimestamp", "Light", "ProxTimestamp", "Proximity", "GyroTimestamp", "GyroX", "GyroY", "GyroZ", "MagnetoTimestamp", "MagnetoX", "MagnetoY", "MagnetoZ", "WiFiTimestamp", "SSID", "BSSID", "RSS", "frequency", "LocationTimestamp", "Latitude", "Longitude", "Altitude", "Accuracy", "Bearing", "Speed", "Provider"));
        headerCounts.addAll(Arrays.asList(4, 2, 2, 2, 4, 4, 5, 10));

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
                    line.append(",");
                }
            }
            combinedData.add(line.toString());
        }
        writeDataToCsv(fileName, combinedData, headers);
    }



    private void writeDataToCsv(String fileName, List<String> allData, List<String> headers) {
        File directory = getExternalFilesDir(null);
        File file = new File(directory, fileName + ".csv");

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
    protected void onResume() {
        super.onResume();
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


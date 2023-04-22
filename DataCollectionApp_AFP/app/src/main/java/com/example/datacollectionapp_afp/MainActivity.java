package com.example.datacollectionapp_afp;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Static string to transfer the intent on another class
    public static final String DATA_TRANSFER = "data_transfer";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;


    // Sensors that are used
    private SensorManager sm;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Sensor magnetoSensor;
    private Sensor baroSensor;
    private Sensor lightSensor;
    private Sensor proxSensor;

    // add location
    private LocationManager locationManager;
    private StringBuilder locationDataSB = new StringBuilder();
    // Declare a boolean variable outside the onLocationChanged method
    private boolean firstLocationUpdate = true;
    private GnssStatus.Callback gnssStatusCallback;
    private TextView gnssStatusView, gnssInfoView;
    private String gnssFeaturesText;
    private double timestamp_ns, timestamp_Gnss_last, timestamp_Gnss_last_update;
    private long initial_time_ns_raw, timestamp_ns_raw;
    private float freq_medida_Gnss;
    private int counter_Gnss, num_satellites_in_view, num_satellites_in_use;
    private static final double deltaT_update = 0.5;

    // add wifi
    private WifiManager wifiManager;
    private BroadcastReceiver wifiBroadcastReceiver;
    private Timer timerWifi;
    private String wifiCsvData;
    private long initialTimeNs;
    private long timestampNs;
    private double timestamp;
    private int wifiCounter;
    private float measuredWifiFreq;
    private double lastWifiTimestamp;
    private FileWriter fout;
    private TextView wifiTextView;
    private TextView wifiStatusView;
    private TextView wifiInfoView;
    private String wifiFeaturesText;
    private TextView wifiConnectionView;
    private TimerTask scanTaskWifi;
    private final Handler handlerWifi = new Handler();
    private final int SCAN_INTERVAL_MS = 5000; // Adjust this value to set the desired scan interval (3-10 seconds)


    // For recording the sensor values
    private boolean sensorRunning = false;

    // Layout Views
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
    String wifiStr;

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

    // For sensors with 1 sample per second (needs to be unique as it sometimes sample 4 samples per
    // second
    private double lastBaroTimestamp = 0;
    private double lastLightTimestamp = 0;
    private double lastProxTimestamp = 0;

    // String to be saved as CSV format
    private StringBuilder accelCsv = new StringBuilder();
    private StringBuilder gyroCsv = new StringBuilder();
    private StringBuilder magnetoCsv = new StringBuilder();
    private StringBuilder baroCsv = new StringBuilder();
    private StringBuilder lightCsv = new StringBuilder();
    private StringBuilder proxCsv = new StringBuilder();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        initialization();
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

        gnssStatusCallback = new GnssStatusCallback();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the required permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // Permission is already granted, register the GnssStatusCallback
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, locationListener);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        gnssStatusView = findViewById(R.id.gnssStatusView);
        gnssInfoView = findViewById(R.id.gnssInfoView);

        wifiInfoView = findViewById(R.id.wifiInfoView);
        wifiStatusView = findViewById(R.id.wifiStatusView);
        wifiConnectionView = findViewById(R.id.wifiConnectionView);
        wifiTextView = findViewById(R.id.wifiTextView);

        //Time settings
        mChronometer = findViewById(R.id.chronometer);

        sensorsInformation();
        graphViews();
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

        //--- For Wi-Fi data---//
        // Check the Wi-Fi services
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //--- for Location GNSS data ---//
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    private void setWiFiScanHandler() {
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) { // if Wi-Fi is not enabled, turn it on
                wifiStatusView.setText(" WIFI: Switched OFF");
                wifiStatusView.setBackgroundColor(0xFFFF0000); // red color (later the timer will turn it to green)
                wifiStr = "Not available ";
                if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) { // if Wi-Fi is not already in the process of being enabled, turn it on
                    wifiManager.setWifiEnabled(true); // turn Wi-Fi on
                }
            } else {
                wifiStatusView.setText(" WIFI: Switched ON");
                wifiStr = " WiFi MAC address: " + wifiManager.getConnectionInfo().getMacAddress();
            }
        } else {
            wifiStatusView.setText(" WIFI: Not available");
            wifiStatusView.setBackgroundColor(0xFFFF0000); // red color
            wifiStr = "No Features";
        }
        wifiInfoView.setText(wifiStr);

        // Register a broadcast receiver that listens for WiFi scan results.
        wifiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long rawTimestampNs = System.nanoTime();
                if (rawTimestampNs >= initialTimeNs) {
                    timestampNs = rawTimestampNs - initialTimeNs;
                } else {
                    timestampNs = (rawTimestampNs - initialTimeNs) + Long.MAX_VALUE;
                }
                timestamp = ((double) (timestampNs)) * 1E-9;

                List<ScanResult> results = wifiManager.getScanResults();
                int numAPs = results.size();
//                Log.i("WiFi Scan", " Number of APs: " + numAPs);
                String wifiStr = "";

                // Get the elapsed time from the chronometer in milliseconds
                long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();

                // Convert the elapsed time to seconds
                double elapsedSeconds = elapsedMillis / 1000.0;

                for (ScanResult result : results) {
                    String SSID = result.SSID, BSSID = result.BSSID;
                    int frequency = result.frequency;
                    long timeUs = result.timestamp;
                    double sensorTimestamp = ((double) (timeUs)) * 1E-6;
                    int RSS = result.level;
                    wifiStr = wifiStr + "\n\t- " + SSID + ",\t" + BSSID + ",\tRSS:" + RSS + " dBm";
                    // Updating the wifi data for CSV
                    wifiCsvData += String.format(Locale.US, "%.5f,%s,%s,%d,%d\n", elapsedSeconds, SSID, BSSID, RSS, frequency);
                }

                String text = "\tNumber of Wifi APs: " + numAPs + wifiStr;
                wifiTextView.setText(text);

                wifiCounter++;
                measuredWifiFreq = (float) (0.8 * measuredWifiFreq + 0.2 / (timestamp - lastWifiTimestamp));
                lastWifiTimestamp = timestamp;

                if (wifiManager.isWifiEnabled()) {
                    wifiStatusView.setBackgroundColor(0xFF00FF00);
                    wifiStatusView.setText(" WIFI: Switched ON");
                    wifiFeaturesText = " WiFi MAC address: " + wifiManager.getConnectionInfo().getMacAddress();
                    wifiInfoView.setText(wifiFeaturesText);
                }

                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getBSSID() != null) {
                    String displayString = String.format(Locale.US, "\tConnected to: %s\n\tBSSID: %s\n\tRSSI: %d dBm \n\tLinkSpeed: %d Mbps\n\t\t\t\t\t\t\t\tFreq: %5.1f Hz", wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getRssi(), wifiInfo.getLinkSpeed(), measuredWifiFreq);
                    wifiConnectionView.setText(displayString);
                } else {
                    wifiConnectionView.setText(" No connection detected");
                }
            }
        };

        scanTaskWifi = new TimerTask() {
            public void run() {
                handlerWifi.post(new Runnable() {
                    public void run() {
                        if (wifiManager != null && wifiManager.isWifiEnabled()) {
                            wifiManager.startScan();
                        }
                    }
                });
            }
        };
    }

    private void setLocationScanHandler(){
        // Display general GNSS data:
        if (locationManager != null) {
            LocationProvider provider = null;

            gnssFeaturesText = "";
            gnssStatusView.setText(" GNSS: Location Service (GPS/Network)");

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

    //======================onLocationChanged==================================
    // Called when location has changed
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude(), longitude = location.getLongitude(), altitude = location.getAltitude();
            float bearing = location.getBearing(), accuracy = location.getAccuracy(), speed = location.getSpeed();
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

            num_satellites_in_view = satellitesInView;
            num_satellites_in_use = satellitesInUse;
        }
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

    // Basic UI to start/stop recording method
    public void onClickStartButton(View view){
        // askPermission
        askPermission();

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

        setWiFiScanHandler();
        // Launch the timer to scan for Wi-Fi every 3 seconds
        timerWifi = new Timer("Timer Thread WiFiScan");
        timerWifi.scheduleAtFixedRate(scanTaskWifi, 3000, 3000); // call the timer every 3 seconds (with a delay of 2 seconds)

        setLocationScanHandler();
        // Unregister the sensor listener if it's already registered
        sm.unregisterListener(this);

        // Register the sensor listener
        registeringListeners();
    }

    public void onClickStopButton(View view){
        mChronometer.stop();

        if(sensorRunning){
            // Unregister the sensor listener
            unregisteringListeners();

            // Stop the WiFi scanning timer
            timerWifi.cancel();
            timerWifi.purge();

            // Save all the sensors in a single csv file named by the user
            showSaveDialogAllData();
        }
        sensorRunning = false;
    }

    private void showSaveDialogAllData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a file name for all data");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String fileName = input.getText().toString().trim();
                if (!fileName.isEmpty()) {
                    saveAllDataToFile(fileName);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a valid file name", Toast.LENGTH_SHORT).show();
                }
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

    private void saveAllDataToFile(String fileName) {
        List<List<String>> allData = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        List<String> combinedData = new ArrayList<>();
        List<Integer> headerCounts = new ArrayList<>();
        int maxLines = 0;

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

        splitAndAddData.accept(accelCsv, accelLines);
        splitAndAddData.accept(baroCsv, baroLines);
        splitAndAddData.accept(lightCsv, lightLines);
        splitAndAddData.accept(proxCsv, proxLines);
        splitAndAddData.accept(gyroCsv, gyroLines);
        splitAndAddData.accept(magnetoCsv, magnetoLines);
        splitAndAddData.accept(new StringBuilder(wifiCsvData), wifiLines);
        splitAndAddData.accept(locationDataSB, locationLines);

        allData.addAll(Arrays.asList(accelLines, baroLines, lightLines, proxLines, gyroLines, magnetoLines, wifiLines, locationLines));
        headers.addAll(Arrays.asList("AccelTimestamp", "AccelX", "AccelY", "AccelZ", "BaroTimestamp", "Baro", "LightTimestamp", "Light", "ProxTimestamp", "Proximity", "GyroTimestamp", "GyroX", "GyroY", "GyroZ", "MagnetoTimestamp", "MagnetoX", "MagnetoY", "MagnetoZ", "WiFiTimestamp", "SSID", "BSSID", "RSS", "frequency", "LocationTimestamp", "Latitude", "Longitude", "Altitude", "Accuracy", "Bearing", "Speed", "Provider"));
        headerCounts.addAll(Arrays.asList(4, 2, 2, 2, 4, 4, 5, 10));

        // Calculate the maximum number of lines from all sensor data lists
        maxLines = allData.stream().mapToInt(List::size).max().orElse(0);

        // Combine sensor data and skip empty columns where needed
        for (int i = 0; i < maxLines; i++) {
            StringBuilder line = new StringBuilder();
            int currentHeaderIndex = 0;
            for (int dataIndex = 0; dataIndex < allData.size(); dataIndex++) {
                List<String> lines = allData.get(dataIndex);
                int skipColumns = 0;
                if (i < lines.size()) {
                    String[] values = lines.get(i).split(",");
                    for (int j = 0; j < values.length; j++) {
                        line.append(values[j]);
                        line.append(",");
                        currentHeaderIndex++;
                    }
                } else {
                    int columnCount = headerCounts.get(dataIndex);
                    skipColumns = columnCount;
                    currentHeaderIndex += columnCount;
                }
                // Append commas to skip empty columns
                for (int j = 0; j < skipColumns; j++) {
                    line.append(",");
                }
            }
            combinedData.add(line.toString());
        }

        // Write combined data to CSV file
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




    // Implementing the methods for sensor accuracy, resume, pause
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // set to 100 for graph update to not consume too much RAM on the background
        int maxDataPoints = 100;
        if (sensorRunning) {
            long elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
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
    protected void onResume() {
        super.onResume();
        if(sensorRunning) {
            registeringListeners();

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
        if(sensorRunning) {
            unregisteringListeners();
        }
    }

    private void registeringListeners(){
        // Sensors with sampling rate of 100 samples per second
        sm.registerListener(this, accelSensor, 10000);
        sm.registerListener(this, gyroSensor,10000);
        sm.registerListener(this, magnetoSensor, 10000);

        // Sensors with sampling rate of 1 sample per second
        sm.registerListener(this, baroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_FASTEST);

        // registering the wifi
        //............. Register WiFi .................
        if (wifiManager!=null)
        {
            registerReceiver(wifiBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)  );
        }
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

    private void unregisteringListeners(){
        sm.unregisterListener(this,accelSensor);
        sm.unregisterListener(this, gyroSensor);
        sm.unregisterListener(this,magnetoSensor);
        sm.unregisterListener(this,baroSensor);
        sm.unregisterListener(this,lightSensor);
        sm.unregisterListener(this,proxSensor);

        if (wifiManager != null) {
            unregisterReceiver(wifiBroadcastReceiver);
        }
        // Remove location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
        }
    }
}
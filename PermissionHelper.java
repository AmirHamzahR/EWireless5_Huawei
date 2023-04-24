package com.example.datacollectionapp_afp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.location.GnssStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class PermissionHelper {

    public static void askPermission(Activity activity, LocationManager locationManager, GnssStatus.Callback gnssStatusCallback ,LocationListener locationListener) {
        // ask for activity recognition
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            // ask for permission
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, Sensor.TYPE_ACCELEROMETER);
        }
        // ask for body sensors
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BODY_SENSORS}, Sensor.TYPE_GYROSCOPE);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the required permission
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.REQUEST_LOCATION_PERMISSION);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MainActivity.REQUEST_LOCATION_PERMISSION);
        } else {
            // Permission is already granted, register the GnssStatusCallback
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, locationListener);
        }

        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}


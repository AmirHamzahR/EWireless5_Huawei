package com.example.pdr_test;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.location.LocationManager;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import DataCollection.PermissionHelper;
import DataCollection.SensorManagerHelper;
import DataCollection.WifiManagerHelper;

public class SortingViews {
    TrajectoryView trajectoryView_global;
    SensorManagerHelper sensorManagerHelper_global;

    public static void setupProcessNoiseSlider(MainActivity mainActivity, IMUCalculations imu_calculations) {
        SeekBar processNoiseSlider = mainActivity.findViewById(R.id.process_noise_slider);
        processNoiseSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert slider progress to a float value in a suitable range, e.g., 0.001 to 1
                float processNoise = progress / 1000f;
                imu_calculations.processNoise = processNoise;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public static void setupMeasurementNoiseSlider(MainActivity mainActivity, IMUCalculations imu_calculations) {
        SeekBar measurementNoiseSlider = mainActivity.findViewById(R.id.measurement_noise_slider);
        measurementNoiseSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert slider progress to a float value in a suitable range, e.g., 0.001 to 1
                float measurementNoise = progress / 1000f;
                imu_calculations.measurementNoise = measurementNoise;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public static void setupToggleMoveMarkerButton(MainActivity mainActivity, TrajectoryView trajectoryView, IMUCalculations imu_calculations, SensorManagerHelper sensorManagerHelper, WifiManagerHelper wifiManagerHelper) {
        Button toggleMoveMarkerButton = mainActivity.findViewById(R.id.toggle_move_marker_button);
        toggleMoveMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trajectoryView.markerSet) {
                    float[] center = new float[]{trajectoryView.getWidth() / 2f, trajectoryView.getHeight() / 2f};
                    float[] centerInFloorPlan = new float[2];
                    Matrix invertedMatrix = new Matrix();
                    trajectoryView.matrix.invert(invertedMatrix);
                    invertedMatrix.mapPoints(centerInFloorPlan, center);
                    trajectoryView.setMarker(centerInFloorPlan[0], centerInFloorPlan[1]);
                    trajectoryView.setStartPosition();
                    imu_calculations.setStartPosition(centerInFloorPlan[0], centerInFloorPlan[1]); // Call the setStartPosition method in IMUCalculations
                    // Start PDR updates here
                    mainActivity.startPDR = true;

                    if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Register the GnssStatusCallback
                        try {
                            mainActivity.locationManager.registerGnssStatusCallback(mainActivity.gnssStatusCallback, null);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }

                        // Register the LocationListener with default minTime and minDistance values
                        try {
                            mainActivity.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, mainActivity.locationListener);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
                    }

                    mainActivity.mChronometer.setBase(SystemClock.elapsedRealtime()); // reset to 0
                    mainActivity.mChronometer.start();

                    sensorManagerHelper.sensorRunning = true;

                    // resetting the timestamps
                    sensorManagerHelper.accelTimestamp = 0.0;
                    sensorManagerHelper.accelTimestampUncalibrated = 0.0;
                    sensorManagerHelper.gyroTimestamp = 0.0;
                    sensorManagerHelper.gyroTimestampUncalibrated = 0.0;
                    sensorManagerHelper.magnetoTimestamp = 0.0;
                    sensorManagerHelper.magnetoTimestampUncalibrated = 0.0;
                    sensorManagerHelper.baroTimestamp = 0.0;
                    sensorManagerHelper.lightTimestamp = 0.0;
                    sensorManagerHelper.proxTimestamp = 0.0;

                    wifiManagerHelper.setWiFiScanHandler();

                    mainActivity.setLocationScanHandler();
                    // Unregister the sensor listener if it's already registered
                    sensorManagerHelper.unregisterListeners();

                    // Register the sensor listener
                    sensorManagerHelper.registerListeners();
                    wifiManagerHelper.registerWifiReceiver();

                    Toast.makeText(v.getContext(), "PDR recording starts!", Toast.LENGTH_SHORT).show();
                    Log.d("Initial marker","Marker X" + trajectoryView.markerPosition.x + "Marker Y" + trajectoryView.markerPosition.y);
                }
            }
        });
    }

    public static void setupResetButton(MainActivity mainActivity, TrajectoryView trajectoryView, IMUCalculations imu_calculations, SensorManagerHelper sensorManagerHelper, WifiManagerHelper wifiManagerHelper, FloorChangeHandler floorChangeHandler) {
        Button resetSaveButton = mainActivity.findViewById(R.id.resetSaveButton);
        resetSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trajectoryView.resetPath(); // Replace 'trajectoryView' with the actual reference to your TrajectoryView instance
                imu_calculations.positionX = trajectoryView.markerPosition.x;
                imu_calculations.positionY = trajectoryView.markerPosition.y;
                if(sensorManagerHelper.sensorRunning){
                    // Unregister the sensor listener
                    sensorManagerHelper.unregisterListeners();

                    // Unregister the WiFi broadcast receiver
                    wifiManagerHelper.unregisterWifiReceiver();
                    mainActivity.mChronometer.stop();
                    // Stop the WiFi scanning timer
                    wifiManagerHelper.timerWifi.cancel();
                    wifiManagerHelper.timerWifi.purge();
                    if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, mainActivity.REQUEST_WRITE_STORAGE);
                    } else {
                        Log.d("Sorting View Debug: ",  " floor number: " + floorChangeHandler.getFloorNumberForSV());
                        mainActivity.showFileNameDialog();
                        mainActivity.showSaveDialogAllData();
                    }
                }
                sensorManagerHelper.sensorRunning = false;
                trajectoryView.resetPoints();
            }
        });
    }

    public static void setupChooseFloorButton(MainActivity mainActivity, FloorChangeHandler floorChangeHandler) {
        Button chooseFloorButton = mainActivity.findViewById(R.id.chooseFloorButton);
        chooseFloorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorChangeHandler.showFloorInputDialog(mainActivity);
            }
        });
    }
}


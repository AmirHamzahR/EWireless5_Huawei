package com.example.pdr_test;



import static com.example.pdr_test.IMU_calc.STEP_LENGTH;
import static com.example.pdr_test.IMU_calc.lowPassFilter;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener, FloorChangeHandler.OnFloorChangeListener{
    private static final int REQUEST_WRITE_STORAGE = 112;

    // Initialize the class that are used
    private IMU_calc imu_calc = new IMU_calc();

    private static final float STEP_THRESHOLD = 1.0f;
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

    // Views
    TrajectoryView trajectoryView;
    private Button toggleMoveMarkerButton;
    private boolean moveMarkerEnabled = false;
    public static boolean startPDR = false;

    private float previousBarometerReading = -1;
    private FloorChangeHandler floorChangeHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialization();
        askForUserHeight();
    }

    private void initialization() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        imu_calc = new IMU_calc();

        // Views
        trajectoryView = findViewById(R.id.trajectory_view);
        SeekBar processNoiseSlider = findViewById(R.id.process_noise_slider);
        SeekBar measurementNoiseSlider = findViewById(R.id.measurement_noise_slider);

        processNoiseSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert slider progress to a float value in a suitable range, e.g., 0.001 to 1
                float processNoise = progress / 1000f;
                imu_calc.processNoise = processNoise;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        measurementNoiseSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert slider progress to a float value in a suitable range, e.g., 0.001 to 1
                float measurementNoise = progress / 1000f;
                imu_calc.measurementNoise = measurementNoise;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        trajectoryView = findViewById(R.id.trajectory_view);

        toggleMoveMarkerButton = findViewById(R.id.toggle_move_marker_button);

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
                    imu_calc.setStartPosition(centerInFloorPlan[0], centerInFloorPlan[1]); // Call the setStartPosition method in IMU_calc
                    // Start PDR updates here
                    startPDR = true;
                    Toast.makeText(MainActivity.this, "PDR recording starts!", Toast.LENGTH_SHORT).show();
                    Log.d("Initial marker","Marker X" + trajectoryView.markerPosition.x + "Marker Y" + trajectoryView.markerPosition.y);
                }
            }
        });

        Button savePointsButton = findViewById(R.id.save_points_button);
        savePointsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                } else {
                    CSVExporter.savePointsToCSV(trajectoryView.points);
                }
            }
        });

        Button resetButton = findViewById(R.id.reset_button); // Replace with the actual ID of your button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trajectoryView.resetPath(); // Replace 'trajectoryView' with the actual reference to your TrajectoryView instance
                imu_calc.positionX = trajectoryView.markerPosition.x;
                imu_calc.positionY = trajectoryView.markerPosition.y;
            }
        });

        // In MainActivity's onCreate method
        FloorChangeHandler floorChangeHandler = new FloorChangeHandler(this, this);
        Button chooseFloorButton = findViewById(R.id.chooseFloorButton);
        chooseFloorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floorChangeHandler.showFloorInputDialog();
            }
        });



    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, 10000);
        sensorManager.registerListener(this, gyroscope, 10000);
        sensorManager.registerListener(this, magnetometer, 10000);
        sensorManager.registerListener(this, rotationVectorSensor, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onFloorChange(int floorResourceId) {
        trajectoryView.changeFloor(floorResourceId);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // Update Madgwick filter with accelerometer values
                // Detect steps
                if(startPDR) {
                    aVal = imu_calc.lowPassFilter(event.values, aVal);
                    imu_calc.detectStep(imu_calc, aVal);
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                // Update Madgwick filter with magnetometer values
                mVal = imu_calc.lowPassFilter(event.values, mVal);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                if (IMU_calc.stepCount > 0 && startPDR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                    SensorManager.getOrientation(rotationMatrix, orientationAngles);
                    // Update position in the IMU_calc class
                    imu_calc.updatePosition(imu_calc, trajectoryView, orientationAngles);
                    imu_calc.stepCount = 0; // Reset step count after updating position
                }
                break;
            case Sensor.TYPE_PRESSURE:
                float currentReading = event.values[0];

                if (previousBarometerReading != -1) {
                    float threshold = 0.5f; // Adjust this value based on your needs
                    floorChangeHandler.detectChangeFloor(currentReading, previousBarometerReading, threshold);
                }

                previousBarometerReading = currentReading;
        }
//        Log.d("Initial marker","Marker X" + trajectoryView.markerPosition.x + "Marker Y" + trajectoryView.markerPosition.y);

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
                STEP_LENGTH = IMU_calc.estimateStrideLength(userHeight);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CSVExporter.savePointsToCSV(trajectoryView.points);
        }
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this example
    }
}


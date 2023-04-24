package com.example.pdr_test;

import android.util.Log;

public class IMU_calc {
    TrajectoryView trajectoryView;
    private KalmanFilter kalmanFilterX;
    private KalmanFilter kalmanFilterY;

    // For detecting the steps
    public static final float STEP_THRESHOLD = 10.2f; // If lower value, higher sensitivity
    public static float STEP_LENGTH = 0.75f; // You can use an average step length or personalize it for each user
    public static int stepCount = 0;
    public float distanceTraveled = 0;

    // Calculating the user's position
    public float positionX = Float.MAX_VALUE;
    public float positionY = Float.MAX_VALUE;

    float initialX = 0;
    float initialY = 0;
    float processNoise = 0.005f; // Adjust this value based on your needs
    float measurementNoise = 0.005f; // Adjust this value based on your needs

    public IMU_calc() {
        kalmanFilterX = new KalmanFilter(processNoise, measurementNoise, initialX);
        kalmanFilterY = new KalmanFilter(processNoise, measurementNoise, initialY);
    }

    // To calculate the filtered values
    public static float[] lowPassFilter(float[] input, float[] prev) {
        if (prev == null) return input;

        float alpha = 0.8f;
        float[] filteredValues = new float[3];
        for (int i = 0; i < 3; i++) {
            filteredValues[i] = alpha * prev[i] + (1 - alpha) * input[i];
        }
        return filteredValues;
    }

    public static float estimateStrideLength(float userHeight) {
        return userHeight * 0.42f;
    }




    public void detectStep(IMU_calc imu_calc, float[] accelerometerData) {
        float magnitude = (float) Math.sqrt(accelerometerData[0] * accelerometerData[0] +
                accelerometerData[1] * accelerometerData[1] +
                accelerometerData[2] * accelerometerData[2]);

        if (magnitude > STEP_THRESHOLD) {
            imu_calc.stepCount++;
            imu_calc.distanceTraveled += STEP_LENGTH;

        }
    }

    public void setStartPosition(float x, float y) {
        positionX = x;
        positionY = y;
    }



    public void updatePosition(IMU_calc imu_calc, TrajectoryView trajectoryView, float[] orientationAngles) {
        if(imu_calc.positionX == Float.MAX_VALUE){
            imu_calc.positionX = trajectoryView.markerPosition.x;
            imu_calc.positionY = trajectoryView.markerPosition.y;
        }
        // Only updates the position x and y once when the update Position is called
        Log.d("IMU_calc", "Distance travelled: " + distanceTraveled);

        float heading = (float) -Math.toDegrees(orientationAngles[0]);
        float stepDisplacementX = (float) (imu_calc.distanceTraveled * Math.sin(Math.toRadians(heading)));
        float stepDisplacementY = (float) (imu_calc.distanceTraveled * Math.cos(Math.toRadians(heading)));

        // Rotate the step displacement based on the phone's orientation
        float phonePitch = (float) Math.toDegrees(orientationAngles[1]);
        float deltaX = stepDisplacementX * (float) Math.cos(Math.toRadians(phonePitch)) - stepDisplacementY * (float) Math.sin(Math.toRadians(phonePitch));
        float deltaY = stepDisplacementX * (float) Math.sin(Math.toRadians(phonePitch)) + stepDisplacementY * (float) Math.cos(Math.toRadians(phonePitch));

        // Apply the Kalman filters to deltaX and deltaY
        float filteredDeltaX = imu_calc.kalmanFilterX.update(deltaX);
        float filteredDeltaY = imu_calc.kalmanFilterY.update(deltaY);

        imu_calc.positionX += filteredDeltaX;
        imu_calc.positionY += filteredDeltaY;
        trajectoryView.addPoint(imu_calc.positionX, imu_calc.positionY);
        imu_calc.distanceTraveled = 0;
//        Log.d("Trajectory values", "Pos x:" + positionX + "Pos y:" + positionY);
    }

}

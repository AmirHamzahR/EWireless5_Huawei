package com.example.pdr_test;

public class KalmanFilter {
    private float processNoise;
    private float measurementNoise;
    private float estimationError;
    private float currentEstimate;
    private float currentError;

    public KalmanFilter(float processNoise, float measurementNoise, float initialEstimate) {
        this.processNoise = processNoise;
        this.measurementNoise = measurementNoise;
        this.currentEstimate = initialEstimate;
        this.currentError = Float.MAX_VALUE;
    }

    public float update(float measurement) {
        float kalmanGain = currentError / (currentError + measurementNoise);
        currentEstimate = currentEstimate + kalmanGain * (measurement - currentEstimate);
        currentError = (1 - kalmanGain) * (currentError + processNoise);

        return currentEstimate;
    }
}


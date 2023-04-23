package com.example.datacollectionapp_afp;

import android.hardware.Sensor;

public class SensorInformationHelper {

    public static String getSensorInfo(Sensor sensor, String unit, String noSensorMessage) {
        if (sensor != null) {
            return sensor.getName() +
                    " \n Manufacturer: " + sensor.getVendor()
                    + ", Version: " + sensor.getVersion()
                    + ", Type:" + sensor.getType()
                    + ", Resolution: " + sensor.getResolution() + " " + unit
                    + ", Max. Range: " + sensor.getMaximumRange() + " " + unit
                    + ", Power consumption: " + sensor.getPower() + " mA"
                    + ", Min. Delay: " + sensor.getMinDelay();
        } else {
            return noSensorMessage;
        }
    }
}


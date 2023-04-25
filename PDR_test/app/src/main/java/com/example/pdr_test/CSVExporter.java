package com.example.pdr_test;

import android.content.Context;
import android.graphics.PointF;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CSVExporter {
    public static void savePointsToCSV(StringBuilder pointsCsv, StringBuilder positionVal, String fileName, Context context) {
        File directory = context.getExternalFilesDir(null);
        File file = new File(directory, "Traj_" + fileName + ".csv");

        // Headers for the CSV file
        List<String> headers = Arrays.asList("x[m]", "y[m]", "time[ms]", "floor", "building");

        // Prepare data for saving
        List<String> allData = new ArrayList<>();
        String[] pointsValues = pointsCsv.toString().split("\n");
        String[] positionValues = positionVal.toString().split("\n");
        Log.d("CSVExporter", "Points count: " + pointsValues.length);
        Log.d("CSVExporter", "PositionValues count: " + positionValues.length);
        for (int i = 0; i < pointsValues.length; i++) {
            String[] pointValuesSplit = pointsValues[i].split(",");
            String x = pointValuesSplit[0];
            String y = pointValuesSplit[1];
            String[] positionValuesSplit = positionValues[i].split(",");
            String time = positionValuesSplit[0];
            String floor = positionValuesSplit[1];
            String building = positionValuesSplit[2];
            String line = String.join(",", x, y, time, floor, building);
            allData.add(line);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])))) {

            // Write data lines
            for (String line : allData) {
                String[] values = line.split(",");
                csvPrinter.printRecord(Arrays.asList(values));
            }

            csvPrinter.flush();
            Log.d("CSVExporter", "Data saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CSVExporter", "Error writing points to CSV file", e);
        }
    }
}


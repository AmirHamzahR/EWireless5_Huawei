package com.example.pdr_test;

import static java.security.AccessController.getContext;

import android.graphics.PointF;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class CSVExporter {
    public static void savePointsToCSV(List<PointF> points) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, "points.csv");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            osw.write("x,y\n");
            for (PointF point : points) {
                osw.write(point.x + "," + point.y + "\n");
            }
            osw.close();
            fos.close();
            Log.d("CSVExporter", "Points saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CSVExporter", "Error writing points to CSV file", e);
        }
    }
}


//package com.example.datacollectionapp_afp;
//
//import android.graphics.Color;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.jjoe64.graphview.GraphView;
//import com.jjoe64.graphview.series.DataPoint;
//import com.jjoe64.graphview.series.LineGraphSeries;
//
//public class GraphManager {
//    private AppCompatActivity activity;
//
//    public GraphManager(AppCompatActivity activity) {
//        this.activity = activity;
//    }
//
//    public void setupGraphs() {
//        GraphView accelGraph = activity.findViewById(R.id.accelgraph);
//        GraphView gyroGraph = activity.findViewById(R.id.gyroGraph);
//        GraphView magnetoGraph = activity.findViewById(R.id.magnetoGraph);
//        GraphView baroGraph = activity.findViewById(R.id.baroGraph);
//        GraphView lightGraph = activity.findViewById(R.id.lightGraph);
//        GraphView proxGraph = activity.findViewById(R.id.proxGraph);
//
//        // Configure viewport settings
//        accelGraph.getViewport().setXAxisBoundsManual(true);
//        accelGraph.getViewport().setMinX(0);
//        accelGraph.getViewport().setMaxX(1); // Adjust this value to set the viewport width
//        accelGraph.getViewport().setScrollable(true); // Enables horizontal scrolling
//
//        // data from accelerometer
//        accelX = new LineGraphSeries<>();
//        accelY = new LineGraphSeries<>();
//        accelZ = new LineGraphSeries<>();
//
//        // Adding accelerometer values into the graph
//        graphSettings(accelX, Color.RED, 8, accelGraph);
//        graphSettings(accelY, Color.GREEN, 8, accelGraph);
//        graphSettings(accelZ, Color.BLUE, 8, accelGraph);
//
//        // Data from gyroscope
//        gyroX = new LineGraphSeries<>();
//        gyroY = new LineGraphSeries<>();
//        gyroZ = new LineGraphSeries<>();
//
//        // Adding gyroscope values into the graph
//        graphSettings(gyroX, Color.RED, 8, gyroGraph);
//        graphSettings(gyroY, Color.GREEN, 8, gyroGraph);
//        graphSettings(gyroZ, Color.BLUE, 8, gyroGraph);
//
//        // Data from magnetometer
//        magnetoX = new LineGraphSeries<>();
//        magnetoY = new LineGraphSeries<>();
//        magnetoZ = new LineGraphSeries<>();
//
//        // Adding magnetometer values into the graph
//        graphSettings(magnetoX, Color.RED, 8, magnetoGraph);
//        graphSettings(magnetoY, Color.GREEN, 8, magnetoGraph);
//        graphSettings(magnetoZ, Color.BLUE, 8, magnetoGraph);
//
//        // Data from barometer
//        baroSeries = new LineGraphSeries<>();
//
//        // Adding barometer values into the graph
//        graphSettings(baroSeries, Color.RED, 8, baroGraph);
//
//        // Data from light sensor
//        lightSeries = new LineGraphSeries<>();
//
//        // Adding light sensor values into the graph
//        graphSettings(lightSeries, Color.RED, 8, lightGraph);
//
//        // Data from proximity sensor
//        proxSeries = new LineGraphSeries<>();
//
//        // Adding proximity sensor values into the graph
//        graphSettings(proxSeries, Color.RED, 8, proxGraph);
//    }
//
//    // You can also move other graph-related methods to this class, like graphSettings().
//    public void graphSettings(LineGraphSeries<DataPoint> series, int color, int thickness, GraphView graph) {
//        series.setColor(color);
//        series.setThickness(thickness);
//        graph.addSeries(series);
//    }
//}

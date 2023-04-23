package com.example.datacollectionapp_afp;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphViewHelper {

    public static void graphSettings(GraphView graphView) {
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(1);
        graphView.getViewport().setScrollable(true);
    }

    static void dataSettings(LineGraphSeries data, String str, int color, int thickness, GraphView graph){
        data.setColor(color);
        data.setThickness(thickness);
        graph.addSeries(data);
        data.setTitle(str);
        graph.getLegendRenderer().setVisible(true);
    }
}

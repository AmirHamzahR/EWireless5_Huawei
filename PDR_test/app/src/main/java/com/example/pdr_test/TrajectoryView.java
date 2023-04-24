package com.example.pdr_test;

import static com.example.pdr_test.MainActivity.startPDR;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.example.pdr_test.R;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryView extends View {
    public Paint paint;
    public Path path;
    public List<PointF> points = new ArrayList<>();

    public Bitmap floorPlan;
    public Matrix matrix = new Matrix();
    public ScaleGestureDetector scaleDetector;
    public float[] lastTouch = new float[2];

    public PointF markerPosition = new PointF();
    public boolean markerSet = false;
    public Paint markerPaint;
    private float pathLengthScaleFactor = 0.6f; // Adjust this value to make the path longer or shorter


    public TrajectoryView(Context context) {
        super(context);
        init(context);
    }

    public TrajectoryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7);
        path = new Path();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        floorPlan = BitmapFactory.decodeResource(getResources(), R.drawable.groundfloor, options);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                float dx = (getWidth() - floorPlan.getWidth()) / 2f;
                float dy = (getHeight() - floorPlan.getHeight()) / 2f;
                matrix.postTranslate(dx, dy);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                invalidate();
                return true;
            }
        });

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(Color.RED);
        markerPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.setMatrix(matrix);

        // Draw floor plan
        canvas.drawBitmap(floorPlan, 0, 0, null);

        // Draw path
        path.reset();
        if (!points.isEmpty()) {
            PointF startPoint = points.get(0);
            path.moveTo(startPoint.x, startPoint.y);
            for (int i = 1; i < points.size(); i++) {
                PointF prevPoint = points.get(i - 1);
                PointF point = points.get(i);
                float dx = (point.x - prevPoint.x) * pathLengthScaleFactor;
                float dy = (point.y - prevPoint.y) * pathLengthScaleFactor;
                path.lineTo(prevPoint.x + dx, prevPoint.y + dy);
            }
        }

        // Draw marker
        if (markerSet) {
            canvas.drawCircle(markerPosition.x, markerPosition.y, 10, markerPaint);
        } else {
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float[] center = new float[]{centerX, centerY};
            float[] centerInFloorPlan = new float[2];
            Matrix invertedMatrix = new Matrix();
            matrix.invert(invertedMatrix);
            invertedMatrix.mapPoints(centerInFloorPlan, center);
            canvas.drawCircle(centerInFloorPlan[0], centerInFloorPlan[1], 10, markerPaint);
        }
        canvas.drawPath(path, paint);

        canvas.restore();

    }

    public void setStartPosition() {
        if (markerSet) {
            PointF startPosition = new PointF(markerPosition.x, markerPosition.y);
            points.clear();
            points.add(startPosition);
            invalidate();
        }
    }

    public void changeFloor(int floorResourceId) {
        resetPath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        floorPlan = BitmapFactory.decodeResource(getResources(), floorResourceId, options);
        Toast.makeText(getContext(), "Changing floors detected. Resetting everything, saving!", Toast.LENGTH_SHORT).show();
        invalidate(); // Redraw the view
    }


    public void resetPath() {
        points.clear();
        CSVExporter.savePointsToCSV(this.points);
        markerSet = false;
        startPDR = false;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch[0] = x;
                lastTouch[1] = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    float dx = x - lastTouch[0];
                    float dy = y - lastTouch[1];
                    matrix.postTranslate(dx, dy);
                    invalidate();
                    lastTouch[0] = x;
                    lastTouch[1] = y;
                }
                break;
        }

        return true;
    }

    public void setMarker(float x, float y) {
        float scaledX = x;
        float scaledY = y;
        markerPosition.set(scaledX, scaledY);
        markerSet = true;
        invalidate();
    }


    public int getPointCount() {
        return points.size();
    }

    public void addPoint(float x, float y) {
        float scaledX = x;; // Adjust scaleFactor if needed
        float scaledY = y; // Adjust scaleFactor if needed

        points.add(new PointF(scaledX, scaledY));
        invalidate();
    }
}


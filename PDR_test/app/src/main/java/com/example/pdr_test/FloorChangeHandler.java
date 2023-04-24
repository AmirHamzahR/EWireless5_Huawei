package com.example.pdr_test;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

public class FloorChangeHandler {

    public interface OnFloorChangeListener {
        void onFloorChange(int floorResourceId);
    }

    TrajectoryView trajectoryView;
    private ChangeListener changeListener;
    private Context context;
    private OnFloorChangeListener onFloorChangeListener;

    public FloorChangeHandler(Context context, OnFloorChangeListener onFloorChangeListener) {
        this.context = context;
        this.onFloorChangeListener = onFloorChangeListener;
    }

    void showFloorInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter floor number");

        // Set up the input
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int floorNumber = Integer.parseInt(input.getText().toString());
                changeFloorBasedOnInput(floorNumber);
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



    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void detectChangeFloor(float currentReading, float previousReading, float threshold) {
        int floorChange = 0;

        if (Math.abs(currentReading - previousReading) > threshold) {
            if (currentReading > previousReading) {
                floorChange = 1; // Going up
            } else {
                floorChange = -1; // Going down
            }

            trajectoryView.changeFloor(floorChange);
            if (changeListener != null) {
                changeListener.onFloorChanged(floorChange);
            }
        }
    }

    public interface ChangeListener {
        void onFloorChanged(int floorChange);
    }


    private void changeFloorBasedOnInput(int floorNumber) {
        // Add logic to map the floor number to the corresponding resource ID
        int floorResourceId;
        switch (floorNumber) {
            case 1:
                floorResourceId = R.drawable.lowergroundfloor;
                break;
            case 2:
                floorResourceId = R.drawable.groundfloor;
                break;
            case 3:
                floorResourceId = R.drawable.firstfloor;
                break;
            case 4:
                floorResourceId = R.drawable.secondfloor;
                break;
            case 5:
                floorResourceId = R.drawable.thirdfloor;
                break;
            // Add more cases for other floors
            default:
                // Show an error message or set a default floor
                floorResourceId = R.drawable.groundfloor;
                break;
        }

        onFloorChangeListener.onFloorChange(floorResourceId);
    }

}

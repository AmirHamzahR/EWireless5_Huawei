package com.example.pdr_test;

import static com.example.pdr_test.MainActivity.selectedBuilding;

import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

public class FloorChangeHandler {

    TrajectoryView trajectoryView;
    private ChangeListener changeListener;
    private Context context;
    private OnFloorChangeListener onFloorChangeListener;
    private MainActivity mainActivity;
    protected int floorNumberForSV = 1;

    public FloorChangeHandler() {
    }

    public int getFloorNumberForSV() {
        return floorNumberForSV;
    }



    public FloorChangeHandler(Context context) {
        this.context = context;
    }

    public void setOnFloorChangeListener(OnFloorChangeListener listener) {
        this.onFloorChangeListener = listener;
    }

    private void changeFloorBasedOnInput(int floorNumber, String building) {
        // Add logic to map the floor number to the corresponding resource ID
        int floorResourceId;
        switch(building){
            case "Nucleus Building":
                switch (floorNumber) {
                    case -1:
                        floorResourceId = R.drawable.lowergroundfloor;
                        break;
                    case 0:
                        floorResourceId = R.drawable.groundfloor;
                        break;
                    case 1:
                        floorResourceId = R.drawable.firstfloor;
                        break;
                    case 2:
                        floorResourceId = R.drawable.secondfloor;
                        break;
                    case 3:
                        floorResourceId = R.drawable.thirdfloor;
                        break;
                    // Add more cases for other floors
                    default:
                        // Show an error message or set a default floor
                        floorResourceId = R.drawable.groundfloor;
                        break;
                }
                break;
            case "Murray Library":
                switch (floorNumber) {
                    case 0:
                        floorResourceId = R.drawable.murray_library_ground_floor;
                        break;
                    case 1:
                        floorResourceId = R.drawable.murray_library_first_floor;
                        break;
                    case 2:
                        floorResourceId = R.drawable.murray_library_second_floor;
                        break;
                    case 3:
                        floorResourceId = R.drawable.murray_library_third_floor;
                        break;
                    default:
                        // Show an error message or set a default floor
                        floorResourceId = R.drawable.murray_library_ground_floor;
                        break;
                }
                break;
            default:
                // Show an error message or set a default floor for an unknown building
                floorResourceId = R.drawable.groundfloor;
                break;
        }

        if (onFloorChangeListener != null) {
            onFloorChangeListener.onFloorChange(floorResourceId, building);
        } else {
            Log.e("FloorChangeHandler", "OnFloorChangeListener is not set");
        }
        floorNumberForSV = floorNumber;
    }

    public interface OnFloorChangeListener {
        void onFloorChange(int floorResourceId, String building);
    }

    void showFloorInputDialog(Context context) {
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
                changeFloorBasedOnInput(floorNumber, selectedBuilding);
                floorNumberForSV = floorNumber;
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

            trajectoryView.changeFloor(floorChange, selectedBuilding);
            if (changeListener != null) {
                changeListener.onFloorChanged(floorChange);
            }
        }
    }

    public interface ChangeListener {
        void onFloorChanged(int floorChange);
    }

}

package com.example.pdr_test;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import DataCollection.PermissionHelper;

public class InitialMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_menu);

        Button nucleusBuildingButton = findViewById(R.id.button_nucleus_building);
        Button murrayLibraryButton = findViewById(R.id.button_murray_library);

        nucleusBuildingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMainActivity("Nucleus Building");
            }
        });

        murrayLibraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMainActivity("Murray Library");
            }
        });
    }

    private void startMainActivity(String building) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("building", building);
        startActivity(intent);
    }
}

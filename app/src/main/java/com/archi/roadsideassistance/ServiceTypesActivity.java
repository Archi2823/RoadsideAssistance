
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ServiceTypesActivity extends AppCompatActivity {

    private LinearLayout cardGeneralService;
    private LinearLayout cardAnnualService;
    private LinearLayout cardInspection;

    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_types);

        cardGeneralService = findViewById(R.id.cardGeneralService);
        cardAnnualService = findViewById(R.id.cardAnnualService);
        cardInspection = findViewById(R.id.cardInspection);

        btnBack = findViewById(R.id.btnBack);

        // General Service
        cardGeneralService.setOnClickListener(v ->
                showServiceDetails(
                        "General Service",
                        "General vehicle servicing includes routine maintenance, " +
                                "engine oil and fluid checks, basic cleaning, and general " +
                                "vehicle condition checks."
                )
        );

        // Annual Service
        cardAnnualService.setOnClickListener(v ->
                showServiceDetails(
                        "Annual Service",
                        "Annual servicing is a periodic vehicle maintenance check. " +
                                "It can include engine, battery, brake, tyre, fluid and " +
                                "other important vehicle checks."
                )
        );

        // Vehicle Inspection
        cardInspection.setOnClickListener(v ->
                showServiceDetails(
                        "Vehicle Inspection",
                        "Vehicle inspection helps check the overall condition " +
                                "of your vehicle, including tyres, brakes, lights, battery " +
                                "and other important safety components."
                )
        );

        // Back to previous screen
        btnBack.setOnClickListener(v -> finish());
    }

    private void showServiceDetails(String title, String description) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(description)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }
}

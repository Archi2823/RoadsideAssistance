
package com.archi.roadsideassistance;

import android.content.Intent;
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

        cardGeneralService.setOnClickListener(v ->
                showServiceDetails(
                        "General Service",
                        "General vehicle servicing includes routine maintenance, " +
                                "engine oil and fluid checks, basic cleaning, and general " +
                                "vehicle condition checks."
                )
        );

        cardAnnualService.setOnClickListener(v ->
                showServiceDetails(
                        "Annual Service",
                        "Annual servicing is a periodic vehicle maintenance check. " +
                                "It can include engine, battery, brake, tyre, fluid and " +
                                "other important vehicle checks."
                )
        );

        cardInspection.setOnClickListener(v ->
                showServiceDetails(
                        "Inspection",
                        "Vehicle inspection helps check the overall condition " +
                                "of your vehicle, including tyres, brakes, lights, battery " +
                                "and other important safety components."
                )
        );

        btnBack.setOnClickListener(v -> finish());
    }

    private void showServiceDetails(String title, String description) {

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(description)
                .setNegativeButton("Close", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Book Now", (dialog, which) ->
                        openBookingScreen(title))
                .show();
    }

    private void openBookingScreen(String serviceType) {

        Intent intent = new Intent(
                ServiceTypesActivity.this,
                ServiceBookingActivity.class
        );

        intent.putExtra("serviceType", serviceType);
        startActivity(intent);
    }
}

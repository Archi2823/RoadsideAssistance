package com.archi.roadsideassistance;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BatteryHelpActivity extends AppCompatActivity {

    private Spinner spVehicleType, spBatteryIssue;
    private Spinner spFuelType, spFuelQuantity;

    private EditText etBatteryLocation, etBatteryDescription;
    private TextView tvBatteryMessage, tvFuelQuantityLabel;
    private Button btnSubmitBatteryRequest;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final String[] quantities = {"2", "5", "10", "15"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spVehicleType = findViewById(R.id.spVehicleType);
        spBatteryIssue = findViewById(R.id.spBatteryIssue);
        spFuelType = findViewById(R.id.spFuelType);
        spFuelQuantity = findViewById(R.id.spFuelQuantity);

        etBatteryLocation = findViewById(R.id.etBatteryLocation);
        etBatteryDescription = findViewById(R.id.etBatteryDescription);

        tvBatteryMessage = findViewById(R.id.tvBatteryMessage);
        tvFuelQuantityLabel = findViewById(R.id.tvFuelQuantityLabel);

        btnSubmitBatteryRequest =
                findViewById(R.id.btnSubmitBatteryRequest);

        setupSpinner(
                spVehicleType,
                new String[]{
                        "Select vehicle type",
                        "Bike", "Car", "SUV", "Truck", "Other"
                }
        );

        setupSpinner(
                spBatteryIssue,
                new String[]{
                        "Select battery issue",
                        "Battery dead",
                        "Vehicle not starting",
                        "Jump-start required",
                        "Battery replacement required",
                        "Other"
                }
        );

        setupSpinner(
                spFuelType,
                new String[]{
                        "Select fuel type",
                        "Petrol",
                        "Diesel",
                        "CNG",
                        "EV Charging"
                }
        );

        setupSpinner(
                spFuelQuantity,
                new String[]{
                        "Select quantity",
                        "2", "5", "10", "15"
                }
        );

        spFuelType.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        updateQuantityUnit();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        btnSubmitBatteryRequest.setOnClickListener(
                v -> submitRequest()
        );
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                options
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void updateQuantityUnit() {
        if (spFuelType == null || tvFuelQuantityLabel == null) {
            return;
        }

        String fuelType = spFuelType.getSelectedItem().toString();

        if ("CNG".equals(fuelType)) {
            tvFuelQuantityLabel.setText("Fuel Quantity (kg)");
        } else if ("EV Charging".equals(fuelType)) {
            tvFuelQuantityLabel.setText("Charging Quantity (kWh)");
        } else {
            tvFuelQuantityLabel.setText("Fuel Quantity (litres)");
        }
    }

    private void submitRequest() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvBatteryMessage.setText(
                    "Please log in before requesting assistance."
            );
            return;
        }

        if (spVehicleType.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText(
                    "Please select your vehicle type."
            );
            return;
        }

        if (spBatteryIssue.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText(
                    "Please select the battery issue."
            );
            return;
        }

        if (spFuelType.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText(
                    "Please select a fuel type."
            );
            return;
        }

        if (spFuelQuantity.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText(
                    "Please select the fuel quantity."
            );
            return;
        }

        String location = etBatteryLocation.getText()
                .toString().trim();

        if (location.isEmpty()) {
            etBatteryLocation.setError("Location is required");
            etBatteryLocation.requestFocus();
            return;
        }

        String description = etBatteryDescription.getText()
                .toString().trim();

        String fuelType = spFuelType.getSelectedItem().toString();
        String fuelQuantity =
                spFuelQuantity.getSelectedItem().toString();

        String quantityUnit;

        if ("CNG".equals(fuelType)) {
            quantityUnit = "kg";
        } else if ("EV Charging".equals(fuelType)) {
            quantityUnit = "kWh";
        } else {
            quantityUnit = "litres";
        }

        Map<String, Object> request = new HashMap<>();

        request.put("userId", user.getUid());
        request.put(
                "userEmail",
                user.getEmail() == null ? "" : user.getEmail()
        );

        request.put("serviceType", "Battery Help");

        request.put(
                "vehicleType",
                spVehicleType.getSelectedItem().toString()
        );

        request.put(
                "batteryIssue",
                spBatteryIssue.getSelectedItem().toString()
        );

        // New fuel selection fields
        request.put("fuelType", fuelType);
        request.put("fuelQuantity", fuelQuantity);
        request.put("fuelQuantityUnit", quantityUnit);

        request.put("location", location);
        request.put("description", description);

        request.put("status", "pending");
        request.put("technicianAssigned", false);

        request.put("technicianName", "");
        request.put("technicianPhone", "");
        request.put("technicianRating", "");
        request.put("technicianType", "");

        request.put("createdAt", FieldValue.serverTimestamp());

        btnSubmitBatteryRequest.setEnabled(false);
        btnSubmitBatteryRequest.setText("Submitting...");
        tvBatteryMessage.setText("");

        db.collection("serviceRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {

                    tvBatteryMessage.setText(
                            "Request submitted successfully!\n"
                                    + "Request ID: "
                                    + documentReference.getId()
                                    + "\nFuel: " + fuelType
                                    + "\nQuantity: " + fuelQuantity
                                    + " " + quantityUnit
                                    + "\nStatus: pending"
                    );

                    Toast.makeText(
                            BatteryHelpActivity.this,
                            "Request saved successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSubmitBatteryRequest.setEnabled(true);
                    btnSubmitBatteryRequest.setText("Submit Request");

                    tvBatteryMessage.setText(
                            "Request could not be saved: "
                                    + e.getMessage()
                    );
                });
    }
}

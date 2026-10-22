package com.archi.roadsideassistance;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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
import java.util.Locale;
import java.util.Map;

public class BatteryHelpActivity extends AppCompatActivity {

    private Spinner spVehicleType, spBatteryIssue;
    private Spinner spFuelType, spFuelQuantity;

    private EditText etBatteryLocation, etBatteryDescription;

    private TextView tvBatteryMessage;
    private TextView tvFuelQuantityLabel;
    private TextView tvEstimatedPrice;
    private TextView tvPriceNote;

    private Button btnSubmitBatteryRequest;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Example rates only. Update these with your actual rates.
    private static final double PETROL_RATE = 95.0;
    private static final double DIESEL_RATE = 91.0;
    private static final double CNG_RATE = 80.0;
    private static final double EV_RATE = 15.0;

    private double estimatedPrice = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Spinners
        spVehicleType = findViewById(R.id.spVehicleType);
        spBatteryIssue = findViewById(R.id.spBatteryIssue);
        spFuelType = findViewById(R.id.spFuelType);
        spFuelQuantity = findViewById(R.id.spFuelQuantity);

        // Input fields
        etBatteryLocation = findViewById(R.id.etBatteryLocation);
        etBatteryDescription = findViewById(R.id.etBatteryDescription);

        // TextViews
        tvBatteryMessage = findViewById(R.id.tvBatteryMessage);
        tvFuelQuantityLabel = findViewById(R.id.tvFuelQuantityLabel);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        tvPriceNote = findViewById(R.id.tvPriceNote);

        // Button
        btnSubmitBatteryRequest =
                findViewById(R.id.btnSubmitBatteryRequest);

        setupSpinner(
                spVehicleType,
                new String[]{
                        "Select vehicle type",
                        "Bike",
                        "Car",
                        "SUV",
                        "Truck",
                        "Other"
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
                        "2",
                        "5",
                        "10",
                        "15"
                }
        );

        // Update unit and price when fuel type changes
        spFuelType.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        updateQuantityUnit();
                        updateEstimatedPrice();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                        updateEstimatedPrice();
                    }
                }
        );

        // Update price when quantity changes
        spFuelQuantity.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        updateEstimatedPrice();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                        updateEstimatedPrice();
                    }
                }
        );

        btnSubmitBatteryRequest.setOnClickListener(
                v -> submitRequest()
        );

        updateEstimatedPrice();
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

    // Get example rate for selected fuel
    private double getFuelRate(String fuelType) {

        switch (fuelType) {
            case "Petrol":
                return PETROL_RATE;

            case "Diesel":
                return DIESEL_RATE;

            case "CNG":
                return CNG_RATE;

            case "EV Charging":
                return EV_RATE;

            default:
                return 0.0;
        }
    }

    // Update the quantity unit dynamically
    private void updateQuantityUnit() {

        if (spFuelType == null || tvFuelQuantityLabel == null) {
            return;
        }

        Object selectedItem = spFuelType.getSelectedItem();

        if (selectedItem == null) {
            return;
        }

        String fuelType = selectedItem.toString();

        if ("CNG".equals(fuelType)) {
            tvFuelQuantityLabel.setText("Fuel Quantity (kg) *");
        } else if ("EV Charging".equals(fuelType)) {
            tvFuelQuantityLabel.setText("Charging Quantity (kWh) *");
        } else {
            tvFuelQuantityLabel.setText("Fuel Quantity (litres) *");
        }
    }

    // Calculate price whenever fuel type or quantity changes
    private void updateEstimatedPrice() {

        if (spFuelType == null || spFuelQuantity == null
                || tvEstimatedPrice == null) {
            return;
        }

        if (spFuelType.getSelectedItemPosition() == 0
                || spFuelQuantity.getSelectedItemPosition() == 0) {

            estimatedPrice = 0.0;

            tvEstimatedPrice.setText(
                    "Select fuel type and quantity"
            );

            return;
        }

        String fuelType =
                spFuelType.getSelectedItem().toString();

        String quantityText =
                spFuelQuantity.getSelectedItem().toString();

        try {
            double quantity = Double.parseDouble(quantityText);
            double rate = getFuelRate(fuelType);

            estimatedPrice = quantity * rate;

            tvEstimatedPrice.setText(
                    String.format(
                            Locale.getDefault(),
                            "₹%.2f",
                            estimatedPrice
                    )
            );

            if (tvPriceNote != null) {
                String unit;

                if ("CNG".equals(fuelType)) {
                    unit = "kg";
                } else if ("EV Charging".equals(fuelType)) {
                    unit = "kWh";
                } else {
                    unit = "litre";
                }

                tvPriceNote.setText(
                        String.format(
                                Locale.getDefault(),
                                "Rate: ₹%.2f per %s. Illustrative estimate only.",
                                rate,
                                unit
                        )
                );
            }

        } catch (NumberFormatException e) {

            estimatedPrice = 0.0;

            tvEstimatedPrice.setText(
                    "Unable to calculate price"
            );
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

        String fuelType =
                spFuelType.getSelectedItem().toString();

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

        // Ensure the displayed estimate is current
        updateEstimatedPrice();

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

        // Fuel information
        request.put("fuelType", fuelType);
        request.put("fuelQuantity", fuelQuantity);
        request.put("fuelQuantityUnit", quantityUnit);

        // Estimated price saved as a numeric value
        request.put("estimatedPrice", estimatedPrice);

        request.put("location", location);
        request.put("description", description);

        // Existing request status and technician fields
        request.put("status", "pending");
        request.put("technicianAssigned", false);
        request.put("technicianName", "");
        request.put("technicianPhone", "");
        request.put("technicianRating", "");
        request.put("technicianType", "");

        // Firebase server timestamp
        request.put("createdAt", FieldValue.serverTimestamp());

        btnSubmitBatteryRequest.setEnabled(false);
        btnSubmitBatteryRequest.setText("Submitting...");
        tvBatteryMessage.setText("");

        db.collection("serviceRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            BatteryHelpActivity.this,
                            "Request saved successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    tvBatteryMessage.setText(
                            "Request submitted successfully!\n"
                                    + "Request ID: "
                                    + documentReference.getId()
                                    + "\nFuel: " + fuelType
                                    + "\nQuantity: " + fuelQuantity
                                    + " " + quantityUnit
                                    + String.format(
                                    Locale.getDefault(),
                                    "\nEstimated Price: ₹%.2f",
                                    estimatedPrice
                            )
                                    + "\nStatus: pending"
                    );

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

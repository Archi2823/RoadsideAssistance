
package com.archi.roadsideassistance;

import android.os.Bundle;
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
    private EditText etBatteryLocation, etBatteryDescription;
    private TextView tvBatteryMessage;
    private Button btnSubmitBatteryRequest;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize form fields
        spVehicleType = findViewById(R.id.spVehicleType);
        spBatteryIssue = findViewById(R.id.spBatteryIssue);
        etBatteryLocation = findViewById(R.id.etBatteryLocation);
        etBatteryDescription =
                findViewById(R.id.etBatteryDescription);
        tvBatteryMessage = findViewById(R.id.tvBatteryMessage);
        btnSubmitBatteryRequest =
                findViewById(R.id.btnSubmitBatteryRequest);

        // Vehicle options
        String[] vehicles = {
                "Select vehicle type",
                "Bike",
                "Car",
                "SUV",
                "Truck",
                "Other"
        };

        // Battery issue options
        String[] issues = {
                "Select battery issue",
                "Battery dead",
                "Vehicle not starting",
                "Jump-start required",
                "Battery replacement required",
                "Other"
        };

        // Set vehicle spinner adapter
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicles
        );

        vehicleAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spVehicleType.setAdapter(vehicleAdapter);

        // Set battery issue spinner adapter
        ArrayAdapter<String> issueAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                issues
        );

        issueAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spBatteryIssue.setAdapter(issueAdapter);

        // Submit request
        btnSubmitBatteryRequest.setOnClickListener(
                v -> submitRequest()
        );
    }

    private void submitRequest() {

        FirebaseUser user = auth.getCurrentUser();

        // Check login
        if (user == null) {
            tvBatteryMessage.setText(
                    "Please log in before requesting battery help."
            );
            return;
        }

        int vehiclePosition =
                spVehicleType.getSelectedItemPosition();

        int issuePosition =
                spBatteryIssue.getSelectedItemPosition();

        String vehicleType =
                spVehicleType.getSelectedItem().toString();

        String batteryIssue =
                spBatteryIssue.getSelectedItem().toString();

        String location = etBatteryLocation.getText()
                .toString().trim();

        String description = etBatteryDescription.getText()
                .toString().trim();

        // Validate vehicle type
        if (vehiclePosition == 0) {
            tvBatteryMessage.setText(
                    "Please select your vehicle type."
            );
            return;
        }

        // Validate battery issue
        if (issuePosition == 0) {
            tvBatteryMessage.setText(
                    "Please select the battery issue."
            );
            return;
        }

        // Validate location
        if (location.isEmpty()) {
            etBatteryLocation.setError(
                    "Please enter your location"
            );
            etBatteryLocation.requestFocus();
            return;
        }

        // Prepare request data
        Map<String, Object> request = new HashMap<>();

        request.put("userId", user.getUid());

        request.put(
                "userEmail",
                user.getEmail() == null ? "" : user.getEmail()
        );

        request.put("serviceType", "Battery Help");
        request.put("vehicleType", vehicleType);
        request.put("batteryIssue", batteryIssue);
        request.put("location", location);
        request.put("description", description);

        // Initial request status
        request.put("status", "pending");

        // Firebase server timestamp
        request.put("createdAt", FieldValue.serverTimestamp());

        // Prevent repeated submissions
        btnSubmitBatteryRequest.setEnabled(false);
        btnSubmitBatteryRequest.setText("Submitting...");
        tvBatteryMessage.setText("");

        // Save request in Firestore
        db.collection("serviceRequests")
                .add(request)

                .addOnSuccessListener(documentReference -> {

                    // Request successfully saved
                    String requestId =
                            documentReference.getId();

                    Toast.makeText(
                            BatteryHelpActivity.this,
                            "Battery help request submitted successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    tvBatteryMessage.setText(
                            "Request submitted successfully.\n"
                                    + "Request ID: " + requestId
                                    + "\nStatus: pending"
                    );

                    // Return to previous screen after success
                    finish();
                })

                .addOnFailureListener(e -> {

                    // Allow retry if saving fails
                    btnSubmitBatteryRequest.setEnabled(true);
                    btnSubmitBatteryRequest.setText(
                            "Submit Request"
                    );

                    tvBatteryMessage.setText(
                            "Request could not be saved: "
                                    + e.getMessage()
                    );
                });
    }
}

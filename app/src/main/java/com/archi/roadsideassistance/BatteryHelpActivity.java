
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BatteryHelpActivity extends AppCompatActivity {

    private Spinner spVehicleType, spBatteryIssue;
    private EditText etBatteryLocation, etBatteryDescription;
    private TextView tvBatteryMessage;
    private Button btnSubmitBatteryRequest;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Predefined dummy technicians
    private final String[][] technicians = {
            {"Rahul Sharma", "9876543210", "4.8"},
            {"Amit Patel", "9876501234", "4.7"},
            {"Vijay Kumar", "9876512345", "4.9"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spVehicleType = findViewById(R.id.spVehicleType);
        spBatteryIssue = findViewById(R.id.spBatteryIssue);
        etBatteryLocation = findViewById(R.id.etBatteryLocation);
        etBatteryDescription =
                findViewById(R.id.etBatteryDescription);
        tvBatteryMessage = findViewById(R.id.tvBatteryMessage);
        btnSubmitBatteryRequest =
                findViewById(R.id.btnSubmitBatteryRequest);

        String[] vehicles = {
                "Select vehicle type",
                "Bike", "Car", "SUV", "Truck", "Other"
        };

        String[] issues = {
                "Select battery issue",
                "Battery dead",
                "Vehicle not starting",
                "Jump-start required",
                "Battery replacement required",
                "Other"
        };

        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicles
        );
        vehicleAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spVehicleType.setAdapter(vehicleAdapter);

        ArrayAdapter<String> issueAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                issues
        );
        issueAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spBatteryIssue.setAdapter(issueAdapter);

        btnSubmitBatteryRequest.setOnClickListener(
                v -> submitRequest()
        );
    }

    private void submitRequest() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvBatteryMessage.setText(
                    "Please log in before requesting battery help."
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

        String location = etBatteryLocation.getText()
                .toString().trim();

        if (location.isEmpty()) {
            etBatteryLocation.setError("Location is required");
            etBatteryLocation.requestFocus();
            return;
        }

        String description = etBatteryDescription.getText()
                .toString().trim();

        // Select a predefined dummy technician
        int index = new Random().nextInt(technicians.length);

        String technicianName = technicians[index][0];
        String technicianPhone = technicians[index][1];
        String technicianRating = technicians[index][2];

        // Prepare request document
        Map<String, Object> request = new HashMap<>();

        request.put("userId", user.getUid());
        request.put("userEmail",
                user.getEmail() == null ? "" : user.getEmail());

        request.put("serviceType", "Battery Help");
        request.put("vehicleType",
                spVehicleType.getSelectedItem().toString());
        request.put("batteryIssue",
                spBatteryIssue.getSelectedItem().toString());

        request.put("location", location);
        request.put("description", description);

        // Request status
        request.put("status", "pending");

        // Assigned dummy technician
        request.put("technicianName", technicianName);
        request.put("technicianPhone", technicianPhone);
        request.put("technicianRating", technicianRating);
        request.put("technicianType", "Dummy Technician");

        request.put("createdAt", FieldValue.serverTimestamp());

        btnSubmitBatteryRequest.setEnabled(false);
        btnSubmitBatteryRequest.setText("Submitting...");
        tvBatteryMessage.setText("");

        db.collection("serviceRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {

                    String requestId = documentReference.getId();

                    // Show assigned technician after successful save
                    showTechnicianDetails(
                            requestId,
                            technicianName,
                            technicianPhone,
                            technicianRating
                    );
                })
                .addOnFailureListener(e -> {

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

    private void showTechnicianDetails(
            String requestId,
            String name,
            String phone,
            String rating) {

        new AlertDialog.Builder(this)
                .setTitle("Technician Assigned!")
                .setMessage(
                        "Your battery help request has been saved.\n\n"
                                + "Request ID: " + requestId + "\n\n"
                                + "Technician: " + name + "\n"
                                + "Contact: " + phone + "\n"
                                + "Rating: " + rating + " / 5\n\n"
                                + "Request Status: pending"
                )
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }
}

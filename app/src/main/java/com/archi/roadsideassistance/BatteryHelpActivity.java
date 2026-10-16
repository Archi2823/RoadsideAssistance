
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
                "Bike",
                "Car",
                "SUV",
                "Truck",
                "Other"
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

        request.put("location", location);
        request.put("description", description);

        // New request is pending and has no assigned technician yet.
        request.put("status", "pending");
        request.put("technicianAssigned", false);

        // Empty technician fields until assignment happens.
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

                    String requestId = documentReference.getId();

                    tvBatteryMessage.setText(
                            "Request submitted successfully!\n"
                                    + "Request ID: " + requestId
                                    + "\nStatus: pending"
                                    + "\nYou can edit or cancel it "
                                    + "before a technician is assigned."
                    );

                    Toast.makeText(
                            BatteryHelpActivity.this,
                            "Battery help request saved!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
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
}

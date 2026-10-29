
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TowingHelpActivity extends AppCompatActivity {

    private Spinner spTowingVehicleType, spTowingIssue;
    private EditText etPickupLocation;
    private EditText etDestination, etTowingDetails;
    private Button btnSubmitTowingRequest;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towing_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spTowingVehicleType = findViewById(R.id.spTowingVehicleType);
        spTowingIssue = findViewById(R.id.spTowingIssue);

        etPickupLocation = findViewById(R.id.etPickupLocation);
        etDestination = findViewById(R.id.etTowingDestination);
        etTowingDetails = findViewById(R.id.etTowingDetails);

        btnSubmitTowingRequest =
                findViewById(R.id.btnSubmitTowingRequest);

        setupSpinners();

        spTowingVehicleType.setSelection(1);
        spTowingIssue.setSelection(0);

        btnSubmitTowingRequest.setOnClickListener(
                v -> submitTowingRequest()
        );
    }

    private void setupSpinners() {

        String[] vehicleTypes = {
                "Bike",
                "Car",
                "SUV",
                "Truck",
                "Other"
        };

        String[] towingIssues = {
                "Vehicle breakdown",
                "Accident",
                "Engine failure",
                "Flat tyre / wheel issue",
                "Vehicle won't start",
                "Other"
        };

        ArrayAdapter<String> vehicleAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        vehicleTypes
                );

        vehicleAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spTowingVehicleType.setAdapter(vehicleAdapter);

        ArrayAdapter<String> issueAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        towingIssues
                );

        issueAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spTowingIssue.setAdapter(issueAdapter);
    }

    private void submitTowingRequest() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in before requesting towing help.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String vehicleType =
                spTowingVehicleType.getSelectedItem().toString();

        String towingIssue =
                spTowingIssue.getSelectedItem().toString();

        String pickupLocation =
                etPickupLocation.getText().toString().trim();

        String destination =
                etDestination.getText().toString().trim();

        String details =
                etTowingDetails.getText().toString().trim();

        if (TextUtils.isEmpty(pickupLocation)) {
            etPickupLocation.setError(
                    "Please enter the pickup location"
            );
            etPickupLocation.requestFocus();
            return;
        }

        if (pickupLocation.length() < 5) {
            etPickupLocation.setError(
                    "Enter a more specific pickup location"
            );
            etPickupLocation.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(destination)
                && destination.length() < 3) {
            etDestination.setError(
                    "Enter a valid destination or leave it blank"
            );
            etDestination.requestFocus();
            return;
        }

        Map<String, Object> request = new HashMap<>();

        request.put("userId", user.getUid());
        request.put(
                "userEmail",
                user.getEmail() == null ? "" : user.getEmail()
        );

        request.put("serviceType", "Towing Help");
        request.put("vehicleType", vehicleType);
        request.put("towingIssue", towingIssue);
        request.put("location", pickupLocation);
        request.put("destination", destination);
        request.put("description", details);

        // Initial request status
        request.put("status", "pending");

        // Assignment fields before simulated assignment
        request.put("technicianAssigned", false);
        request.put("technicianName", "");
        request.put("technicianPhone", "");
        request.put("technicianRating", "");
        request.put("technicianType", "");

        request.put("createdAt", FieldValue.serverTimestamp());

        btnSubmitTowingRequest.setEnabled(false);
        btnSubmitTowingRequest.setText("Submitting...");

        db.collection("serviceRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {

                    // Request is saved. Now simulate assignment.
                    simulateAssignment(documentReference.getId());

                })
                .addOnFailureListener(e -> {

                    btnSubmitTowingRequest.setEnabled(true);
                    btnSubmitTowingRequest.setText(
                            "Submit Towing Request"
                    );

                    Toast.makeText(
                            TowingHelpActivity.this,
                            "Request failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void simulateAssignment(String requestId) {

        Map<String, Object> assignment = new HashMap<>();

        // Demo provider, not a verified real business
        assignment.put(
                "technicianName",
                "Towing Point 1 (Demo)"
        );

        assignment.put("technicianPhone", "");
        assignment.put("technicianRating", "");
        assignment.put("technicianType", "Towing Service");

        assignment.put("technicianAssigned", true);
        assignment.put("status", "assigned");
        assignment.put("assignedAt", FieldValue.serverTimestamp());

        db.collection("serviceRequests")
                .document(requestId)
                .update(assignment)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            TowingHelpActivity.this,
                            "Towing request saved and demo service assigned!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSubmitTowingRequest.setEnabled(true);
                    btnSubmitTowingRequest.setText(
                            "Submit Towing Request"
                    );

                    Toast.makeText(
                            TowingHelpActivity.this,
                            "Request was saved, but assignment failed: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}

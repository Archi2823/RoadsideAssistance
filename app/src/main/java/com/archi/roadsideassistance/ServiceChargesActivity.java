
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ServiceChargesActivity extends AppCompatActivity {

    private Spinner spChargesVehicleType;

    private EditText etGeneralServiceCharge;
    private EditText etAnnualServiceCharge;
    private EditText etInspectionCharge;
    private EditText etChargesDescription;

    private TextView tvChargesMessage;

    private Button btnSaveServiceCharges;
    private Button btnChargesBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String workerUid;

    private boolean loadingCharges = false;
    private boolean savingCharges = false;
    private boolean workerVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_charges);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        spChargesVehicleType = findViewById(R.id.spChargesVehicleType);

        etGeneralServiceCharge = findViewById(R.id.etGeneralServiceCharge);
        etAnnualServiceCharge = findViewById(R.id.etAnnualServiceCharge);
        etInspectionCharge = findViewById(R.id.etInspectionCharge);
        etChargesDescription = findViewById(R.id.etChargesDescription);

        tvChargesMessage = findViewById(R.id.tvChargesMessage);

        btnSaveServiceCharges = findViewById(R.id.btnSaveServiceCharges);
        btnChargesBack = findViewById(R.id.btnChargesBack);

        // Initially disable form until worker is verified
        spChargesVehicleType.setEnabled(false);
        btnSaveServiceCharges.setEnabled(false);

        setupVehicleSpinner();

        btnChargesBack.setOnClickListener(v -> finish());

        btnSaveServiceCharges.setOnClickListener(v -> saveServiceCharges());

        verifyWorker();
    }

    // Verify logged-in worker and approval status
    private void verifyWorker() {

        if (auth.getCurrentUser() == null) {
            tvChargesMessage.setText("Please login first.");
            btnSaveServiceCharges.setEnabled(false);
            spChargesVehicleType.setEnabled(false);
            return;
        }

        workerUid = auth.getCurrentUser().getUid();

        tvChargesMessage.setText("Verifying worker account...");

        db.collection("users")
                .document(workerUid)
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        tvChargesMessage.setText("Worker profile not found.");
                        return;
                    }

                    String role = document.getString("role");
                    String approvalStatus = document.getString("approvalStatus");

                    if (role == null || !role.equalsIgnoreCase("worker")) {
                        tvChargesMessage.setText(
                                "Only service providers can manage charges."
                        );
                        return;
                    }

                    if (approvalStatus == null
                            || !approvalStatus.equalsIgnoreCase("approved")) {

                        tvChargesMessage.setText(
                                "Your account is not approved yet."
                        );
                        return;
                    }

                    workerVerified = true;

                    spChargesVehicleType.setEnabled(true);
                    btnSaveServiceCharges.setEnabled(true);

                    tvChargesMessage.setText(
                            "Select vehicle type to manage your service charges."
                    );

                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    tvChargesMessage.setText(
                            "Verification failed: " + e.getLocalizedMessage()
                    );
                });
    }

    // Vehicle type dropdown
    private void setupVehicleSpinner() {

        String[] vehicleTypes = {
                "Select vehicle type",
                "Bike",
                "Car",
                "SUV",
                "Truck",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spChargesVehicleType.setAdapter(adapter);

        spChargesVehicleType.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        if (position == 0) {
                            clearFields();

                            if (workerVerified) {
                                tvChargesMessage.setText(
                                        "Select a vehicle type."
                                );
                            }

                            return;
                        }

                        if (!workerVerified || workerUid == null) {
                            return;
                        }

                        String vehicleType =
                                spChargesVehicleType.getSelectedItem().toString();

                        loadServiceCharges(vehicleType);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    // Load charges belonging to the logged-in worker
    private void loadServiceCharges(String vehicleType) {

        if (!workerVerified || workerUid == null) {
            return;
        }

        loadingCharges = true;

        clearFields();

        tvChargesMessage.setText("Loading saved charges...");

        db.collection("serviceCharges")
                .document(workerUid)
                .collection("vehicleCharges")
                .document(vehicleType)
                .get()
                .addOnSuccessListener(document -> {

                    loadingCharges = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (document.exists()) {

                        etGeneralServiceCharge.setText(
                                formatCharge(
                                        document.get("generalServiceCharge")
                                )
                        );

                        etAnnualServiceCharge.setText(
                                formatCharge(
                                        document.get("annualServiceCharge")
                                )
                        );

                        etInspectionCharge.setText(
                                formatCharge(
                                        document.get("inspectionCharge")
                                )
                        );

                        String description =
                                document.getString("description");

                        etChargesDescription.setText(
                                description == null ? "" : description
                        );

                        tvChargesMessage.setText(
                                "Saved charges loaded for " + vehicleType
                        );

                    } else {

                        tvChargesMessage.setText(
                                "No charges saved for " + vehicleType
                                        + ". Enter new charges."
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    loadingCharges = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    tvChargesMessage.setText(
                            "Failed to load charges: "
                                    + e.getLocalizedMessage()
                    );
                });
    }

    // Convert Firestore charge value safely into text
    private String formatCharge(Object charge) {

        if (charge == null) {
            return "";
        }

        return String.valueOf(charge);
    }

    // Save or update worker's charges
    private void saveServiceCharges() {

        if (!workerVerified || workerUid == null) {
            tvChargesMessage.setText(
                    "Worker verification is required."
            );
            return;
        }

        if (loadingCharges || savingCharges) {
            return;
        }

        if (spChargesVehicleType.getSelectedItemPosition() == 0) {

            tvChargesMessage.setText(
                    "Please select a vehicle type first."
            );

            return;
        }

        String vehicleType =
                spChargesVehicleType.getSelectedItem().toString();

        String generalText =
                etGeneralServiceCharge.getText().toString().trim();

        String annualText =
                etAnnualServiceCharge.getText().toString().trim();

        String inspectionText =
                etInspectionCharge.getText().toString().trim();

        // Validate General Service charge
        if (generalText.isEmpty()) {
            etGeneralServiceCharge.setError(
                    "Enter General Service charge"
            );
            etGeneralServiceCharge.requestFocus();
            return;
        }

        // Validate Annual Service charge
        if (annualText.isEmpty()) {
            etAnnualServiceCharge.setError(
                    "Enter Annual Service charge"
            );
            etAnnualServiceCharge.requestFocus();
            return;
        }

        // Validate Inspection charge
        if (inspectionText.isEmpty()) {
            etInspectionCharge.setError(
                    "Enter Inspection charge"
            );
            etInspectionCharge.requestFocus();
            return;
        }

        double generalCharge;
        double annualCharge;
        double inspectionCharge;

        try {

            generalCharge = Double.parseDouble(generalText);
            annualCharge = Double.parseDouble(annualText);
            inspectionCharge = Double.parseDouble(inspectionText);

            if (!Double.isFinite(generalCharge)
                    || !Double.isFinite(annualCharge)
                    || !Double.isFinite(inspectionCharge)
                    || generalCharge < 0
                    || annualCharge < 0
                    || inspectionCharge < 0) {

                tvChargesMessage.setText(
                        "Charges must be valid non-negative amounts."
                );

                return;
            }

        } catch (NumberFormatException e) {

            tvChargesMessage.setText(
                    "Please enter valid numeric charges."
            );

            return;
        }

        String description =
                etChargesDescription.getText().toString().trim();

        // Prepare Firestore data
        Map<String, Object> chargeData = new HashMap<>();

        chargeData.put("workerId", workerUid);
        chargeData.put("vehicleType", vehicleType);

        chargeData.put("generalServiceCharge", generalCharge);
        chargeData.put("annualServiceCharge", annualCharge);
        chargeData.put("inspectionCharge", inspectionCharge);

        chargeData.put("description", description);

        chargeData.put("updatedAt", FieldValue.serverTimestamp());

        // Worker-specific Firebase path
        savingCharges = true;

        btnSaveServiceCharges.setEnabled(false);
        btnSaveServiceCharges.setText("Saving Charges...");

        tvChargesMessage.setText("Saving your charges...");

        db.collection("serviceCharges")
                .document(workerUid)
                .collection("vehicleCharges")
                .document(vehicleType)
                .set(chargeData)
                .addOnSuccessListener(unused -> {

                    savingCharges = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnSaveServiceCharges.setEnabled(true);
                    btnSaveServiceCharges.setText("Save Service Charges");

                    tvChargesMessage.setText(
                            "Service charges saved successfully for "
                                    + vehicleType + "."
                    );

                    Toast.makeText(
                            ServiceChargesActivity.this,
                            "Service charges saved successfully!",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    savingCharges = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    btnSaveServiceCharges.setEnabled(true);
                    btnSaveServiceCharges.setText("Save Service Charges");

                    tvChargesMessage.setText(
                            "Failed to save charges: "
                                    + e.getLocalizedMessage()
                    );
                });
    }

    // Clear input fields
    private void clearFields() {

        etGeneralServiceCharge.setText("");
        etAnnualServiceCharge.setText("");
        etInspectionCharge.setText("");
        etChargesDescription.setText("");
    }
}

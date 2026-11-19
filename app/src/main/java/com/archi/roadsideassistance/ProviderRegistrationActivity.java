
package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRegistrationActivity extends AppCompatActivity {

    private EditText etFullName, etPhone, etEmail;
    private EditText etGarage, etExperience, etSpecialization;
    private EditText etCity, etPincode, etServiceArea;
    private EditText etAvailableDays, etWorkingHours;

    private CheckBox cbBikeRepair, cbCarRepair, cbBattery;
    private CheckBox cbTowing, cbMaintenance, cbEmergency;

    private Button btnSubmit;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_registration);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initializeViews() {

        etFullName = findViewById(R.id.etProviderFullName);
        etPhone = findViewById(R.id.etProviderPhone);
        etEmail = findViewById(R.id.etProviderEmail);

        etGarage = findViewById(R.id.etProviderGarage);
        etExperience = findViewById(R.id.etProviderExperience);
        etSpecialization = findViewById(R.id.etProviderSpecialization);

        cbBikeRepair = findViewById(R.id.cbBikeRepair);
        cbCarRepair = findViewById(R.id.cbCarRepair);
        cbBattery = findViewById(R.id.cbBattery);
        cbTowing = findViewById(R.id.cbTowing);
        cbMaintenance = findViewById(R.id.cbMaintenance);
        cbEmergency = findViewById(R.id.cbEmergency);

        etCity = findViewById(R.id.etProviderCity);
        etPincode = findViewById(R.id.etProviderPincode);
        etServiceArea = findViewById(R.id.etProviderServiceArea);

        etAvailableDays = findViewById(R.id.etProviderAvailableDays);
        etWorkingHours = findViewById(R.id.etProviderWorkingHours);

        btnSubmit = findViewById(R.id.btnSubmitProviderRegistration);
    }

    private String getText(EditText editText) {
        return editText.getText().toString().trim();
    }

    private void submitRegistration() {

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please login before registering as a service provider",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String fullName = getText(etFullName);
        String phone = getText(etPhone);
        String email = getText(etEmail);

        String garage = getText(etGarage);
        String experienceText = getText(etExperience);
        String specialization = getText(etSpecialization);

        String city = getText(etCity);
        String pincode = getText(etPincode);
        String serviceArea = getText(etServiceArea);

        String availableDays = getText(etAvailableDays);
        String workingHours = getText(etWorkingHours);

        // Validate required fields

        if (fullName.isEmpty()) {
            etFullName.setError("Enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (phone.length() != 10) {
            etPhone.setError("Enter a valid 10-digit mobile number");
            etPhone.requestFocus();
            return;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (experienceText.isEmpty()) {
            etExperience.setError("Enter your experience");
            etExperience.requestFocus();
            return;
        }

        int experience;

        try {
            experience = Integer.parseInt(experienceText);
            if (experience < 0 || experience > 80) {
                etExperience.setError("Enter valid experience years");
                etExperience.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etExperience.setError("Enter experience in numbers");
            etExperience.requestFocus();
            return;
        }

        if (specialization.isEmpty()) {
            etSpecialization.setError("Enter your specialization");
            etSpecialization.requestFocus();
            return;
        }

        if (city.isEmpty()) {
            etCity.setError("Enter your city");
            etCity.requestFocus();
            return;
        }

        if (pincode.length() != 6) {
            etPincode.setError("Enter a valid 6-digit pincode");
            etPincode.requestFocus();
            return;
        }

        if (availableDays.isEmpty()) {
            etAvailableDays.setError("Enter your available days");
            etAvailableDays.requestFocus();
            return;
        }

        if (workingHours.isEmpty()) {
            etWorkingHours.setError("Enter your working hours");
            etWorkingHours.requestFocus();
            return;
        }

        // Collect selected services

        List<String> services = new ArrayList<>();

        if (cbBikeRepair.isChecked())
            services.add("Bike Repair");

        if (cbCarRepair.isChecked())
            services.add("Car Repair");

        if (cbBattery.isChecked())
            services.add("Battery Assistance / Jump Start");

        if (cbTowing.isChecked())
            services.add("Towing Assistance");

        if (cbMaintenance.isChecked())
            services.add("General Maintenance");

        if (cbEmergency.isChecked())
            services.add("Emergency Roadside Repair");

        if (services.isEmpty()) {
            Toast.makeText(this,
                    "Please select at least one service",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple submissions

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        // Prepare Firestore data

        Map<String, Object> application = new HashMap<>();

        application.put("applicantId", currentUser.getUid());
        application.put("fullName", fullName);
        application.put("phone", phone);
        application.put("email", email);

        application.put("garageName", garage);
        application.put("experience", experience);
        application.put("specialization", specialization);

        application.put("services", services);

        application.put("city", city);
        application.put("pincode", pincode);
        application.put("serviceArea", serviceArea);

        application.put("availableDays", availableDays);
        application.put("workingHours", workingHours);

        application.put("status", "pending");
        application.put("createdAt", FieldValue.serverTimestamp());

        application.put("reviewed", false);
        application.put("approved", false);

        // Save application

        db.collection("workerApplications")
                .add(application)
                .addOnSuccessListener(documentReference -> {

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Application");

                    new AlertDialog.Builder(this)
                            .setTitle("Application Submitted")
                            .setMessage("Your service provider application has been submitted successfully. Our admin team will review it.")
                            .setCancelable(false)
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {

                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Application");

                    Toast.makeText(this,
                            "Failed to submit application: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}

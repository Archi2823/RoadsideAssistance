
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etProfileName;
    private EditText etProfilePhone;
    private EditText etProfileEmail;

    private EditText etProfileGarageName;
    private EditText etProfileCategory;
    private EditText etProfileSpecialization;
    private EditText etProfileExperience;
    private EditText etProfileServices;

    private EditText etProfileCity;
    private EditText etProfilePincode;
    private EditText etProfileServiceArea;
    private EditText etProfileAvailableDays;
    private EditText etProfileWorkingHours;

    private TextView tvApprovalStatus;
    private TextView tvWorkerId;

    private Button btnSaveProfile;
    private Button btnBackProfile;

    private String workerUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();

        btnBackProfile.setOnClickListener(v -> finish());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadWorkerProfile();
    }

    // --------------------------------------------------
    // INITIALIZE VIEWS
    // --------------------------------------------------

    private void initializeViews() {

        etProfileName = findViewById(R.id.etProfileName);
        etProfilePhone = findViewById(R.id.etProfilePhone);
        etProfileEmail = findViewById(R.id.etProfileEmail);

        etProfileGarageName =
            findViewById(R.id.etProfileGarageName);

        etProfileCategory =
            findViewById(R.id.etProfileCategory);

        etProfileSpecialization =
            findViewById(R.id.etProfileSpecialization);

        etProfileExperience =
            findViewById(R.id.etProfileExperience);

        etProfileServices =
            findViewById(R.id.etProfileServices);

        etProfileCity =
            findViewById(R.id.etProfileCity);

        etProfilePincode =
            findViewById(R.id.etProfilePincode);

        etProfileServiceArea =
            findViewById(R.id.etProfileServiceArea);

        etProfileAvailableDays =
            findViewById(R.id.etProfileAvailableDays);

        etProfileWorkingHours =
            findViewById(R.id.etProfileWorkingHours);

        tvApprovalStatus =
            findViewById(R.id.tvApprovalStatus);

        tvWorkerId =
            findViewById(R.id.tvWorkerId);

        btnSaveProfile =
            findViewById(R.id.btnSaveProfile);

        btnBackProfile =
            findViewById(R.id.btnBackProfile);
    }

    // --------------------------------------------------
    // LOAD WORKER PROFILE
    // --------------------------------------------------

    private void loadWorkerProfile() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        workerUid = user.getUid();

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Loading Profile...");

        db.collection("users")
            .document(workerUid)
            .get()
            .addOnSuccessListener(document -> {

        if (!document.exists()) {
            showToast("Worker profile not found.");
            goToLogin();
            return;
        }

        String role = document.getString("role");

        if (!"worker".equalsIgnoreCase(role)) {
            showToast("Access denied. Worker account required.");
            goToLogin();
            return;
        }

        populateProfile(document);

        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Save Profile Changes");
    })
        .addOnFailureListener(e -> {

        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Retry / Save Profile");

        showToast(
            "Unable to load profile: " +
                    e.getMessage()
        );
    });
    }

    // --------------------------------------------------
    // POPULATE PROFILE FIELDS
    // --------------------------------------------------

    private void populateProfile(DocumentSnapshot document) {

        etProfileName.setText(
            getStringValue(document, "name")
        );

        etProfilePhone.setText(
            getStringValue(document, "phone")
        );

        etProfileEmail.setText(
            getStringValue(document, "email")
        );

        etProfileGarageName.setText(
            getStringValue(document, "garageName")
        );

        etProfileCategory.setText(
            getStringValue(document, "serviceCategory")
        );

        etProfileSpecialization.setText(
            getStringValue(document, "specialization")
        );

        etProfileExperience.setText(
            getNumberValue(document, "experience")
        );

        etProfileServices.setText(
            listOrStringValue(document, "services")
        );

        etProfileCity.setText(
            getStringValue(document, "city")
        );

        etProfilePincode.setText(
            getStringValue(document, "pincode")
        );

        etProfileServiceArea.setText(
            getStringValue(document, "serviceArea")
        );

        etProfileAvailableDays.setText(
            listOrStringValue(document, "availableDays")
        );

        etProfileWorkingHours.setText(
            getStringValue(document, "workingHours")
        );

        String approvalStatus =
        getStringValue(document, "approvalStatus");

        tvApprovalStatus.setText(
            "Status: " + approvalStatus.toUpperCase()
        );

        tvWorkerId.setText(
            "Worker ID: " + workerUid
        );
    }

    // --------------------------------------------------
    // SAVE PROFILE
    // --------------------------------------------------

    private void saveProfile() {

        if (workerUid == null || workerUid.isEmpty()) {
            showToast("Worker account not found.");
            return;
        }

        String name = etProfileName.getText()
            .toString().trim();

        String phone = etProfilePhone.getText()
            .toString().trim();

        String garageName = etProfileGarageName.getText()
            .toString().trim();

        String category = etProfileCategory.getText()
            .toString().trim();

        String specialization = etProfileSpecialization.getText()
            .toString().trim();

        String experienceText = etProfileExperience.getText()
            .toString().trim();

        String servicesText = etProfileServices.getText()
            .toString().trim();

        String city = etProfileCity.getText()
            .toString().trim();

        String pincode = etProfilePincode.getText()
            .toString().trim();

        String serviceArea = etProfileServiceArea.getText()
            .toString().trim();

        String availableDaysText =
        etProfileAvailableDays.getText()
            .toString().trim();

        String workingHours = etProfileWorkingHours.getText()
            .toString().trim();

        // NAME VALIDATION
        if (name.length() < 3) {
            etProfileName.setError(
                "Name must contain at least 3 characters"
            );
            etProfileName.requestFocus();
            return;
        }

        // PHONE VALIDATION
        if (!phone.matches("^[6-9][0-9]{9}$")) {
            etProfilePhone.setError(
                "Enter a valid 10-digit Indian mobile number"
            );
            etProfilePhone.requestFocus();
            return;
        }

        // GARAGE NAME VALIDATION
        if (garageName.isEmpty()) {
            etProfileGarageName.setError(
                "Enter garage/business name"
            );
            etProfileGarageName.requestFocus();
            return;
        }

        // CATEGORY VALIDATION
        if (category.isEmpty()) {
            etProfileCategory.setError(
                "Enter your service category"
            );
            etProfileCategory.requestFocus();
            return;
        }

        // EXPERIENCE VALIDATION
        if (experienceText.isEmpty()) {
            etProfileExperience.setError(
                "Enter experience in years"
            );
            etProfileExperience.requestFocus();
            return;
        }

        int experience;

        try {
            experience = Integer.parseInt(experienceText);

            if (experience < 0 || experience > 99) {
                etProfileExperience.setError(
                    "Experience must be between 0 and 99 years"
                );
                etProfileExperience.requestFocus();
                return;
            }

        } catch (NumberFormatException e) {
            etProfileExperience.setError(
                "Enter valid experience"
            );
            etProfileExperience.requestFocus();
            return;
        }

        // SERVICES VALIDATION
        List<String> services = parseCommaSeparated(servicesText);

        if (services.isEmpty()) {
            etProfileServices.setError(
                "Enter at least one service"
            );
            etProfileServices.requestFocus();
            return;
        }

        // CITY VALIDATION
        if (city.isEmpty()) {
            etProfileCity.setError(
                "Enter your city"
            );
            etProfileCity.requestFocus();
            return;
        }

        // PINCODE VALIDATION
        if (!pincode.matches("^[0-9]{6}$")) {
            etProfilePincode.setError(
                "Enter a valid 6-digit pincode"
            );
            etProfilePincode.requestFocus();
            return;
        }

        // SERVICE AREA VALIDATION
        if (serviceArea.isEmpty()) {
            etProfileServiceArea.setError(
                "Enter your service area/address"
            );
            etProfileServiceArea.requestFocus();
            return;
        }

        // AVAILABLE DAYS VALIDATION
        List<String> availableDays =
        parseCommaSeparated(availableDaysText);

        if (availableDays.isEmpty()) {
            etProfileAvailableDays.setError(
                "Enter your available days"
            );
            etProfileAvailableDays.requestFocus();
            return;
        }

        // WORKING HOURS VALIDATION
        if (workingHours.isEmpty()) {
            etProfileWorkingHours.setError(
                "Enter working hours"
            );
            etProfileWorkingHours.requestFocus();
            return;
        }

        // PREPARE PROFILE UPDATES
        Map<String, Object> updates = new HashMap<>();

        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("garageName", garageName);
        updates.put("serviceCategory", category);
        updates.put("specialization", specialization);
        updates.put("experience", experience);
        updates.put("services", services);
        updates.put("city", city);
        updates.put("pincode", pincode);
        updates.put("serviceArea", serviceArea);
        updates.put("availableDays", availableDays);
        updates.put("workingHours", workingHours);
        updates.put("profileUpdatedAt", FieldValue.serverTimestamp());

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        db.collection("users")
            .document(workerUid)
            .update(updates)
            .addOnSuccessListener(unused -> {

        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Save Profile Changes");

        showToast("Profile updated successfully!");

    })
        .addOnFailureListener(e -> {

        btnSaveProfile.setEnabled(true);
        btnSaveProfile.setText("Save Profile Changes");

        showToast(
            "Profile update failed: " +
                    e.getMessage()
        );
    });
    }

    // --------------------------------------------------
    // COMMA-SEPARATED TEXT TO LIST
    // --------------------------------------------------

    private List<String> parseCommaSeparated(String text) {

        List<String> result = new ArrayList<>();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String[] parts = text.split(",");

        for (String part : parts) {

        String value = part.trim();

        if (!value.isEmpty() && !result.contains(value)) {
            result.add(value);
        }
    }

        return result;
    }

    // --------------------------------------------------
    // READ STRING FIELD
    // --------------------------------------------------

    private String getStringValue(
        DocumentSnapshot document,
        String field) {

        Object value = document.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    // --------------------------------------------------
    // READ EXPERIENCE
    // --------------------------------------------------

    private String getNumberValue(
        DocumentSnapshot document,
        String field) {

        Object value = document.get(field);

        if (value == null) {
            return "";
        }

        return String.valueOf(value);
    }

    // --------------------------------------------------
    // READ LIST OR STRING
    // Supports Firestore ArrayList and plain string
    // --------------------------------------------------

    private String listOrStringValue(
        DocumentSnapshot document,
        String field) {

        Object value = document.get(field);

        if (value == null) {
            return "";
        }

        if (value instanceof List<?>) {

            List<?> list = (List<?>) value;

            StringBuilder result = new StringBuilder();

            for (Object item : list) {

                if (item == null) continue;

                if (result.length() > 0) {
                    result.append(", ");
                }

                result.append(item.toString());
            }

            return result.toString();
        }

        return String.valueOf(value);
    }

    // --------------------------------------------------
    // GO TO LOGIN
    // --------------------------------------------------

    private void goToLogin() {

        auth.signOut();

        Intent intent = new Intent(
            WorkerProfileActivity.this,
            MainActivity.class
            );

        intent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // --------------------------------------------------
    // TOAST
    // --------------------------------------------------

    private void showToast(String message) {

        Toast.makeText(
            WorkerProfileActivity.this,
            message,
            Toast.LENGTH_LONG
        ).show();
    }
}

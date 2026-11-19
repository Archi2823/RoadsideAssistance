
package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderRegistrationActivity extends AppCompatActivity {

    // Personal Information
    private EditText etFullName, etPhone, etEmail;

    // Professional Information
    private Spinner spServiceCategory;
    private EditText etGarage, etExperience, etSpecialization;

    // Services
    private CheckBox cbBikeRepair, cbCarRepair, cbBattery;
    private CheckBox cbTowing, cbMaintenance, cbEmergency;

    // Location
    private EditText etCity, etPincode, etServiceArea;

    // Availability
    private CheckBox cbMonday, cbTuesday, cbWednesday;
    private CheckBox cbThursday, cbFriday, cbSaturday, cbSunday;
    private EditText etWorkingHours;

    // Account Security
    private EditText etPassword, etConfirmPassword;

    private Button btnSubmit;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean isRegistering = false;

    private final String[] serviceCategories = {
            "Select Service Category",
            "Bike Mechanic",
            "Car Mechanic",
            "Auto Electrician",
            "Tyre / Puncture Specialist",
            "Towing Service Provider",
            "Multi-Service Mechanic",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_registration);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupServiceCategorySpinner();

        btnSubmit.setOnClickListener(v -> submitRegistration());
    }

    private void initializeViews() {

        etFullName = findViewById(R.id.etProviderFullName);
        etPhone = findViewById(R.id.etProviderPhone);
        etEmail = findViewById(R.id.etProviderEmail);

        spServiceCategory = findViewById(R.id.spProviderServiceCategory);

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

        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);

        etWorkingHours = findViewById(R.id.etProviderWorkingHours);

        etPassword = findViewById(R.id.etProviderPassword);
        etConfirmPassword = findViewById(R.id.etProviderConfirmPassword);

        btnSubmit = findViewById(R.id.btnSubmitProviderRegistration);
    }

    private void setupServiceCategorySpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                serviceCategories
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spServiceCategory.setAdapter(adapter);
    }

    private String getText(EditText editText) {
        return editText.getText().toString().trim();
    }

    private void submitRegistration() {

        if (isRegistering) {
            return;
        }

        // PERSONAL DETAILS
        String fullName = getText(etFullName);
        String phone = getText(etPhone);
        String email = getText(etEmail);

        // CATEGORY
        String serviceCategory = spServiceCategory.getSelectedItem().toString();

        // PROFESSIONAL DETAILS
        String garage = getText(etGarage);
        String experienceText = getText(etExperience);
        String specialization = getText(etSpecialization);

        // LOCATION
        String city = getText(etCity);
        String pincode = getText(etPincode);
        String serviceArea = getText(etServiceArea);

        // AVAILABILITY
        String workingHours = getText(etWorkingHours);

        // PASSWORD
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // ---------------- VALIDATION ----------------

        if (fullName.isEmpty()) {
            etFullName.setError("Enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (phone.length() != 10 ||
                !TextUtils.isDigitsOnly(phone)) {
            etPhone.setError("Enter a valid 10-digit mobile number");
            etPhone.requestFocus();
            return;
        }

        if (spServiceCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    "Please select a service category",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int experience = 0;

        if (!experienceText.isEmpty()) {
            try {
                experience = Integer.parseInt(experienceText);

                if (experience < 0 || experience > 80) {
                    etExperience.setError("Enter valid experience years");
                    etExperience.requestFocus();
                    return;
                }

            } catch (NumberFormatException e) {
                etExperience.setError("Experience must be in numbers");
                etExperience.requestFocus();
                return;
            }
        }

        if (city.isEmpty()) {
            etCity.setError("Enter your city");
            etCity.requestFocus();
            return;
        }

        if (pincode.length() != 6 ||
                !TextUtils.isDigitsOnly(pincode)) {
            etPincode.setError("Enter valid 6-digit pincode");
            etPincode.requestFocus();
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
            Toast.makeText(
                    this,
                    "Please select at least one service",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Collect available days

        List<String> availableDays = new ArrayList<>();

        if (cbMonday.isChecked())
            availableDays.add("Monday");

        if (cbTuesday.isChecked())
            availableDays.add("Tuesday");

        if (cbWednesday.isChecked())
            availableDays.add("Wednesday");

        if (cbThursday.isChecked())
            availableDays.add("Thursday");

        if (cbFriday.isChecked())
            availableDays.add("Friday");

        if (cbSaturday.isChecked())
            availableDays.add("Saturday");

        if (cbSunday.isChecked())
            availableDays.add("Sunday");

        if (availableDays.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select your available days",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Password validation

        if (password.isEmpty()) {
            etPassword.setError("Create a password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // Prevent duplicate submission

        isRegistering = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Creating Account...");

        final int finalExperience = experience;

        // Create Firebase Authentication Account

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

                    FirebaseUser firebaseUser = authResult.getUser();

                    if (firebaseUser == null) {
                        resetButton();
                        Toast.makeText(
                                this,
                                "Account creation failed",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    saveProviderData(
                            uid,
                            fullName,
                            phone,
                            email,
                            serviceCategory,
                            garage,
                            finalExperience,
                            specialization,
                            services,
                            city,
                            pincode,
                            serviceArea,
                            availableDays,
                            workingHours
                    );
                })
                .addOnFailureListener(e -> {

                    resetButton();

                    Toast.makeText(
                            this,
                            "Registration failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void saveProviderData(
            String uid,
            String fullName,
            String phone,
            String email,
            String serviceCategory,
            String garage,
            int experience,
            String specialization,
            List<String> services,
            String city,
            String pincode,
            String serviceArea,
            List<String> availableDays,
            String workingHours
    ) {

        // ---------------- USERS PROFILE ----------------

        Map<String, Object> workerProfile = new HashMap<>();

        workerProfile.put("uid", uid);
        workerProfile.put("name", fullName);
        workerProfile.put("fullName", fullName);
        workerProfile.put("phone", phone);
        workerProfile.put("email", email);

        workerProfile.put("role", "worker");
        workerProfile.put("approvalStatus", "pending");

        workerProfile.put("serviceCategory", serviceCategory);
        workerProfile.put("garageName", garage);
        workerProfile.put("experience", experience);
        workerProfile.put("specialization", specialization);

        workerProfile.put("services", services);

        workerProfile.put("city", city);
        workerProfile.put("pincode", pincode);
        workerProfile.put("serviceArea", serviceArea);

        workerProfile.put("availableDays", availableDays);
        workerProfile.put("workingHours", workingHours);

        workerProfile.put("approved", false);
        workerProfile.put("reviewed", false);

        workerProfile.put("createdAt", FieldValue.serverTimestamp());
        workerProfile.put("updatedAt", FieldValue.serverTimestamp());

        // ---------------- WORKER STATS ----------------

        Map<String, Object> workerStats = new HashMap<>();

        workerStats.put("workerId", uid);
        workerStats.put("activeRequestCount", 0);
        workerStats.put("completedRequestCount", 0);
        workerStats.put("updatedAt", FieldValue.serverTimestamp());

        // ---------------- ADMIN APPLICATION ----------------

        Map<String, Object> application = new HashMap<>();

        application.put("applicantId", uid);
        application.put("uid", uid);

        application.put("fullName", fullName);
        application.put("name", fullName);
        application.put("phone", phone);
        application.put("email", email);

        application.put("role", "worker");
        application.put("serviceCategory", serviceCategory);

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
        application.put("approvalStatus", "pending");
        application.put("reviewed", false);
        application.put("approved", false);

        application.put("createdAt", FieldValue.serverTimestamp());
        application.put("updatedAt", FieldValue.serverTimestamp());

        // ---------------- FIRESTORE BATCH ----------------

        WriteBatch batch = db.batch();

        batch.set(
                db.collection("users").document(uid),
                workerProfile
        );

        batch.set(
                db.collection("workerStats").document(uid),
                workerStats
        );

        // Admin application document

        batch.set(
                db.collection("workerApplications").document(uid),
                application
        );

        batch.commit()
                .addOnSuccessListener(unused -> {

                    resetButton();

                    new AlertDialog.Builder(this)
                            .setTitle("Registration Successful")
                            .setMessage(
                                    "Your service provider account has been created successfully.\n\n" +
                                            "Your application is pending admin approval. " +
                                            "You can log in once your account is approved."
                            )
                            .setCancelable(false)
                            .setPositiveButton("OK", (dialog, which) -> {

                                // Sign out registered provider.
                                // They will login again after approval.

                                auth.signOut();
                                finish();
                            })
                            .show();
                })
                .addOnFailureListener(e -> {

                    // Firestore failed. Delete the newly created Auth user
                    // so an incomplete account is not left behind.

                    FirebaseUser user = auth.getCurrentUser();

                    if (user != null && user.getUid().equals(uid)) {

                        user.delete()
                                .addOnCompleteListener(task -> {

                                    auth.signOut();
                                    resetButton();

                                    Toast.makeText(
                                            this,
                                            "Could not save registration. Please try again.\n" +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    } else {

                        resetButton();

                        Toast.makeText(
                                this,
                                "Could not save provider details: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void resetButton() {

        isRegistering = false;

        btnSubmit.setEnabled(true);
        btnSubmit.setText("Register as Service Provider");
    }
}


package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
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
import java.util.Locale;
import java.util.Map;

public class WorkerSignupActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Personal Information
    private EditText etFullName;
    private EditText etEmail;
    private EditText etPhone;

    // Professional Details
    private Spinner spServiceCategory;
    private EditText etGarageName;
    private EditText etExperience;
    private EditText etSpecialization;

    // Services Offered
    private CheckBox cbBikeRepair;
    private CheckBox cbCarRepair;
    private CheckBox cbBattery;
    private CheckBox cbTowing;
    private CheckBox cbGeneralMaintenance;
    private CheckBox cbEmergencyRepair;

    // Service Location
    private EditText etCity;
    private EditText etPincode;
    private EditText etServiceArea;

    // Availability
    private CheckBox cbMonday;
    private CheckBox cbTuesday;
    private CheckBox cbWednesday;
    private CheckBox cbThursday;
    private CheckBox cbFriday;
    private CheckBox cbSaturday;
    private CheckBox cbSunday;
    private EditText etWorkingHours;

    // Account Security
    private EditText etPassword;
    private EditText etConfirmPassword;

    // Buttons
    private Button btnSignup;
    private TextView tvLogin;

    private boolean isRegistering = false;

    private final String[] serviceCategories = {
            "Select Service Category",
            "Mechanical Assistance",
            "Battery Assistance",
            "Tyre Assistance",
            "Towing Assistance",
            "Fuel Assistance",
            "EV Assistance"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupServiceCategorySpinner();
        setupClickListeners();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initializeViews() {

        // Personal Information
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        // Professional Details
        spServiceCategory = findViewById(R.id.spServiceCategory);
        etGarageName = findViewById(R.id.etGarageName);
        etExperience = findViewById(R.id.etExperience);
        etSpecialization = findViewById(R.id.etSpecialization);

        // Services Offered
        cbBikeRepair = findViewById(R.id.cbBikeRepair);
        cbCarRepair = findViewById(R.id.cbCarRepair);
        cbBattery = findViewById(R.id.cbBattery);
        cbTowing = findViewById(R.id.cbTowing);
        cbGeneralMaintenance = findViewById(R.id.cbGeneralMaintenance);
        cbEmergencyRepair = findViewById(R.id.cbEmergencyRepair);

        // Service Location
        etCity = findViewById(R.id.etCity);
        etPincode = findViewById(R.id.etPincode);
        etServiceArea = findViewById(R.id.etServiceArea);

        // Availability
        cbMonday = findViewById(R.id.cbMonday);
        cbTuesday = findViewById(R.id.cbTuesday);
        cbWednesday = findViewById(R.id.cbWednesday);
        cbThursday = findViewById(R.id.cbThursday);
        cbFriday = findViewById(R.id.cbFriday);
        cbSaturday = findViewById(R.id.cbSaturday);
        cbSunday = findViewById(R.id.cbSunday);
        etWorkingHours = findViewById(R.id.etWorkingHours);

        // Account Security
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Buttons
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);
    }

    // =========================================================
    // SERVICE CATEGORY SPINNER
    // =========================================================

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
        spServiceCategory.setSelection(0);
    }

    // =========================================================
    // CLICK LISTENERS
    // =========================================================

    private void setupClickListeners() {

        btnSignup.setOnClickListener(v -> registerWorker());

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WorkerSignupActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }

    // =========================================================
    // REGISTER WORKER
    // =========================================================

    private void registerWorker() {

        if (isRegistering) {
            return;
        }

        // Read personal details
        final String name = etFullName.getText().toString().trim();

        final String email = etEmail.getText().toString().trim()
                .toLowerCase(Locale.ROOT);

        final String phone = etPhone.getText().toString().trim();

        // Professional details
        String selectedCategory = "";

        if (spServiceCategory.getSelectedItem() != null) {
            selectedCategory = spServiceCategory.getSelectedItem()
                    .toString().trim();
        }

        final String serviceCategory = selectedCategory;

        final String garageName = etGarageName.getText().toString().trim();

        String experienceText = etExperience.getText().toString().trim();

        final String specialization = etSpecialization.getText()
                .toString().trim();

        // Location
        final String city = etCity.getText().toString().trim();
        final String pincode = etPincode.getText().toString().trim();
        final String serviceArea = etServiceArea.getText().toString().trim();

        // Availability
        final String workingHours = etWorkingHours.getText()
                .toString().trim();

        // Password
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // =====================================================
        // VALIDATION
        // =====================================================

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            etFullName.setError("Enter a valid name");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Mobile number is required");
            etPhone.requestFocus();
            return;
        }

        if (!phone.matches("[6-9][0-9]{9}")) {
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

            spServiceCategory.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(city)) {
            etCity.setError("City is required");
            etCity.requestFocus();
            return;
        }

        if (!pincode.matches("[1-9][0-9]{5}")) {
            etPincode.setError("Enter a valid 6-digit pincode");
            etPincode.requestFocus();
            return;
        }

        // Experience is optional; default is 0
        int parsedExperience = 0;

        if (!TextUtils.isEmpty(experienceText)) {
            try {
                parsedExperience = Integer.parseInt(experienceText);

                if (parsedExperience < 0 || parsedExperience > 99) {
                    etExperience.setError(
                            "Experience must be between 0 and 99 years"
                    );

                    etExperience.requestFocus();
                    return;
                }

            } catch (NumberFormatException e) {
                etExperience.setError("Enter valid experience");
                etExperience.requestFocus();
                return;
            }
        }

        // Final experience value for Firebase listeners
        final int experience = parsedExperience;

        // At least one service should be selected
        List<String> selectedServices = getSelectedServices();

        if (selectedServices.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select at least one service offered",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // At least one availability day is required
        List<String> selectedDays = getSelectedAvailableDays();

        if (selectedDays.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select at least one available day",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // Final copies for Firebase lambda expressions
        final List<String> services = new ArrayList<>(selectedServices);
        final List<String> availableDays = new ArrayList<>(selectedDays);

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (!isStrongPassword(password)) {
            etPassword.setError(
                    "Password must contain 8+ characters, uppercase, lowercase, number and special character"
            );

            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // =====================================================
        // CREATE FIREBASE AUTH ACCOUNT
        // =====================================================

        setRegistering(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        setRegistering(false);

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";

                        Toast.makeText(
                                WorkerSignupActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser firebaseUser = auth.getCurrentUser();

                    if (firebaseUser == null) {
                        setRegistering(false);

                        Toast.makeText(
                                WorkerSignupActivity.this,
                                "Account created but user details could not be loaded.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    final String uid = firebaseUser.getUid();

                    // Save profile and worker statistics
                    saveWorkerToFirestore(
                            uid,
                            name,
                            email,
                            phone,
                            serviceCategory,
                            garageName,
                            experience,
                            specialization,
                            services,
                            city,
                            pincode,
                            serviceArea,
                            availableDays,
                            workingHours
                    );
                });
    }

    // =========================================================
    // SAVE WORKER PROFILE + WORKER STATS
    // =========================================================

    private void saveWorkerToFirestore(
            String uid,
            String name,
            String email,
            String phone,
            String serviceCategory,
            String garageName,
            int experience,
            String specialization,
            List<String> services,
            String city,
            String pincode,
            String serviceArea,
            List<String> availableDays,
            String workingHours
    ) {

        Map<String, Object> workerData = new HashMap<>();

        // Existing fields - preserve compatibility
        workerData.put("uid", uid);
        workerData.put("name", name);
        workerData.put("email", email);
        workerData.put("phone", phone);
        workerData.put("role", "worker");
        workerData.put("approvalStatus", "pending");
        workerData.put("serviceCategory", serviceCategory);
        workerData.put("createdAt", FieldValue.serverTimestamp());

        // Additional Service Provider Registration fields
        workerData.put("garageName", garageName);
        workerData.put("experience", experience);
        workerData.put("specialization", specialization);
        workerData.put("services", services);
        workerData.put("city", city);
        workerData.put("pincode", pincode);
        workerData.put("serviceArea", serviceArea);
        workerData.put("availableDays", availableDays);
        workerData.put("workingHours", workingHours);

        // Existing workerStats structure
        Map<String, Object> workerStats = new HashMap<>();
        workerStats.put("uid", uid);
        workerStats.put("activeRequestCount", 0);
        workerStats.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();

        batch.set(
                db.collection("users").document(uid),
                workerData
        );

        batch.set(
                db.collection("workerStats").document(uid),
                workerStats
        );

        batch.commit()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            WorkerSignupActivity.this,
                            "Registration successful! Your account is awaiting admin approval.",
                            Toast.LENGTH_LONG
                    ).show();

                    // Preserve existing flow: sign out and return to MainActivity
                    auth.signOut();

                    Intent intent = new Intent(
                            WorkerSignupActivity.this,
                            MainActivity.class
                    );

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    // Rollback Firebase Auth account if Firestore save fails
                    FirebaseUser currentUser = auth.getCurrentUser();

                    if (currentUser != null) {
                        currentUser.delete()
                                .addOnCompleteListener(deleteTask -> {

                                    auth.signOut();
                                    setRegistering(false);

                                    Toast.makeText(
                                            WorkerSignupActivity.this,
                                            "Registration save failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    } else {
                        auth.signOut();
                        setRegistering(false);

                        Toast.makeText(
                                WorkerSignupActivity.this,
                                "Registration save failed: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    // =========================================================
    // GET SELECTED SERVICES
    // =========================================================

    private List<String> getSelectedServices() {

        List<String> services = new ArrayList<>();

        if (cbBikeRepair.isChecked()) {
            services.add("Bike Repair");
        }

        if (cbCarRepair.isChecked()) {
            services.add("Car Repair");
        }

        if (cbBattery.isChecked()) {
            services.add("Battery Assistance / Jump Start");
        }

        if (cbTowing.isChecked()) {
            services.add("Towing Assistance");
        }

        if (cbGeneralMaintenance.isChecked()) {
            services.add("General Maintenance");
        }

        if (cbEmergencyRepair.isChecked()) {
            services.add("Emergency Roadside Repair");
        }

        return services;
    }

    // =========================================================
    // GET SELECTED AVAILABLE DAYS
    // =========================================================

    private List<String> getSelectedAvailableDays() {

        List<String> days = new ArrayList<>();

        if (cbMonday.isChecked()) {
            days.add("Monday");
        }

        if (cbTuesday.isChecked()) {
            days.add("Tuesday");
        }

        if (cbWednesday.isChecked()) {
            days.add("Wednesday");
        }

        if (cbThursday.isChecked()) {
            days.add("Thursday");
        }

        if (cbFriday.isChecked()) {
            days.add("Friday");
        }

        if (cbSaturday.isChecked()) {
            days.add("Saturday");
        }

        if (cbSunday.isChecked()) {
            days.add("Sunday");
        }

        return days;
    }

    // =========================================================
    // PASSWORD VALIDATION
    // =========================================================

    private boolean isStrongPassword(String password) {

        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialCharacter = false;

        for (char c : password.toCharArray()) {

            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecialCharacter = true;
            }
        }

        return hasUppercase
                && hasLowercase
                && hasDigit
                && hasSpecialCharacter;
    }

    // =========================================================
    // REGISTRATION LOADING STATE
    // =========================================================

    private void setRegistering(boolean registering) {

        isRegistering = registering;

        if (btnSignup != null) {
            btnSignup.setEnabled(!registering);

            btnSignup.setText(
                    registering
                            ? "Registering..."
                            : "Register as Service Provider"
            );
        }
    }
}

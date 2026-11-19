
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class activity_signup extends AppCompatActivity {

    private static final String TAG = "SIGNUP_FIRESTORE";

    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private Button btnSignup;
    private TextView tvSignupMessage;
    private TextView tvAccountTypeMessage;

    private Spinner spinnerAccountType;
    private LinearLayout customerFormContainer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String pendingName;
    private String pendingEmail;
    private String pendingPhone;

    private boolean signupInProgress = false;
    private boolean openingWorkerSignup = false;

    private String selectedAccountType = "Customer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Customer fields
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Common views
        btnSignup = findViewById(R.id.btnSignup);
        tvSignupMessage = findViewById(R.id.tvSignupMessage);
        tvAccountTypeMessage = findViewById(R.id.tvAccountTypeMessage);

        spinnerAccountType = findViewById(R.id.spinnerAccountType);
        customerFormContainer = findViewById(R.id.customerFormContainer);

        if (etName == null
                || etEmail == null
                || etPhone == null
                || etPassword == null
                || etConfirmPassword == null
                || btnSignup == null
                || tvSignupMessage == null
                || tvAccountTypeMessage == null
                || spinnerAccountType == null
                || customerFormContainer == null) {

            Toast.makeText(
                    this,
                    "Signup screen initialization error.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        setupAccountTypeDropdown();

        btnSignup.setOnClickListener(v -> {

            if (selectedAccountType.equals("Customer")) {
                validateAndSignup();
            } else if (selectedAccountType.equals("Worker")) {
                openWorkerSignup();
            } else {
                showAdminRestriction();
            }
        });
    }

    // Setup account type dropdown
    private void setupAccountTypeDropdown() {

        String[] accountTypes = {
                "Customer",
                "Worker",
                "Admin"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                accountTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerAccountType.setAdapter(adapter);
        spinnerAccountType.setSelection(0);

        spinnerAccountType.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        if (openingWorkerSignup) {
                            return;
                        }

                        selectedAccountType = accountTypes[position];

                        tvSignupMessage.setText("");
                        tvAccountTypeMessage.setVisibility(View.GONE);

                        if (selectedAccountType.equals("Customer")) {
                            showCustomerForm();
                        } else if (selectedAccountType.equals("Worker")) {
                            showWorkerOption();
                        } else {
                            showAdminRestriction();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    // Customer signup form
    private void showCustomerForm() {

        customerFormContainer.setVisibility(View.VISIBLE);

        btnSignup.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(true);
        btnSignup.setText("CREATE CUSTOMER ACCOUNT");

        tvAccountTypeMessage.setVisibility(View.GONE);
    }

    // Worker signup option
    private void showWorkerOption() {

        customerFormContainer.setVisibility(View.GONE);

        btnSignup.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(true);
        btnSignup.setText("CONTINUE TO WORKER REGISTRATION");

        tvAccountTypeMessage.setVisibility(View.VISIBLE);
        tvAccountTypeMessage.setText(
                "Worker registration requires additional service and " +
                        "professional details. Continue to the worker registration form."
        );
        tvAccountTypeMessage.setTextColor(
                android.graphics.Color.parseColor("#2563EB")
        );
    }

    // Open existing worker registration screen
    private void openWorkerSignup() {

        if (openingWorkerSignup) {
            return;
        }

        openingWorkerSignup = true;

        Intent intent = new Intent(
                activity_signup.this,
                WorkerSignupActivity.class
        );

        startActivity(intent);

        // Reset dropdown selection to Customer
        spinnerAccountType.setSelection(0);

        selectedAccountType = "Customer";
        showCustomerForm();

        openingWorkerSignup = false;
    }

    // Admin signup is restricted
    private void showAdminRestriction() {

        customerFormContainer.setVisibility(View.GONE);

        btnSignup.setVisibility(View.VISIBLE);
        btnSignup.setEnabled(false);
        btnSignup.setText("ADMIN REGISTRATION RESTRICTED");

        tvAccountTypeMessage.setVisibility(View.VISIBLE);
        tvAccountTypeMessage.setText(
                "Admin accounts cannot be created publicly. " +
                        "Only an authorized administrator account can access Admin Dashboard."
        );

        tvAccountTypeMessage.setTextColor(
                android.graphics.Color.parseColor("#C62828")
        );
    }

    // Validate customer signup
    private void validateAndSignup() {

        if (signupInProgress) {
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        tvSignupMessage.setText("");

        // Name validation
        if (!name.matches("^[\\p{L}][\\p{L} .'-]{1,49}$")) {
            etName.setError(
                    "Enter a valid name (minimum 2 characters)"
            );
            etName.requestFocus();
            return;
        }

        // Email validation
        if (email.isEmpty()
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Indian phone validation
        if (!phone.matches("^[6-9][0-9]{9}$")) {
            etPhone.setError(
                    "Enter a valid 10-digit Indian mobile number"
            );
            etPhone.requestFocus();
            return;
        }

        // Password validation
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9].*")) {

            etPassword.setError(
                    "Use 8+ characters with uppercase, lowercase, " +
                            "number and special character"
            );
            etPassword.requestFocus();
            return;
        }

        // Confirm password
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        createAccount(name, email, phone, password);
    }

    // Create Firebase customer account
    private void createAccount(
            String name,
            String email,
            String phone,
            String password) {

        pendingName = name;
        pendingEmail = email;
        pendingPhone = phone;

        signupInProgress = true;
        setSignupLoading(true);

        tvSignupMessage.setText("Creating your account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        String error = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Account creation failed.";

                        Log.e(TAG,
                                "Firebase Authentication signup failed",
                                task.getException());

                        showError(error);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {

                        showError(
                                "Account created, but user session " +
                                        "was not found. Please sign in."
                        );
                        return;
                    }

                    Log.d(TAG,
                            "Customer account created. UID: " +
                                    user.getUid());

                    UserProfileChangeRequest profileRequest =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    user.updateProfile(profileRequest)
                            .addOnCompleteListener(profileTask -> {

                                if (!profileTask.isSuccessful()) {
                                    Log.e(TAG,
                                            "Display name update failed",
                                            profileTask.getException());
                                }

                                // Save Firestore profile even if
                                // optional display name update fails.
                                saveUserDetails(
                                        name,
                                        email,
                                        phone,
                                        user
                                );
                            });
                });
    }

    // Save customer profile in Firestore
    private void saveUserDetails(
            String name,
            String email,
            String phone,
            FirebaseUser user) {

        String uid = user.getUid();

        Map<String, Object> userData = new HashMap<>();

        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("role", "customer");
        userData.put("createdAt", FieldValue.serverTimestamp());

        Log.d(TAG, "Saving Firestore profile for UID: " + uid);

        db.collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {

                    Log.d(TAG,
                            "Firestore customer profile created. UID: " +
                                    uid);

                    Toast.makeText(
                            activity_signup.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    tvSignupMessage.setText(
                            "Account created successfully!"
                    );

                    // Open login screen
                    Intent intent = new Intent(
                            activity_signup.this,
                            MainActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    Log.e(TAG,
                            "Firestore profile creation failed. UID: " +
                                    uid,
                            e);

                    showError(
                            "Account was created, but profile could not " +
                                    "be saved. Please check your internet and " +
                                    "Firestore Rules. Error: " +
                                    e.getLocalizedMessage()
                    );
                });
    }

    // Signup loading state
    private void setSignupLoading(boolean loading) {

        btnSignup.setEnabled(!loading);

        if (loading) {
            btnSignup.setText("CREATING ACCOUNT...");
        } else if (selectedAccountType.equals("Customer")) {
            btnSignup.setText("CREATE CUSTOMER ACCOUNT");
        }
    }

    // Show signup error
    private void showError(String message) {

        signupInProgress = false;

        tvSignupMessage.setText(message);
        setSignupLoading(false);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

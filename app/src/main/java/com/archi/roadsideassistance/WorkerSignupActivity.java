package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class WorkerSignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private EditText etPassword, etConfirmPassword;

    private Spinner spWorkerCategory;

    private Button btnSignup;
    private TextView tvLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final String[] workerCategories = {
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

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        spWorkerCategory = findViewById(R.id.spWorkerCategory);

        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        setupCategorySpinner();

        btnSignup.setOnClickListener(v -> registerWorker());

        tvLogin.setOnClickListener(v -> finish());
    }

    private void setupCategorySpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                workerCategories
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spWorkerCategory.setAdapter(adapter);
    }

    private void registerWorker() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        String workerCategory =
                spWorkerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your full name");
            etName.requestFocus();
            return;
        }

        if (name.length() < 3) {
            etName.setError("Name must contain at least 3 characters");
            etName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            etPhone.setError("Enter a valid 10-digit Indian mobile number");
            etPhone.requestFocus();
            return;
        }

        if (spWorkerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(
                    this,
                    "Please select your service category",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 8 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*[0-9].*") ||
                !password.matches(".*[^A-Za-z0-9].*")) {

            etPassword.setError(
                    "Password must have 8 characters, uppercase, lowercase, number and special character"
            );
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnSignup.setEnabled(false);
        btnSignup.setText("Creating Account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser firebaseUser = auth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveWorkerProfile(
                                    firebaseUser,
                                    name,
                                    email,
                                    phone,
                                    workerCategory
                            );
                        } else {
                            signupFailed("Unable to create worker profile.");
                        }

                    } else {

                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed.";

                        signupFailed(error);
                    }
                });
    }

    private void saveWorkerProfile(
            FirebaseUser user,
            String name,
            String email,
            String phone,
            String workerCategory
    ) {

        String uid = user.getUid();

        Map<String, Object> worker = new HashMap<>();

        worker.put("uid", uid);
        worker.put("name", name);
        worker.put("email", email);
        worker.put("phone", phone);
        worker.put("role", "worker");
        worker.put("approvalStatus", "pending");

        // Selected service category
        worker.put("serviceCategory", workerCategory);

        worker.put("createdAt", FieldValue.serverTimestamp());

        // Initial workload counter for this worker
        Map<String, Object> workerStats = new HashMap<>();

        workerStats.put("uid", uid);
        workerStats.put("activeRequestCount", 0);
        workerStats.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();

        batch.set(
                db.collection("users").document(uid),
                worker
        );

        batch.set(
                db.collection("workerStats").document(uid),
                workerStats
        );

        batch.commit()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            WorkerSignupActivity.this,
                            "Worker registration submitted. Waiting for admin approval.",
                            Toast.LENGTH_LONG
                    ).show();

                    auth.signOut();

                    Intent intent = new Intent(
                            WorkerSignupActivity.this,
                            MainActivity.class
                    );

                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {

                    user.delete().addOnCompleteListener(deleteTask -> {
                        auth.signOut();

                        signupFailed(
                                "Unable to save worker profile: "
                                        + e.getLocalizedMessage()
                        );
                    });
                });
    }

    private void signupFailed(String message) {

        btnSignup.setEnabled(true);
        btnSignup.setText("Sign Up");

        Toast.makeText(
                WorkerSignupActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
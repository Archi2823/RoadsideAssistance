
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WorkerSignupActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private EditText etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

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

        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLogin);

        btnSignup.setOnClickListener(v -> registerWorker());

        tvLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void registerWorker() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword =
                etConfirmPassword.getText().toString();

        // Name validation
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

        // Email validation
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

        // Phone validation
        if (!phone.matches("^[6-9][0-9]{9}$")) {
            etPhone.setError("Enter a valid 10-digit Indian mobile number");
            etPhone.requestFocus();
            return;
        }

        // Password validation
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

                        FirebaseUser firebaseUser =
                                auth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveWorkerProfile(
                                    firebaseUser,
                                    name,
                                    email,
                                    phone
                            );
                        } else {
                            signupFailed(
                                    "Unable to create worker profile."
                            );
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
            String phone
    ) {

        String uid = user.getUid();

        Map<String, Object> worker = new HashMap<>();

        worker.put("uid", uid);
        worker.put("name", name);
        worker.put("email", email);
        worker.put("phone", phone);
        worker.put("role", "worker");
        worker.put("approvalStatus", "pending");
        worker.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(worker)
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

                    // Remove the newly created Auth account if
                    // its Firestore profile could not be saved.
                    user.delete().addOnCompleteListener(deleteTask -> {
                        auth.signOut();

                        signupFailed(
                                "Unable to save worker profile: "
                                        + e.getMessage()
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

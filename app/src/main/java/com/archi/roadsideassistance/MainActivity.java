
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private RadioGroup rgUserRole;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String selectedRole = "customer";
    private boolean loginInProgress = false;

    private static final String TAG = "MAIN_CHECK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "MainActivity onCreate started");

        // Load original login screen
        setContentView(R.layout.activity_main);

        Log.e(TAG, "activity_main layout loaded");

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.e(TAG, "Firebase initialized");

        // Connect views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        rgUserRole = findViewById(R.id.rgUserRole);

        // Verify views
        if (etEmail == null
                || etPassword == null
                || btnLogin == null
                || tvRegister == null
                || tvForgotPassword == null
                || rgUserRole == null) {

            Log.e(TAG, "ERROR: One or more views are missing");

            Toast.makeText(
                    this,
                    "Login screen initialization error.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Log.e(TAG, "All views connected successfully");

        // Default role: Customer
        rgUserRole.check(R.id.rbCustomer);
        selectedRole = "customer";

        // Update signup text
        updateSignupText();

        // Role selection listener
        rgUserRole.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.rbCustomer) {
                selectedRole = "customer";

            } else if (checkedId == R.id.rbWorker) {
                selectedRole = "worker";

            } else if (checkedId == R.id.rbAdmin) {
                selectedRole = "admin";
            }

            Log.d(TAG, "Selected role: " + selectedRole);

            updateSignupText();
        });

        // Login button
        btnLogin.setOnClickListener(v -> loginUser());

        // Signup navigation
        tvRegister.setOnClickListener(v -> openSignupScreen());

        // Forgot password navigation
        tvForgotPassword.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    ResetPasswordActivity.class
            );

            startActivity(intent);
        });

        Log.e(TAG, "MainActivity initialization completed successfully");
    }

    // Update signup text according to selected role
    private void updateSignupText() {

        if (selectedRole.equals("customer")) {

            tvRegister.setVisibility(TextView.VISIBLE);

            tvRegister.setText(
                    "Don't have a Customer account? Customer Sign Up"
            );

        } else if (selectedRole.equals("worker")) {

            tvRegister.setVisibility(TextView.VISIBLE);

            tvRegister.setText(
                    "Don't have a Worker account? Worker Sign Up"
            );

        } else {

            // Admin cannot register publicly
            tvRegister.setVisibility(TextView.GONE);
        }
    }

    // Open appropriate signup screen
    private void openSignupScreen() {

        Intent intent;

        if (selectedRole.equals("customer")) {

            intent = new Intent(
                    MainActivity.this,
                    activity_signup.class
            );

        } else if (selectedRole.equals("worker")) {

            intent = new Intent(
                    MainActivity.this,
                    WorkerSignupActivity.class
            );

        } else {

            // Admin signup is not publicly available
            return;
        }

        startActivity(intent);
    }

    // Login user
    private void loginUser() {

        if (loginInProgress) {
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validate email
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

        // Validate password
        if (TextUtils.isEmpty(password)) {

            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        loginInProgress = true;

        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        rgUserRole.setEnabled(false);

        final String loginRole = selectedRole;

        Log.d(TAG, "Starting Firebase login for role: " + loginRole);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Log.d(TAG, "Firebase authentication successful");

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            verifyUserRole(user, loginRole);

                        } else {

                            auth.signOut();

                            loginFailed(
                                    "Unable to verify user. Please try again."
                            );
                        }

                    } else {

                        Log.e(TAG, "Firebase login failed", task.getException());

                        loginFailed(
                                "Login failed. Check your email and password."
                        );
                    }
                });
    }

    // Verify user role from Firestore
    private void verifyUserRole(FirebaseUser user, String loginRole) {

        String uid = user.getUid();

        Log.d(TAG, "Checking Firestore role for UID: " + uid);

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        auth.signOut();

                        loginFailed("User profile not found.");
                        return;
                    }

                    String actualRole = document.getString("role");

                    if (actualRole == null) {

                        auth.signOut();

                        loginFailed("User role is not configured.");
                        return;
                    }

                    actualRole = actualRole.trim()
                            .toLowerCase(Locale.ROOT);

                    Log.d(TAG, "Firestore role: " + actualRole);

                    // Verify selected role
                    if (!actualRole.equals(loginRole)) {

                        auth.signOut();

                        loginFailed(
                                "Selected role does not match your account."
                        );
                        return;
                    }

                    // Worker approval verification
                    if (actualRole.equals("worker")) {

                        String approvalStatus =
                                document.getString("approvalStatus");

                        Log.d(TAG, "Worker approval status: " + approvalStatus);

                        if (!"approved".equalsIgnoreCase(approvalStatus)) {

                            auth.signOut();

                            loginFailed(
                                    "Worker account is pending administrator approval."
                            );
                            return;
                        }
                    }

                    // Admin verification
                    if (actualRole.equals("admin")) {

                        Log.d(TAG, "Opening Admin Dashboard");

                        openDashboard("admin");
                        return;
                    }

                    Toast.makeText(
                            MainActivity.this,
                            "Login successful!",
                            Toast.LENGTH_SHORT
                    ).show();

                    openDashboard(actualRole);

                })
                .addOnFailureListener(e -> {

                    Log.e(TAG, "Firestore role verification failed", e);

                    auth.signOut();

                    loginFailed(
                            "Unable to verify account. Please try again."
                    );
                });
    }

    // Navigate to role-specific dashboard
    private void openDashboard(String role) {

        Intent intent;

        if (role.equals("customer")) {

            intent = new Intent(
                    MainActivity.this,
                    HomeActivity.class
            );

        } else if (role.equals("worker")) {

            intent = new Intent(
                    MainActivity.this,
                    WorkerDashboardActivity.class
            );

        } else if (role.equals("admin")) {

            intent = new Intent(
                    MainActivity.this,
                    AdminDashboardActivity.class
            );

        } else {

            auth.signOut();

            loginFailed("Invalid account role.");
            return;
        }

        Log.d(TAG, "Navigating to dashboard: " + role);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // Restore login screen after login failure
    private void loginFailed(String message) {

        Log.e(TAG, "Login failed: " + message);

        loginInProgress = false;

        btnLogin.setEnabled(true);
        btnLogin.setText("Login");

        rgUserRole.setEnabled(true);

        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}


package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
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

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean loginInProgress = false;

    private static final String TAG = "MAIN_CHECK";

    // Existing authorized admin UID
    private static final String ADMIN_UID =
            "mfKj4Ggasqe1aQT9z5fb9JokXV82";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate started");

        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        if (etEmail == null
                || etPassword == null
                || btnLogin == null
                || tvRegister == null
                || tvForgotPassword == null) {

            Log.e(TAG, "Login screen initialization error");

            Toast.makeText(
                    this,
                    "Login screen initialization error.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> openSignupScreen());

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ResetPasswordActivity.class
            );
            startActivity(intent);
        });

        Log.d(TAG, "MainActivity initialization completed");
    }

    // Open the common signup screen
    private void openSignupScreen() {
        Intent intent = new Intent(
                MainActivity.this,
                activity_signup.class
        );

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

        Log.d(TAG, "Starting Firebase login");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Log.d(TAG, "Firebase authentication successful");

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            verifyUserRole(user);
                        } else {
                            auth.signOut();
                            loginFailed(
                                    "Unable to verify user. Please try again."
                            );
                        }

                    } else {

                        Log.e(TAG, "Firebase login failed",
                                task.getException());

                        loginFailed(
                                "Login failed. Check your email and password."
                        );
                    }
                });
    }

    // Automatically detect account role from Firestore
    private void verifyUserRole(FirebaseUser user) {

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

                    Log.d(TAG, "Detected role: " + actualRole);

                    // Only supported roles
                    if (!actualRole.equals("customer")
                            && !actualRole.equals("worker")
                            && !actualRole.equals("admin")) {

                        auth.signOut();

                        loginFailed("Invalid account role.");
                        return;
                    }

                    // Worker approval check
                    if (actualRole.equals("worker")) {

                        String approvalStatus =
                                document.getString("approvalStatus");

                        Log.d(TAG,
                                "Worker approval status: " + approvalStatus);

                        if (!"approved".equalsIgnoreCase(approvalStatus)) {

                            auth.signOut();

                            loginFailed(
                                    "Worker account is pending administrator approval."
                            );
                            return;
                        }
                    }

                    // Admin UID check
                    if (actualRole.equals("admin")) {

                        if (!uid.equals(ADMIN_UID)) {

                            auth.signOut();

                            loginFailed(
                                    "Unauthorized administrator account."
                            );
                            return;
                        }

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

                    Log.e(TAG,
                            "Firestore role verification failed", e);

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
        btnLogin.setText("LOGIN");

        Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

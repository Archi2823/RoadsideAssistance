
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
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

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String pendingName;
    private String pendingEmail;
    private String pendingPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSignup = findViewById(R.id.btnSignup);
        tvSignupMessage = findViewById(R.id.tvSignupMessage);

        btnSignup.setOnClickListener(v -> validateAndSignup());
    }

    private void validateAndSignup() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword =
                etConfirmPassword.getText().toString();

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
                    "Use 8+ characters with uppercase, lowercase, "
                            + "number and special character"
            );
            etPassword.requestFocus();
            return;
        }

        // Confirm password validation
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        createAccount(name, email, phone, password);
    }

    private void createAccount(
            String name,
            String email,
            String phone,
            String password) {

        pendingName = name;
        pendingEmail = email;
        pendingPhone = phone;

        setSignupLoading(true);
        tvSignupMessage.setText("Creating your account...");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        String error = task.getException() != null
                                ? task.getException().getLocalizedMessage()
                                : "Account creation failed.";

                        Log.e(TAG, "Firebase Authentication signup failed",
                                task.getException());

                        showError(error);
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        showError(
                                "Account created, but user session "
                                        + "was not found. Please sign in."
                        );
                        return;
                    }

                    Log.d(TAG, "Authentication account created. UID: "
                            + user.getUid());

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

                                // Save Firestore profile even if the
                                // optional display-name update fails.
                                saveUserDetails(
                                        name, email, phone, user
                                );
                            });
                });
    }

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
                            "Firestore profile created successfully. UID: "
                                    + uid);

                    Toast.makeText(
                            activity_signup.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    tvSignupMessage.setText(
                            "Account created successfully!"
                    );

                    // Open the existing login screen.
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
                            "Firestore profile creation failed. UID: "
                                    + uid,
                            e);

                    // The Authentication account already exists.
                    // Do not try creating the account again.
                    showError(
                            "Account was created, but profile could not "
                                    + "be saved. Please check your internet "
                                    + "and Firebase Firestore Rules. Error: "
                                    + e.getLocalizedMessage()
                    );
                });
    }

    private void setSignupLoading(boolean loading) {
        btnSignup.setEnabled(!loading);
        btnSignup.setText(
                loading ? "CREATING ACCOUNT..." : "CREATE ACCOUNT"
        );
    }

    private void showError(String message) {
        tvSignupMessage.setText(message);
        setSignupLoading(false);
    }
}

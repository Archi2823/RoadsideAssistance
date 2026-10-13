package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class activity_signup extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private EditText etPassword, etConfirmPassword;
    private Button btnSignup;
    private TextView tvSignupMessage;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

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
                    "Enter a valid name (minimum 2 characters)");
            etName.requestFocus();
            return;
        }

        // Email validation
        if (email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Indian mobile number validation
        if (!phone.matches("^[6-9][0-9]{9}$")) {
            etPhone.setError(
                    "Enter a valid 10-digit Indian mobile number");
            etPhone.requestFocus();
            return;
        }

        // Password validation
        if (password.length() < 8 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*[0-9].*") ||
                !password.matches(".*[^A-Za-z0-9].*")) {

            etPassword.setError(
                    "Use 8+ characters with uppercase, lowercase, " +
                            "number and special character");
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

    private void createAccount(String name, String email,
                               String phone, String password) {

        btnSignup.setEnabled(false);
        btnSignup.setText("CREATING ACCOUNT...");
        tvSignupMessage.setText("");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed.";

                        showError(error);
                        return;
                    }

                    if (auth.getCurrentUser() == null) {
                        showError("Account created. Please sign in.");
                        return;
                    }

                    UserProfileChangeRequest profileUpdate =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    auth.getCurrentUser()
                            .updateProfile(profileUpdate)
                            .addOnCompleteListener(this, profileTask -> {
                                saveUserDetails(name, email, phone);
                            });
                });
    }

    private void saveUserDetails(String name, String email,
                                 String phone) {

        if (auth.getCurrentUser() == null) {
            showError("Please sign in again.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", "customer");
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            activity_signup.this,
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            activity_signup.this,
                            MainActivity.class
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError(
                            "Profile save failed. Please retry. " +
                                    e.getMessage());
                });
    }

    private void showError(String message) {
        tvSignupMessage.setText(message);
        btnSignup.setEnabled(true);
        btnSignup.setText("CREATE ACCOUNT");
    }
}


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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "PROFILE_UPDATE";

    private EditText etProfileName;
    private EditText etProfileEmail;
    private EditText etProfilePhone;

    private Button btnSaveProfile;
    private Button btnBackHome;
    private TextView tvProfileMessage;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();

        etProfileName = findViewById(R.id.etProfileName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfilePhone = findViewById(R.id.etProfilePhone);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBackHome = findViewById(R.id.btnBackHome);
        tvProfileMessage = findViewById(R.id.tvProfileMessage);

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        // Email is managed by Firebase Authentication.
        etProfileEmail.setText(currentUser.getEmail());
        etProfileEmail.setEnabled(false);

        loadProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        btnBackHome.setOnClickListener(v -> finish());
    }

    // Load the signed-in user's Firestore profile.
    private void loadProfile() {
        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        setLoading(true);
        tvProfileMessage.setText("Loading profile...");

        String uid = currentUser.getUid();

        Log.d(TAG, "Loading profile for UID: " + uid);

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    setLoading(false);

                    if (!document.exists()) {
                        tvProfileMessage.setText(
                                "Profile document not found. "
                                        + "Check Firestore users collection."
                        );

                        Log.e(TAG,
                                "No users document found for UID: " + uid);
                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");

                    etProfileName.setText(name == null ? "" : name);
                    etProfilePhone.setText(phone == null ? "" : phone);

                    // Use the current Firebase Authentication email.
                    etProfileEmail.setText(currentUser.getEmail());

                    tvProfileMessage.setText("");
                    Log.d(TAG, "Profile loaded successfully.");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    tvProfileMessage.setText(
                            "Could not load profile. See Logcat."
                    );

                    Log.e(TAG, "Firestore profile read failed", e);

                    Toast.makeText(
                            ProfileActivity.this,
                            "Load failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Validate and save the profile.
    private void saveProfile() {
        String name = etProfileName.getText().toString().trim();
        String email = etProfileEmail.getText().toString().trim();
        String phone = etProfilePhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || name.length() < 2) {
            etProfileName.setError("Enter your full name");
            etProfileName.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etProfileEmail.setError("Enter a valid email");
            etProfileEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)
                || !phone.matches("^[6-9][0-9]{9}$")) {
            etProfilePhone.setError(
                    "Enter a valid 10-digit Indian mobile number"
            );
            etProfilePhone.requestFocus();
            return;
        }

        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        // Email changes are not handled by this screen.
        String registeredEmail = currentUser.getEmail();

        if (registeredEmail == null
                || !registeredEmail.equalsIgnoreCase(email)) {
            tvProfileMessage.setText(
                    "Please use your registered email address."
            );
            return;
        }

        setLoading(true);
        tvProfileMessage.setText("Saving changes...");

        String uid = currentUser.getUid();

        // Update only the fields permitted by our Firestore rules.
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        Log.d(TAG, "Updating profile for UID: " + uid);

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    // Update Firebase Authentication display name too.
                    UserProfileChangeRequest request =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    currentUser.updateProfile(request)
                            .addOnCompleteListener(authTask -> {
                                setLoading(false);
                                tvProfileMessage.setText("");

                                if (authTask.isSuccessful()) {
                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Profile updated successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    Log.d(TAG,
                                            "Firestore and Auth profile updated.");
                                } else {
                                    // Firestore save succeeded even if
                                    // updating the Auth display name failed.
                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Details saved, but display name "
                                                    + "could not be refreshed.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    Log.e(TAG,
                                            "Auth display name update failed",
                                            authTask.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    tvProfileMessage.setText(
                            "Profile update failed. Check Logcat."
                    );

                    Log.e(TAG, "Firestore update failed", e);

                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreError =
                                (FirebaseFirestoreException) e;

                        Log.e(TAG,
                                "Firestore error code: "
                                        + firestoreError.getCode());
                    }

                    Toast.makeText(
                            ProfileActivity.this,
                            "Update failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void setLoading(boolean loading) {
        btnSaveProfile.setEnabled(!loading);
        btnSaveProfile.setText(
                loading ? "Please wait..." : "Save Changes"
        );
    }

    private void openLoginScreen() {
        Intent intent = new Intent(
                ProfileActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

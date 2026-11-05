
package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "PROFILE_UPDATE";

    private EditText etProfileName, etProfileEmail, etProfilePhone;

    private ImageView ivProfilePhoto;

    private Button btnChangePhoto;
    private Button btnSaveProfile;
    private Button btnBackHome;
    private Button btnMyVehicles;

    private TextView tvProfileMessage;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<Intent> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUser = auth.getCurrentUser();

        // Initialize views
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);

        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBackHome = findViewById(R.id.btnBackHome);
        btnMyVehicles = findViewById(R.id.btnMyVehicles);

        etProfileName = findViewById(R.id.etProfileName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfilePhone = findViewById(R.id.etProfilePhone);

        tvProfileMessage = findViewById(R.id.tvProfileMessage);

        // Check logged-in user
        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        // Email is read-only
        etProfileEmail.setText(currentUser.getEmail());
        etProfileEmail.setEnabled(false);

        // Image picker
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {

                        savePhotoLocally(result.getData().getData());
                    }
                }
        );

        loadProfile();
        displaySavedPhoto();

        // Change profile photo
        btnChangePhoto.setOnClickListener(v -> openPhotoPicker());

        // Save profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Open My Vehicles screen
        btnMyVehicles.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ProfileActivity.this,
                    MyVehiclesActivity.class
            );

            startActivity(intent);
        });

        // Back button
        btnBackHome.setOnClickListener(v -> finish());
    }

    // Open gallery
    private void openPhotoPicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        photoPickerLauncher.launch(intent);
    }

    // Get local photo file
    private File getProfilePhotoFile() {

        if (currentUser == null) return null;

        return new File(
                getFilesDir(),
                "profile_" + currentUser.getUid() + ".jpg"
        );
    }

    // Save selected photo locally
    private void savePhotoLocally(Uri uri) {

        File destination = getProfilePhotoFile();

        if (destination == null) return;

        try (
                InputStream input =
                        getContentResolver().openInputStream(uri);

                OutputStream output =
                        new FileOutputStream(destination)
        ) {

            if (input == null) {
                throw new Exception("Unable to open selected image.");
            }

            byte[] buffer = new byte[8192];

            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();

            displaySavedPhoto();

            Toast.makeText(
                    this,
                    "Profile photo saved on this device.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Log.e(TAG, "Photo save failed", e);

            Toast.makeText(
                    this,
                    "Could not save photo: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Display locally saved photo
    private void displaySavedPhoto() {

        File file = getProfilePhotoFile();

        if (file == null || !file.exists()) {

            ivProfilePhoto.setImageResource(
                    android.R.drawable.ic_menu_camera
            );

            return;
        }

        BitmapFactory.Options bounds =
                new BitmapFactory.Options();

        bounds.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(
                file.getAbsolutePath(),
                bounds
        );

        BitmapFactory.Options options =
                new BitmapFactory.Options();

        options.inSampleSize =
                calculateSampleSize(bounds, 512, 512);

        Bitmap bitmap = BitmapFactory.decodeFile(
                file.getAbsolutePath(),
                options
        );

        if (bitmap != null) {

            ivProfilePhoto.setImageBitmap(bitmap);

        } else {

            ivProfilePhoto.setImageResource(
                    android.R.drawable.ic_menu_camera
            );
        }
    }

    // Calculate image size
    private int calculateSampleSize(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight
    ) {

        int height = options.outHeight;
        int width = options.outWidth;

        int sample = 1;

        if (height > reqHeight || width > reqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / sample) >= reqHeight
                    && (halfWidth / sample) >= reqWidth) {

                sample *= 2;
            }
        }

        return sample;
    }

    // Load profile from Firestore
    private void loadProfile() {

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        setLoading(true);

        tvProfileMessage.setText("Loading profile...");

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    setLoading(false);

                    if (!document.exists()) {

                        tvProfileMessage.setText(
                                "Profile document not found in users collection."
                        );

                        return;
                    }

                    String name = document.getString("name");
                    String phone = document.getString("phone");

                    etProfileName.setText(
                            name == null ? "" : name
                    );

                    etProfilePhone.setText(
                            phone == null ? "" : phone
                    );

                    etProfileEmail.setText(
                            currentUser.getEmail()
                    );

                    tvProfileMessage.setText("");
                })
                .addOnFailureListener(e -> {

                    setLoading(false);

                    tvProfileMessage.setText(
                            "Could not load profile. See Logcat."
                    );

                    Log.e(TAG, "Firestore read failed", e);

                    Toast.makeText(
                            this,
                            "Load failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Save profile details
    private void saveProfile() {

        String name = etProfileName.getText()
                .toString().trim();

        String email = etProfileEmail.getText()
                .toString().trim();

        String phone = etProfilePhone.getText()
                .toString().trim();

        // Name validation
        if (TextUtils.isEmpty(name) || name.length() < 2) {

            etProfileName.setError("Enter your full name");
            etProfileName.requestFocus();

            return;
        }

        // Email validation
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

            etProfileEmail.setError("Enter a valid email");
            etProfileEmail.requestFocus();

            return;
        }

        // Phone validation
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

        // Email cannot be changed here
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

        Map<String, Object> updates = new HashMap<>();

        updates.put("name", name);
        updates.put("phone", phone);

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // Update Firebase Authentication display name
                    UserProfileChangeRequest request =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    currentUser.updateProfile(request)
                            .addOnCompleteListener(task -> {

                                setLoading(false);

                                tvProfileMessage.setText("");

                                if (task.isSuccessful()) {

                                    Toast.makeText(
                                            this,
                                            "Profile updated successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                } else {

                                    Toast.makeText(
                                            this,
                                            "Details saved, but Auth display name could not be refreshed.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    Log.e(
                                            TAG,
                                            "Auth profile update failed",
                                            task.getException()
                                    );
                                }
                            });
                })
                .addOnFailureListener(e -> {

                    setLoading(false);

                    tvProfileMessage.setText(
                            "Profile update failed. Check Logcat."
                    );

                    Log.e(TAG, "Firestore update failed", e);

                    Toast.makeText(
                            this,
                            "Update failed: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Loading state
    private void setLoading(boolean loading) {

        btnSaveProfile.setEnabled(!loading);

        btnSaveProfile.setText(
                loading ? "Please wait..." : "Save Changes"
        );
    }

    // Redirect to login
    private void openLoginScreen() {

        Intent intent = new Intent(
                this,
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

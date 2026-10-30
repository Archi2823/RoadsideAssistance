
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome);

        Button btnBatteryHelp =
                findViewById(R.id.btnBatteryHelp);

        Button btnTowingHelp =
                findViewById(R.id.btnTowingHelp);

        Button btnGarageTowingMap =
                findViewById(R.id.btnGarageTowingMap);

        Button btnMyRequests =
                findViewById(R.id.btnMyRequests);

        Button btnChangePassword =
                findViewById(R.id.btnChangePassword);

        Button btnLogout =
                findViewById(R.id.btnLogout);

        Button btnAvailableMechanics =
                findViewById(R.id.btnAvailableMechanics);

        ImageButton btnProfile =
                findViewById(R.id.btnProfile);

        // Check logged-in user
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLoginScreen();
            return;
        }

        // Load customer name from Firestore
        loadCustomerDetails(user);

        // Open Profile screen
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    ProfileActivity.class
            ));
        });

        // Open Battery Help form
        btnBatteryHelp.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    BatteryHelpActivity.class
            ));
        });

        // Open Towing Help form
        btnTowingHelp.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    TowingHelpActivity.class
            ));
        });

        // Open Garage/Towing Map
        btnGarageTowingMap.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    GarageTowingMapActivity.class
            ));
        });

        // Open My Requests screen
        btnMyRequests.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    MyRequestsActivity.class
            ));
        });

        // Open Available Mechanics screen
        btnAvailableMechanics.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    AvailableMechanicsActivity.class
            ));
        });

        // Open Change Password screen
        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    ChangePasswordActivity.class
            ));
        });

        // Logout
        btnLogout.setOnClickListener(v -> {

            auth.signOut();

            Toast.makeText(
                    HomeActivity.this,
                    "Logged out successfully",
                    Toast.LENGTH_SHORT
            ).show();

            openLoginScreen();
        });
    }

    // Fetch customer details from Firestore
    private void loadCustomerDetails(FirebaseUser user) {

        String uid = user.getUid();

        tvWelcome.setText("Welcome to Road Side Assistance!");

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (document.exists()) {

                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            tvWelcome.setText("Welcome, " + name + "!");
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) return;

                    Toast.makeText(
                            HomeActivity.this,
                            "Unable to load profile details",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // Redirect to Login screen
    private void openLoginScreen() {

        Intent intent = new Intent(
                HomeActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}

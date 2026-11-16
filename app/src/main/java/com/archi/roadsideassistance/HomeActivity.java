
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvWelcome;

    // Usage statistics views
    private TextView tvTotalRequests;
    private TextView tvBatteryRequests;
    private TextView tvTowingRequests;
    private TextView tvLastUsed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Welcome message
        tvWelcome = findViewById(R.id.tvWelcome);

        // Usage statistics
        tvTotalRequests = findViewById(R.id.tvTotalRequests);
        tvBatteryRequests = findViewById(R.id.tvBatteryRequests);
        tvTowingRequests = findViewById(R.id.tvTowingRequests);
        tvLastUsed = findViewById(R.id.tvLastUsed);

        // Buttons
        Button btnBatteryHelp = findViewById(R.id.btnBatteryHelp);
        Button btnTowingHelp = findViewById(R.id.btnTowingHelp);
        Button btnGarageTowingMap = findViewById(R.id.btnGarageTowingMap);
        Button btnMyRequests = findViewById(R.id.btnMyRequests);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnAvailableMechanics = findViewById(R.id.btnAvailableMechanics);

        ImageButton btnProfile = findViewById(R.id.btnProfile);

        // Check login status
        if (auth.getCurrentUser() == null) {
            openLoginScreen();
            return;
        }

        // Profile navigation
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Battery assistance
        btnBatteryHelp.setOnClickListener(v ->
                startActivity(new Intent(this, BatteryHelpActivity.class)));

        // Towing assistance
        btnTowingHelp.setOnClickListener(v ->
                startActivity(new Intent(this, TowingHelpActivity.class)));

        // Nearby garages and towing map
        btnGarageTowingMap.setOnClickListener(v ->
                startActivity(new Intent(this, GarageTowingMapActivity.class)));

        // My service requests
        btnMyRequests.setOnClickListener(v ->
                startActivity(new Intent(this, MyRequestsActivity.class)));

        // Available mechanics
        btnAvailableMechanics.setOnClickListener(v ->
                startActivity(new Intent(this, AvailableMechanicsActivity.class)));

        // Change password
        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

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

    @Override
    protected void onResume() {
        super.onResume();

        if (tvWelcome == null) {
            return;
        }

        FirebaseUser user = auth == null
                ? null
                : auth.getCurrentUser();

        if (user == null) {
            openLoginScreen();
            return;
        }

        // Refresh customer details
        loadCustomerDetails(user);

        // Refresh usage statistics
        loadUsageStatistics(user);
    }

    // Load customer name from Firestore
    private void loadCustomerDetails(FirebaseUser user) {

        tvWelcome.setText("Welcome!");

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (document.exists()) {

                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            tvWelcome.setText("Welcome, " + name + "!");
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    if (!isFinishing() && !isDestroyed()) {

                        Toast.makeText(
                                HomeActivity.this,
                                "Unable to load profile details",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    // Load usage statistics from Firestore
    private void loadUsageStatistics(FirebaseUser user) {

        tvTotalRequests.setText("Loading...");
        tvBatteryRequests.setText("Loading...");
        tvTowingRequests.setText("Loading...");
        tvLastUsed.setText("Loading...");

        db.collection("serviceRequests")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    int totalRequests = snapshot.size();
                    int batteryRequests = 0;
                    int towingRequests = 0;

                    Date latestDate = null;

                    for (QueryDocumentSnapshot document : snapshot) {

                        // Get service type
                        String serviceType =
                                document.getString("serviceType");

                        if ("Battery Help".equalsIgnoreCase(serviceType)) {
                            batteryRequests++;
                        }

                        if ("Towing Help".equalsIgnoreCase(serviceType)) {
                            towingRequests++;
                        }

                        // Read createdAt safely
                        Object createdAt = document.get("createdAt");

                        Date requestDate = null;

                        if (createdAt instanceof Timestamp) {

                            requestDate =
                                    ((Timestamp) createdAt).toDate();

                        } else if (createdAt instanceof Date) {

                            requestDate = (Date) createdAt;

                        } else if (createdAt instanceof Number) {

                            requestDate = new Date(
                                    ((Number) createdAt).longValue()
                            );
                        }

                        // Find latest request date
                        if (requestDate != null) {

                            if (latestDate == null ||
                                    requestDate.after(latestDate)) {

                                latestDate = requestDate;
                            }
                        }
                    }

                    // Display total requests
                    tvTotalRequests.setText(
                            String.valueOf(totalRequests)
                    );

                    // Display battery requests
                    tvBatteryRequests.setText(
                            String.valueOf(batteryRequests)
                    );

                    // Display towing requests
                    tvTowingRequests.setText(
                            String.valueOf(towingRequests)
                    );

                    // Display last request date
                    if (latestDate != null) {

                        SimpleDateFormat dateFormat =
                                new SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault()
                                );

                        tvLastUsed.setText(
                                dateFormat.format(latestDate)
                        );

                    } else {

                        tvLastUsed.setText("No requests yet");
                    }
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    tvTotalRequests.setText("--");
                    tvBatteryRequests.setText("--");
                    tvTowingRequests.setText("--");
                    tvLastUsed.setText("Unable to load");

                    Toast.makeText(
                            HomeActivity.this,
                            "Unable to load usage statistics: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Open login screen
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

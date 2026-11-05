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
        tvWelcome = findViewById(R.id.tvWelcome);

        Button btnBatteryHelp = findViewById(R.id.btnBatteryHelp);
        Button btnTowingHelp = findViewById(R.id.btnTowingHelp);
        Button btnGarageTowingMap = findViewById(R.id.btnGarageTowingMap);
        Button btnMyRequests = findViewById(R.id.btnMyRequests);
        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnAvailableMechanics = findViewById(R.id.btnAvailableMechanics);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        if (auth.getCurrentUser() == null) {
            openLoginScreen();
            return;
        }

        btnProfile.setOnClickListener(v -> startActivity(
                new Intent(this, ProfileActivity.class)));
        btnBatteryHelp.setOnClickListener(v -> startActivity(
                new Intent(this, BatteryHelpActivity.class)));
        btnTowingHelp.setOnClickListener(v -> startActivity(
                new Intent(this, TowingHelpActivity.class)));
        btnGarageTowingMap.setOnClickListener(v -> startActivity(
                new Intent(this, GarageTowingMapActivity.class)));
        btnMyRequests.setOnClickListener(v -> startActivity(
                new Intent(this, MyRequestsActivity.class)));
        btnAvailableMechanics.setOnClickListener(v -> startActivity(
                new Intent(this, AvailableMechanicsActivity.class)));
        btnChangePassword.setOnClickListener(v -> startActivity(
                new Intent(this, ChangePasswordActivity.class)));

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            openLoginScreen();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvWelcome == null) return;
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            openLoginScreen();
            return;
        }
        loadCustomerDetails(user);
    }

    private void loadCustomerDetails(FirebaseUser user) {
        tvWelcome.setText("Welcome to Road Side Assistance!");
        db.collection("users").document(user.getUid()).get()
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
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this, "Unable to load profile details",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

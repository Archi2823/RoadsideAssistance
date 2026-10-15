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

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnChangePassword =
                findViewById(R.id.btnChangePassword);
        Button btnLogout = findViewById(R.id.btnLogout);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLoginScreen();
            return;
        }

        String name = user.getDisplayName();

        if (name != null && !name.trim().isEmpty()) {
            tvWelcome.setText("Welcome, " + name + "!");
        } else {
            tvWelcome.setText("Welcome to Road Side Assistance!");
        }

        // Open Profile screen from the top-right icon
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(
                    HomeActivity.this,
                    ProfileActivity.class
            ));
        });

        // Change Password
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

    private void openLoginScreen() {
        Intent intent = new Intent(
                HomeActivity.this,
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

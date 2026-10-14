
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgotPassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance();

        // Connect XML views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Login button
        btnLogin.setOnClickListener(v -> loginUser());

        // Open signup screen
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    activity_signup.class
            );
            startActivity(intent);
        });

        // Open Reset Password screen
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ResetPasswordActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginUser() {

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

        // Prevent multiple login requests
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");

        // Verify credentials with Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");

                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // Open HomeActivity
                            Intent intent = new Intent(
                                    MainActivity.this,
                                    HomeActivity.class
                            );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();

                        } else {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to verify user. Please try again.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    } else {

                        Toast.makeText(
                                MainActivity.this,
                                "Login failed. Check your email and password.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

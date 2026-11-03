
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;

    private FirebaseAuth auth;
    private boolean resetInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etResetEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {

        if (resetInProgress) {
            return;
        }

        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your registered email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        resetInProgress = true;

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Sending email...");

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    resetInProgress = false;

                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText("Send Reset Link");

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                "If an account exists for this email, a password reset link has been sent. Please check your inbox and spam folder.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();

                    } else {

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                "Unable to send reset email. Please check your internet connection and try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

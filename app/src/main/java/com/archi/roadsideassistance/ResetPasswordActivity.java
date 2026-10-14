
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail, etOldPassword;
    private EditText etNewPassword, etConfirmPassword;
    private Button btnResetPassword;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etResetEmail);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(email)
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid registered email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(oldPassword)) {
            etOldPassword.setError("Enter your old password");
            etOldPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError(
                    "Password must be at least 6 characters"
            );
            etNewPassword.requestFocus();
            return;
        }

        if (newPassword.equals(oldPassword)) {
            etNewPassword.setError(
                    "New password must be different from old password"
            );
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Verifying...");

        // Verify the registered email and old password
        auth.signInWithEmailAndPassword(email, oldPassword)
                .addOnCompleteListener(this, loginTask -> {

                    if (!loginTask.isSuccessful()) {
                        btnResetPassword.setEnabled(true);
                        btnResetPassword.setText("Reset Password");

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                "Incorrect email or old password.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        btnResetPassword.setEnabled(true);
                        btnResetPassword.setText("Reset Password");

                        Toast.makeText(
                                ResetPasswordActivity.this,
                                "Unable to verify your account.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    btnResetPassword.setText("Updating...");

                    // Update the password after successful verification
                    user.updatePassword(newPassword)
                            .addOnCompleteListener(updateTask -> {

                                btnResetPassword.setEnabled(true);
                                btnResetPassword.setText("Reset Password");

                                if (updateTask.isSuccessful()) {

                                    // Return to login so the new password
                                    // can be used for the next login.
                                    auth.signOut();

                                    Toast.makeText(
                                            ResetPasswordActivity.this,
                                            "Password reset successful. Please log in with your new password.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    finish();

                                } else {
                                    Toast.makeText(
                                            ResetPasswordActivity.this,
                                            "Password update failed. Please try again.",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                });
    }
}

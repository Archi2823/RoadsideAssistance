
package com.archi.roadsideassistance;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private Button btnUpdatePassword;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseAuth.getInstance();

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        btnUpdatePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPassword =
                etNewPassword.getText().toString();

        String confirmPassword =
                etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            etNewPassword.setError(
                    "Password must be at least 6 characters"
            );
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again to change your password.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        user.updatePassword(newPassword)
                .addOnCompleteListener(task -> {
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText("Update Password");

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                ChangePasswordActivity.this,
                                "Password changed successfully!",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    } else {
                        Toast.makeText(
                                ChangePasswordActivity.this,
                                "Password update failed. Please log in again if your session has expired.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}

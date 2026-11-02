
package com.archi.roadsideassistance;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MechanicProfileActivity extends AppCompatActivity {

    private TextView tvMechanicName;
    private TextView tvMechanicEmail;
    private TextView tvMechanicPhone;
    private TextView tvMechanicSpecialization;
    private TextView tvMechanicAvailability;

    private Button btnCallMechanic;

    private String mechanicPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_profile);

        tvMechanicName = findViewById(R.id.tvMechanicName);
        tvMechanicEmail = findViewById(R.id.tvMechanicEmail);
        tvMechanicPhone = findViewById(R.id.tvMechanicPhone);
        tvMechanicSpecialization =
                findViewById(R.id.tvMechanicSpecialization);
        tvMechanicAvailability =
                findViewById(R.id.tvMechanicAvailability);

        btnCallMechanic = findViewById(R.id.btnCallMechanic);

        // Receive mechanic details
        Intent intent = getIntent();

        String name = intent.getStringExtra("mechanicName");
        String email = intent.getStringExtra("mechanicEmail");
        mechanicPhone = intent.getStringExtra("mechanicPhone");
        String specialization =
                intent.getStringExtra("mechanicSpecialization");
        String availability =
                intent.getStringExtra("mechanicAvailability");

        tvMechanicName.setText(valueOrDefault(name, "Mechanic"));
        tvMechanicEmail.setText(valueOrDefault(email, "Not provided"));
        tvMechanicPhone.setText(
                valueOrDefault(mechanicPhone, "Not provided")
        );
        tvMechanicSpecialization.setText(
                valueOrDefault(specialization, "Not specified")
        );
        tvMechanicAvailability.setText(
                valueOrDefault(availability, "Not specified")
        );

        // Availability color
        if ("available".equalsIgnoreCase(availability)) {
            tvMechanicAvailability.setTextColor(0xFF16865B);
        } else if ("busy".equalsIgnoreCase(availability)) {
            tvMechanicAvailability.setTextColor(0xFFD64545);
        } else {
            tvMechanicAvailability.setTextColor(0xFF64748B);
        }

        // Call mechanic
        btnCallMechanic.setOnClickListener(v -> {

            if (mechanicPhone == null
                    || mechanicPhone.trim().isEmpty()
                    || mechanicPhone.equalsIgnoreCase("Not provided")) {

                Toast.makeText(
                        this,
                        "Mechanic phone number is not available",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Intent dialIntent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + Uri.encode(mechanicPhone))
            );

            startActivity(dialIntent);
        });
    }

    private String valueOrDefault(String value, String defaultValue) {

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }
}

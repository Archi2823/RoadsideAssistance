
package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AvailableMechanicsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private LinearLayout mechanicsContainer;
    private ProgressBar progressBar;
    private TextView tvMessage;

    private ListenerRegistration mechanicsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_mechanics);

        db = FirebaseFirestore.getInstance();

        mechanicsContainer = findViewById(R.id.mechanicsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);

        loadAvailableMechanics();
    }

    // Load approved workers from the existing users collection
    private void loadAvailableMechanics() {

        progressBar.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);

        mechanicsListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    progressBar.setVisibility(View.GONE);
                    mechanicsContainer.removeAllViews();

                    if (error != null) {

                        tvMessage.setText(
                                "Unable to load mechanics. Please try again."
                        );
                        tvMessage.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                AvailableMechanicsActivity.this,
                                error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {

                        tvMessage.setText(
                                "No approved mechanics found currently."
                        );
                        tvMessage.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvMessage.setVisibility(View.GONE);

                    for (DocumentSnapshot document : snapshots.getDocuments()) {

                        String uid = document.getId();

                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String specialization =
                                document.getString("specialization");
                        String availability =
                                document.getString("availability");

                        name = getValue(name, "Mechanic");
                        email = getValue(email, "Not provided");
                        phone = getValue(phone, "Not provided");
                        specialization =
                                getValue(specialization, "Not specified");
                        availability =
                                getValue(availability, "Not specified");

                        addMechanicCard(
                                uid,
                                name,
                                email,
                                phone,
                                specialization,
                                availability
                        );
                    }
                });
    }

    private String getValue(String value, String defaultValue) {

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }

    // Create mechanic card
    private void addMechanicCard(
            String uid,
            String name,
            String email,
            String phone,
            String specialization,
            String availability) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(14));
        cardBackground.setStroke(dp(1), Color.rgb(225, 232, 240));

        card.setBackground(cardBackground);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        // Mechanic name
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(19);
        tvName.setTextColor(Color.rgb(23, 43, 77));
        tvName.setTypeface(null, Typeface.BOLD);

        card.addView(tvName);

        // Specialization
        TextView tvSpecialization = new TextView(this);
        tvSpecialization.setText(
                "Specialization: " + specialization
        );
        tvSpecialization.setTextSize(14);
        tvSpecialization.setTextColor(Color.DKGRAY);
        tvSpecialization.setPadding(0, dp(10), 0, 0);

        card.addView(tvSpecialization);

        // Phone
        TextView tvPhone = new TextView(this);
        tvPhone.setText("Phone: " + phone);
        tvPhone.setTextSize(14);
        tvPhone.setTextColor(Color.DKGRAY);
        tvPhone.setPadding(0, dp(8), 0, 0);

        card.addView(tvPhone);

        // Availability
        TextView tvAvailability = new TextView(this);
        tvAvailability.setText("Availability: " + availability);
        tvAvailability.setTextSize(14);
        tvAvailability.setTypeface(null, Typeface.BOLD);
        tvAvailability.setPadding(0, dp(8), 0, dp(16));

        if (availability.equalsIgnoreCase("available")) {
            tvAvailability.setTextColor(Color.rgb(22, 134, 91));
        } else if (availability.equalsIgnoreCase("busy")) {
            tvAvailability.setTextColor(Color.rgb(214, 69, 69));
        } else {
            tvAvailability.setTextColor(Color.GRAY);
        }

        card.addView(tvAvailability);

        // Buttons row
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_VERTICAL);

        // View Profile button
        TextView btnProfile = createActionButton(
                "View Profile",
                Color.rgb(23, 105, 170)
        );

        LinearLayout.LayoutParams profileParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1
                );

        profileParams.setMarginEnd(dp(8));
        buttonRow.addView(btnProfile, profileParams);

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    AvailableMechanicsActivity.this,
                    MechanicProfileActivity.class
            );

            intent.putExtra("mechanicUid", uid);
            intent.putExtra("mechanicName", name);
            intent.putExtra("mechanicEmail", email);
            intent.putExtra("mechanicPhone", phone);
            intent.putExtra("mechanicSpecialization", specialization);
            intent.putExtra("mechanicAvailability", availability);

            startActivity(intent);
        });

        // Call Directly button
        TextView btnCall = createActionButton(
                "Call Directly",
                Color.rgb(22, 134, 91)
        );

        LinearLayout.LayoutParams callParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(46),
                        1
                );

        callParams.setMarginStart(dp(8));
        buttonRow.addView(btnCall, callParams);

        if (phone.equals("Not provided")) {

            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);

        } else {

            btnCall.setOnClickListener(v -> {

                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + Uri.encode(phone))
                );

                startActivity(intent);
            });
        }

        card.addView(buttonRow);

        mechanicsContainer.addView(card);
    }

    // Create styled action button
    private TextView createActionButton(String text, int color) {

        TextView button = new TextView(this);

        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(8));

        button.setBackground(background);
        button.setPadding(dp(5), 0, dp(5), 0);

        return button;
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {

        if (mechanicsListener != null) {
            mechanicsListener.remove();
        }

        super.onDestroy();
    }
}

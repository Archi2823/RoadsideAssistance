package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MyRequestsActivity extends AppCompatActivity {

    private LinearLayout container;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        ScrollView scroll = new ScrollView(this);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);

        scroll.addView(container);
        setContentView(scroll);

        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (container != null && uid != null) {
            loadRequests();
        }
    }

    // =====================================
    // LOAD MY REQUESTS
    // =====================================
    private void loadRequests() {

        container.removeAllViews();

        TextView heading = new TextView(this);
        heading.setText("My Roadside Requests");
        heading.setTextSize(24);
        heading.setPadding(0, 0, 0, 24);
        container.addView(heading);

        db.collection("serviceRequests")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (snapshot.isEmpty()) {
                        addText("No requests found.");
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        showRequest(doc);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load requests: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    // =====================================
    // DISPLAY REQUEST AND ACTION BUTTONS
    // =====================================
    private void showRequest(DocumentSnapshot doc) {

        String service = doc.getString("serviceType");
        String vehicle = doc.getString("vehicleType");
        String issue = doc.getString("batteryIssue");
        String location = doc.getString("location");
        String status = doc.getString("status");

        boolean assigned = Boolean.TRUE.equals(
                doc.getBoolean("technicianAssigned")
        );

        boolean editable = "pending".equalsIgnoreCase(status)
                && !assigned;

        boolean resolved = "resolved".equalsIgnoreCase(status);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = 24;
        card.setLayoutParams(cardParams);

        TextView details = new TextView(this);

        details.setText(
                "Service: " + safe(service) +
                        "\nVehicle: " + safe(vehicle) +
                        "\nIssue: " + safe(issue) +
                        "\nLocation: " + safe(location) +
                        "\nStatus: " + safe(status) +
                        "\nTechnician assigned: "
                        + (assigned ? "Yes" : "No")
        );

        details.setTextSize(16);
        card.addView(details);

        // Pending and unassigned requests can be edited or
        if (editable) {

            Button edit = new Button(this);
            edit.setText("Edit Request");
            card.addView(edit);

            edit.setOnClickListener(v -> editRequest(doc));

            Button cancel = new Button(this);
            cancel.setText("Cancel Request");
            card.addView(cancel);

            cancel.setOnClickListener(v -> confirmCancel(doc));

        } else if (resolved) {

            // Show feedback action only for resolved requests
            checkFeedbackStatus(doc, card);

        } else {

            TextView note = new TextView(this);
            note.setText(
                    "Editing/cancellation is unavailable for this request."
            );
            note.setPadding(0, 8, 0, 0);
            card.addView(note);
        }

        container.addView(card);
    }

    // =====================================
    // CHECK WHETHER FEEDBACK ALREADY EXISTS
    // =====================================
    private void checkFeedbackStatus(
            DocumentSnapshot requestDoc,
            LinearLayout card
    ) {

        String requestId = requestDoc.getId();

        db.collection("serviceFeedback")
                .document(requestId)
                .get()
                .addOnSuccessListener(feedbackDoc -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Button feedbackButton = new Button(this);

                    if (feedbackDoc.exists()) {

                        feedbackButton.setText("Feedback Submitted");
                        feedbackButton.setEnabled(false);

                    } else {

                        feedbackButton.setText("Give Feedback");

                        feedbackButton.setOnClickListener(v -> {

                            Intent intent = new Intent(
                                    MyRequestsActivity.this,
                                    FeedbackActivity.class
                            );

                            intent.putExtra("requestId", requestId);
                            startActivity(intent);
                        });
                    }

                    card.addView(feedbackButton);
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    TextView message = new TextView(this);
                    message.setText(
                            "Unable to check feedback status. Please try again."
                    );
                    message.setPadding(0, 8, 0, 0);
                    card.addView(message);

                    Toast.makeText(
                            MyRequestsActivity.this,
                            "Feedback check failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =====================================
    // EDIT REQUEST
    // =====================================
    private void editRequest(DocumentSnapshot doc) {

        String status = doc.getString("status");

        boolean assigned = Boolean.TRUE.equals(
                doc.getBoolean("technicianAssigned")
        );

        if (!"pending".equalsIgnoreCase(status) || assigned) {

            Toast.makeText(
                    this,
                    "This request can no longer be edited.",
                    Toast.LENGTH_SHORT
            ).show();

            loadRequests();
            return;
        }

        EditText locationInput = new EditText(this);
        locationInput.setHint("Location");
        locationInput.setText(doc.getString("location"));

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Additional details");
        descriptionInput.setText(doc.getString("description"));

        Spinner vehicleSpinner = new Spinner(this);

        String[] vehicles = {
                "Bike", "Car", "SUV", "Truck", "Other"
        };

        vehicleSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        vehicles
                )
        );

        String currentVehicle = doc.getString("vehicleType");

        for (int i = 0; i < vehicles.length; i++) {
            if (vehicles[i].equals(currentVehicle)) {
                vehicleSpinner.setSelection(i);
                break;
            }
        }

        Spinner issueSpinner = new Spinner(this);

        String[] issues = {
                "Battery dead",
                "Vehicle not starting",
                "Jump-start required",
                "Battery replacement required",
                "Other"
        };

        issueSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        issues
                )
        );

        String currentIssue = doc.getString("batteryIssue");

        for (int i = 0; i < issues.length; i++) {
            if (issues[i].equals(currentIssue)) {
                issueSpinner.setSelection(i);
                break;
            }
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(24, 8, 24, 0);

        form.addView(vehicleSpinner);
        form.addView(issueSpinner);
        form.addView(locationInput);
        form.addView(descriptionInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Battery Request")
                .setView(form)
                .setNegativeButton("Back", null)
                .setPositiveButton("Save", (dialog, which) -> {

                    String location = locationInput.getText()
                            .toString()
                            .trim();

                    if (location.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Location is required",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();

                    updates.put(
                            "vehicleType",
                            vehicleSpinner.getSelectedItem().toString()
                    );

                    updates.put(
                            "batteryIssue",
                            issueSpinner.getSelectedItem().toString()
                    );

                    updates.put("location", location);

                    updates.put(
                            "description",
                            descriptionInput.getText().toString().trim()
                    );

                    db.collection("serviceRequests")
                            .document(doc.getId())
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Request updated",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadRequests();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .show();
    }

    // =====================================
    // CANCEL REQUEST
    // =====================================
    private void confirmCancel(DocumentSnapshot doc) {

        new AlertDialog.Builder(this)
                .setTitle("Cancel Request?")
                .setMessage(
                        "Are you sure you want to cancel this request?"
                )
                .setNegativeButton("No", null)
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {

                    String status = doc.getString("status");

                    boolean assigned = Boolean.TRUE.equals(
                            doc.getBoolean("technicianAssigned")
                    );

                    if (!"pending".equalsIgnoreCase(status) || assigned) {

                        Toast.makeText(
                                this,
                                "This request can no longer be cancelled.",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadRequests();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "cancelled");

                    db.collection("serviceRequests")
                            .document(doc.getId())
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(
                                        this,
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadRequests();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Cancellation failed: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .show();
    }

    // =====================================
    // HELPER METHODS
    // =====================================
    private void addText(String message) {

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);

        container.addView(text);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }
}

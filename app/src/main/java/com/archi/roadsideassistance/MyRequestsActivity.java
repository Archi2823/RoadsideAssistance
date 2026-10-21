package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyRequestsActivity extends AppCompatActivity {

    private LinearLayout container;
    private LinearLayout requestsContainer;
    private LinearLayout filterContainer;

    private FirebaseFirestore db;
    private String uid;

    private final List<DocumentSnapshot> allRequests = new ArrayList<>();
    private String selectedFilter = "All";

    private final String[] filters = {
            "All", "Pending", "In Progress", "Resolved", "Cancelled"
    };

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
        container.setPadding(dp(20), dp(20), dp(20), dp(20));

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
    // LOAD REQUESTS FROM FIRESTORE
    // =====================================
    private void loadRequests() {

        container.removeAllViews();
        allRequests.clear();

        TextView heading = new TextView(this);
        heading.setText("My Request History");
        heading.setTextSize(25);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.rgb(28, 59, 104));
        heading.setPadding(0, 0, 0, dp(8));
        container.addView(heading);

        TextView subtitle = new TextView(this);
        subtitle.setText("View and track all your roadside requests");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, 0, 0, dp(18));
        container.addView(subtitle);

        TextView filterHeading = new TextView(this);
        filterHeading.setText("Filter by status");
        filterHeading.setTextSize(16);
        filterHeading.setTypeface(null, Typeface.BOLD);
        filterHeading.setPadding(0, 0, 0, dp(10));
        container.addView(filterHeading);

        HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
        horizontalScroll.setHorizontalScrollBarEnabled(false);

        filterContainer = new LinearLayout(this);
        filterContainer.setOrientation(LinearLayout.HORIZONTAL);

        horizontalScroll.addView(filterContainer);
        container.addView(horizontalScroll);

        requestsContainer = new LinearLayout(this);
        requestsContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams requestsParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        requestsParams.topMargin = dp(18);
        container.addView(requestsContainer, requestsParams);

        createFilterButtons();
        showLoading();

        db.collection("serviceRequests")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    allRequests.clear();
                    allRequests.addAll(snapshot.getDocuments());

                    // Sort newest first locally.
                    Collections.sort(allRequests, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(
                                DocumentSnapshot first,
                                DocumentSnapshot second
                        ) {
                            Timestamp firstTime =
                                    first.getTimestamp("createdAt");

                            Timestamp secondTime =
                                    second.getTimestamp("createdAt");

                            if (firstTime == null && secondTime == null) {
                                return 0;
                            }

                            if (firstTime == null) {
                                return 1;
                            }

                            if (secondTime == null) {
                                return -1;
                            }

                            return secondTime.compareTo(firstTime);
                        }
                    });

                    updateFilterButtons();
                    displayFilteredRequests();
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    requestsContainer.removeAllViews();
                    addTextTo(
                            requestsContainer,
                            "Could not load requests: " + e.getMessage()
                    );
                });
    }

    // =====================================
    // CREATE STATUS FILTER BUTTONS
    // =====================================
    private void createFilterButtons() {

        filterContainer.removeAllViews();

        for (String filter : filters) {

            Button button = new Button(this);
            button.setText(filter);
            button.setTextSize(12);
            button.setAllCaps(false);
            button.setPadding(dp(12), 0, dp(12), 0);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(44)
                    );

            params.rightMargin = dp(8);
            filterContainer.addView(button, params);

            button.setOnClickListener(v -> {

                selectedFilter = filter;

                updateFilterButtons();
                displayFilteredRequests();
            });
        }

        updateFilterButtons();
    }

    private void updateFilterButtons() {

        if (filterContainer == null) {
            return;
        }

        for (int i = 0; i < filterContainer.getChildCount(); i++) {

            View view = filterContainer.getChildAt(i);

            if (!(view instanceof Button)) {
                continue;
            }

            Button button = (Button) view;
            String filter = button.getText().toString();

            boolean selected = filter.equals(selectedFilter);

            button.setTextColor(
                    selected ? Color.WHITE : Color.rgb(35, 75, 130)
            );

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(22));

            background.setColor(
                    selected
                            ? Color.rgb(35, 86, 190)
                            : Color.rgb(235, 240, 248)
            );

            button.setBackground(background);
        }
    }

    // =====================================
    // DISPLAY FILTERED REQUESTS
    // =====================================
    private void displayFilteredRequests() {

        if (requestsContainer == null) {
            return;
        }

        requestsContainer.removeAllViews();

        List<DocumentSnapshot> filteredRequests = new ArrayList<>();

        for (DocumentSnapshot doc : allRequests) {

            String status = doc.getString("status");

            if ("All".equals(selectedFilter)) {
                filteredRequests.add(doc);
            } else if (matchesFilter(status, selectedFilter)) {
                filteredRequests.add(doc);
            }
        }

        TextView count = new TextView(this);
        count.setText(
                selectedFilter + " Requests (" + filteredRequests.size() + ")"
        );
        count.setTextSize(17);
        count.setTypeface(null, Typeface.BOLD);
        count.setTextColor(Color.rgb(35, 55, 85));
        count.setPadding(0, 0, 0, dp(12));

        requestsContainer.addView(count);

        if (filteredRequests.isEmpty()) {

            TextView empty = new TextView(this);
            empty.setText(
                    "No " +
                            ("All".equals(selectedFilter)
                                    ? ""
                                    : selectedFilter.toLowerCase() + " ") +
                            "requests found."
            );

            empty.setTextSize(15);
            empty.setTextColor(Color.DKGRAY);
            empty.setPadding(0, dp(20), 0, dp(20));

            requestsContainer.addView(empty);
            return;
        }

        for (DocumentSnapshot doc : filteredRequests) {
            showRequest(doc);
        }
    }

    private boolean matchesFilter(String status, String filter) {

        if (status == null) {
            return false;
        }

        String normalized = status.trim()
                .toLowerCase()
                .replace("_", " ")
                .replace("-", " ");

        switch (filter) {

            case "Pending":
                return normalized.equals("pending");

            case "In Progress":
                return normalized.equals("in progress")
                        || normalized.equals("inprogress");

            case "Resolved":
                return normalized.equals("resolved")
                        || normalized.equals("completed");

            case "Cancelled":
                return normalized.equals("cancelled")
                        || normalized.equals("canceled");

            default:
                return true;
        }
    }

    private void showLoading() {
        requestsContainer.removeAllViews();

        TextView loading = new TextView(this);
        loading.setText("Loading your requests...");
        loading.setTextSize(15);

        requestsContainer.addView(loading);
    }

    // =====================================
    // DISPLAY REQUEST CARD
    // =====================================
    private void showRequest(DocumentSnapshot doc) {

        String service = doc.getString("serviceType");
        String vehicle = doc.getString("vehicleType");
        String issue = doc.getString("batteryIssue");
        String location = doc.getString("location");
        String description = doc.getString("description");
        String status = doc.getString("status");

        boolean assigned = Boolean.TRUE.equals(
                doc.getBoolean("technicianAssigned")
        );

        boolean editable = "pending".equalsIgnoreCase(status)
                && !assigned;

        boolean resolved = "resolved".equalsIgnoreCase(status);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(14));
        cardBackground.setStroke(dp(1), Color.rgb(220, 227, 238));
        card.setBackground(cardBackground);
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(16);
        requestsContainer.addView(card, cardParams);

        TextView serviceTitle = new TextView(this);
        serviceTitle.setText(safe(service));
        serviceTitle.setTextSize(18);
        serviceTitle.setTypeface(null, Typeface.BOLD);
        serviceTitle.setTextColor(Color.rgb(28, 59, 104));

        card.addView(serviceTitle);

        TextView statusView = new TextView(this);
        statusView.setText("Status: " + displayStatus(status));
        statusView.setTextSize(14);
        statusView.setTypeface(null, Typeface.BOLD);
        statusView.setTextColor(statusColor(status));
        statusView.setPadding(0, dp(8), 0, dp(10));

        card.addView(statusView);

        TextView details = new TextView(this);
        details.setText(
                "Request ID: " + doc.getId() +
                        "\nVehicle: " + safe(vehicle) +
                        "\nIssue: " + safe(issue) +
                        "\nLocation: " + safe(location) +
                        "\nDescription: " + safe(description) +
                        "\nTechnician assigned: " +
                        (assigned ? "Yes" : "No")
        );

        details.setTextSize(14);
        details.setTextColor(Color.rgb(55, 65, 80));
        details.setLineSpacing(dp(3), 1.0f);

        card.addView(details);

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

            checkFeedbackStatus(doc, card);

        } else {

            TextView note = new TextView(this);
            note.setText(
                    "Editing/cancellation is unavailable for this request."
            );
            note.setTextSize(12);
            note.setPadding(0, dp(8), 0, 0);
            card.addView(note);
        }
    }

    private int statusColor(String status) {

        if (status == null) {
            return Color.DKGRAY;
        }

        switch (status.trim().toLowerCase()) {

            case "pending":
                return Color.rgb(190, 115, 0);

            case "in progress":
            case "in_progress":
                return Color.rgb(35, 86, 190);

            case "resolved":
            case "completed":
                return Color.rgb(30, 130, 75);

            case "cancelled":
            case "canceled":
                return Color.rgb(190, 45, 45);

            default:
                return Color.DKGRAY;
        }
    }

    private String displayStatus(String status) {

        if (status == null || status.trim().isEmpty()) {
            return "Unknown";
        }

        String normalized = status.trim()
                .toLowerCase()
                .replace("_", " ")
                .replace("-", " ");

        if (normalized.equals("in progress")) {
            return "In Progress";
        }

        if (normalized.equals("resolved")) {
            return "Resolved";
        }

        if (normalized.equals("pending")) {
            return "Pending";
        }

        if (normalized.equals("cancelled")
                || normalized.equals("canceled")) {
            return "Cancelled";
        }

        return status;
    }

    // =====================================
    // CHECK FEEDBACK STATUS
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
                    message.setPadding(0, dp(8), 0, 0);
                    card.addView(message);
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
        form.setPadding(dp(24), dp(8), dp(24), 0);

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
    // HELPERS
    // =====================================
    private void addTextTo(LinearLayout parent, String message) {

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(15);
        text.setPadding(0, dp(12), 0, dp(12));

        parent.addView(text);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "-"
                : value;
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
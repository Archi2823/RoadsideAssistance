package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvWelcome, tvEmpty;
    private TextView tvAssignedCount, tvProgressCount, tvCompletedCount;
    private LinearLayout assignedRequestsContainer;
    private LinearLayout progressRequestsContainer;
    private LinearLayout completedRequestsContainer;
    private Button btnLogout;

    private ListenerRegistration requestsListener;
    private String workerUid;
    private String workerCategory;

    private EditText completionRemarkInput;
    private AlertDialog completionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvAssignedCount = findViewById(R.id.tvAssignedCount);
        tvProgressCount = findViewById(R.id.tvProgressCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);

        assignedRequestsContainer = findViewById(R.id.assignedRequestsContainer);
        progressRequestsContainer = findViewById(R.id.progressRequestsContainer);
        completedRequestsContainer = findViewById(R.id.completedRequestsContainer);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> logout());


        verifyWorkerAndLoadRequests();
    }

    private void verifyWorkerAndLoadRequests() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        workerUid = user.getUid();

        db.collection("users").document(workerUid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showToast("Worker profile not found.");
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    String role = document.getString("role");
                    String approvalStatus = document.getString("approvalStatus");

                    if (!"worker".equalsIgnoreCase(role)) {
                        showToast("Access denied. Worker account required.");
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    if (!"approved".equalsIgnoreCase(approvalStatus)) {
                        showToast("Your worker account is not approved yet.");
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    workerCategory = document.getString("serviceCategory");
                    if (workerCategory == null || workerCategory.trim().isEmpty()) {
                        showToast("Your service category is missing. Please contact admin.");
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    String name = document.getString("name");
                    tvWelcome.setText("Welcome, " + (name == null ? "Worker" : name)
                            + "\n" + workerCategory);

                    loadAssignedRequests(workerUid);
                })
                .addOnFailureListener(e ->
                        showToast("Unable to verify worker: " + e.getMessage()));
    }

    private void loadAssignedRequests(String uid) {
        if (requestsListener != null) {
            requestsListener.remove();
        }

        requestsListener = db.collection("serviceRequests")
                .whereEqualTo("assignedWorkerId", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        showToast("Unable to load requests: " + error.getMessage());
                        return;
                    }

                    assignedRequestsContainer.removeAllViews();
                    progressRequestsContainer.removeAllViews();
                    completedRequestsContainer.removeAllViews();

                    if (snapshots == null || snapshots.isEmpty()) {
                        tvAssignedCount.setText("0");
                        tvProgressCount.setText("0");
                        tvCompletedCount.setText("0");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);

                    List<DocumentSnapshot> assigned = new ArrayList<>();
                    List<DocumentSnapshot> inProgress = new ArrayList<>();
                    List<DocumentSnapshot> completed = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = safe(doc.getString("status")).toLowerCase();

                        if ("assigned".equals(status)) {
                            assigned.add(doc);
                        } else if ("in progress".equals(status)) {
                            inProgress.add(doc);
                        } else if ("resolved".equals(status)) {
                            completed.add(doc);
                        }
                    }

                    tvAssignedCount.setText(String.valueOf(assigned.size()));
                    tvProgressCount.setText(String.valueOf(inProgress.size()));
                    tvCompletedCount.setText(String.valueOf(completed.size()));

                    addSectionEmpty(assignedRequestsContainer, assigned.isEmpty(),
                            "No new requests right now.");
                    addSectionEmpty(progressRequestsContainer, inProgress.isEmpty(),
                            "No work is currently in progress.");
                    addSectionEmpty(completedRequestsContainer, completed.isEmpty(),
                            "Your completed requests will appear here.");

                    for (DocumentSnapshot doc : assigned) {
                        addRequestCard(doc, assignedRequestsContainer);
                    }
                    for (DocumentSnapshot doc : inProgress) {
                        addRequestCard(doc, progressRequestsContainer);
                    }
                    for (DocumentSnapshot doc : completed) {
                        addRequestCard(doc, completedRequestsContainer);
                    }
                });
    }

    private void addSectionEmpty(LinearLayout parent, boolean empty, String message) {
        if (!empty) return;

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.rgb(125, 139, 155));
        text.setTextSize(13);
        text.setPadding(dp(4), dp(8), dp(4), dp(8));
        parent.addView(text);
    }

    private void addRequestCard(DocumentSnapshot doc, LinearLayout targetContainer) {
        String requestId = doc.getId();
        String serviceType = value(doc, "serviceType");
        String category = value(doc, "serviceCategory");
        String vehicleType = value(doc, "vehicleType");
        String issue = value(doc, "towingIssue");

        if ("Not provided".equals(issue)) {
            issue = value(doc, "batteryIssue");
        }

        String location = value(doc, "location");
        String destination = value(doc, "destination");
        String description = value(doc, "description");
        String status = value(doc, "status");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(16));
        card.setBackground(roundedBackground(Color.WHITE, dp(16),
                Color.rgb(229, 235, 243), dp(1)));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setElevation(dp(2));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(serviceType);
        title.setTextColor(Color.rgb(23, 59, 108));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        topRow.addView(title, titleParams);

        TextView statusBadge = new TextView(this);
        statusBadge.setText(status.toUpperCase());
        statusBadge.setTextSize(10);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
        statusBadge.setTextColor(statusColor(status));
        statusBadge.setBackground(roundedBackground(statusBackground(status),
                dp(30), Color.TRANSPARENT, 0));
        topRow.addView(statusBadge);
        card.addView(topRow);

        TextView requestIdText = smallText("Request #" + shortId(requestId));
        requestIdText.setPadding(0, dp(5), 0, dp(10));
        card.addView(requestIdText);

        addInfoRow(card, "Vehicle", vehicleType);
        addInfoRow(card, "Category", category);
        addInfoRow(card, "Issue", issue);
        addInfoRow(card, "Pickup", location);

        if (!"Not provided".equals(destination)) {
            addInfoRow(card, "Destination", destination);
        }

        if (!"Not provided".equals(description)) {
            addInfoRow(card, "Details", description);
        }

        if ("assigned".equalsIgnoreCase(status)) {
            Button startButton = actionButton("Start Work", Color.rgb(37, 99, 235));
            card.addView(startButton);
            startButton.setOnClickListener(v -> {
                startButton.setEnabled(false);
                startButton.setText("Starting...");
                updateStartWork(requestId, startButton);
            });

        } else if ("in progress".equalsIgnoreCase(status)) {
            TextView progressInfo = smallText("You are currently working on this request.");
            progressInfo.setTextColor(Color.rgb(177, 111, 16));
            progressInfo.setPadding(0, dp(10), 0, dp(8));
            card.addView(progressInfo);

            Button completeButton = actionButton("Complete Work", Color.rgb(24, 131, 76));
            card.addView(completeButton);
            completeButton.setOnClickListener(v -> showCompletionDialog(requestId));

        } else if ("resolved".equalsIgnoreCase(status)) {
            TextView done = smallText("✓ Work completed successfully");
            done.setTextColor(Color.rgb(24, 131, 76));
            done.setTypeface(null, Typeface.BOLD);
            done.setPadding(0, dp(10), 0, dp(4));
            card.addView(done);

            String remark = doc.getString("resolutionRemark");
            if (remark != null && !remark.trim().isEmpty()) {
                addInfoRow(card, "Resolution", remark);
            }

        }

        targetContainer.addView(card);
    }

    private void addInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.rgb(112, 127, 145));
        labelView.setTextSize(12);
        labelView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                dp(82), LinearLayout.LayoutParams.WRAP_CONTENT);
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.rgb(45, 59, 78));
        valueView.setTextSize(13);
        valueView.setMaxLines(4);
        row.addView(valueView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        parent.addView(row);
    }

    private void updateStartWork(String requestId, Button button) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "in progress");
        updates.put("workStartedAt", FieldValue.serverTimestamp());

        db.collection("serviceRequests").document(requestId)
                .update(updates)
                .addOnSuccessListener(unused ->
                        showToast("Work started successfully!"))
                .addOnFailureListener(e -> {
                    button.setEnabled(true);
                    button.setText("Start Work");
                    showToast("Unable to start work: " + e.getMessage());
                });
    }

    private void showCompletionDialog(String requestId) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), 0);

        completionRemarkInput = new EditText(this);
        completionRemarkInput.setHint("Enter work completed / resolution details");
        completionRemarkInput.setGravity(Gravity.TOP | Gravity.START);
        completionRemarkInput.setMinLines(3);
        completionRemarkInput.setMaxLines(5);
        completionRemarkInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        content.addView(completionRemarkInput);

        completionDialog = new AlertDialog.Builder(this)
                .setTitle("Complete Service Request")
                .setMessage("Enter a short remark describing the work completed.")
                .setView(content)
                .setNegativeButton("Cancel", (dialog, which) -> clearCompletionForm())
                .setPositiveButton("Submit Completion", null)
                .create();

        completionDialog.setOnShowListener(dialog -> {
            Button submit = completionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submit.setOnClickListener(v -> {
                String remark = completionRemarkInput.getText().toString().trim();

                if (remark.isEmpty()) {
                    completionRemarkInput.setError("Please enter resolution details");
                    return;
                }

                if (remark.length() > 2000) {
                    completionRemarkInput.setError("Maximum 2000 characters allowed");
                    return;
                }

                submit.setEnabled(false);
                submit.setText("Submitting...");
                resolveRequestAndUpdateCounter(requestId, remark, submit);
            });
        });

        completionDialog.show();
    }

    private void resolveRequestAndUpdateCounter(
            String requestId, String remark, Button submitButton) {

        com.google.firebase.firestore.DocumentReference requestRef =
                db.collection("serviceRequests").document(requestId);
        com.google.firebase.firestore.DocumentReference statsRef =
                db.collection("workerStats").document(workerUid);

        db.runTransaction(transaction -> {
            DocumentSnapshot requestSnapshot = transaction.get(requestRef);
            DocumentSnapshot statsSnapshot = transaction.get(statsRef);

            if (!requestSnapshot.exists()) {
                throw new IllegalStateException("Service request no longer exists.");
            }
            if (!statsSnapshot.exists()) {
                throw new IllegalStateException(
                        "Worker stats are missing. Contact administrator.");
            }

            String assignedWorkerId = requestSnapshot.getString("assignedWorkerId");
            String currentStatus = requestSnapshot.getString("status");

            if (!workerUid.equals(assignedWorkerId)) {
                throw new IllegalStateException("This request is not assigned to you.");
            }
            if (!"in progress".equalsIgnoreCase(currentStatus)) {
                throw new IllegalStateException("This request is not in progress.");
            }

            Long activeCount = statsSnapshot.getLong("activeRequestCount");
            long currentCount = activeCount == null ? 0 : activeCount;

            if (currentCount <= 0) {
                throw new IllegalStateException("Active request counter is invalid.");
            }

            Map<String, Object> requestUpdates = new HashMap<>();
            requestUpdates.put("status", "resolved");
            requestUpdates.put("resolutionRemark", remark);
            requestUpdates.put("resolvedAt", FieldValue.serverTimestamp());

            Map<String, Object> statsUpdates = new HashMap<>();
            statsUpdates.put("activeRequestCount", currentCount - 1);
            statsUpdates.put("lastResolvedRequestId", requestId);
            statsUpdates.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(requestRef, requestUpdates);
            transaction.update(statsRef, statsUpdates);

            return null;
        }).addOnSuccessListener(unused -> {
            showToast("Work completed successfully!");
            if (completionDialog != null && completionDialog.isShowing()) {
                completionDialog.dismiss();
            }
            clearCompletionForm();
        }).addOnFailureListener(e -> {
            submitButton.setEnabled(true);
            submitButton.setText("Submit Completion");
            showToast("Unable to complete work: " + e.getMessage());
        });
    }

    private void clearCompletionForm() {
        completionRemarkInput = null;
        completionDialog = null;
    }

    private Button actionButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        params.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private TextView smallText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.rgb(100, 116, 139));
        view.setTextSize(12);
        return view;
    }

    private GradientDrawable roundedBackground(int color, int radius,
                                               int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private int statusColor(String status) {
        if ("assigned".equalsIgnoreCase(status)) return Color.rgb(37, 99, 235);
        if ("in progress".equalsIgnoreCase(status)) return Color.rgb(196, 122, 22);
        if ("resolved".equalsIgnoreCase(status)) return Color.rgb(24, 131, 76);
        return Color.DKGRAY;
    }

    private int statusBackground(String status) {
        if ("assigned".equalsIgnoreCase(status)) return Color.rgb(232, 241, 255);
        if ("in progress".equalsIgnoreCase(status)) return Color.rgb(255, 243, 223);
        if ("resolved".equalsIgnoreCase(status)) return Color.rgb(229, 247, 237);
        return Color.LTGRAY;
    }

    private String value(DocumentSnapshot doc, String field) {
        String value = doc.getString(field);
        return value == null || value.trim().isEmpty() ? "Not provided" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8).toUpperCase() : id.toUpperCase();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void logout() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
        auth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        android.content.Intent intent = new android.content.Intent(
                WorkerDashboardActivity.this, MainActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
    }
}
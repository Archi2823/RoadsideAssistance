
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class WorkerDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout requestsContainer;
    private TextView tvWelcome, tvEmpty;
    private Button btnLogout;

    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvEmpty = findViewById(R.id.tvEmpty);
        requestsContainer = findViewById(R.id.requestsContainer);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            auth.signOut();

            Intent intent = new Intent(
                    WorkerDashboardActivity.this,
                    MainActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });

        verifyWorkerAndLoadRequests();
    }

    private void verifyWorkerAndLoadRequests() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            goToLogin();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(this,
                                "Worker profile not found.",
                                Toast.LENGTH_LONG).show();
                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    String role = document.getString("role");

                    if (role == null ||
                            !role.trim().equalsIgnoreCase("worker")) {

                        Toast.makeText(this,
                                "Access denied. Worker account required.",
                                Toast.LENGTH_LONG).show();

                        auth.signOut();
                        goToLogin();
                        return;
                    }

                    String name = document.getString("name");

                    tvWelcome.setText(
                            "Welcome, " +
                                    (name == null ? "Worker" : name)
                    );

                    loadAssignedRequests(user.getUid());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to verify worker account: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void loadAssignedRequests(String workerUid) {

        requestsListener = db.collection("serviceRequests")
                .whereEqualTo("assignedWorkerId", workerUid)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "Unable to load requests: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    requestsContainer.removeAllViews();

                    if (snapshots == null || snapshots.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        addRequestCard(doc);
                    }
                });
    }

    private void addRequestCard(DocumentSnapshot doc) {

        String requestId = doc.getId();

        String serviceType = getValue(doc, "serviceType");
        String vehicleType = getValue(doc, "vehicleType");
        String issue = getValue(doc, "towingIssue");
        String location = getValue(doc, "location");
        String destination = getValue(doc, "destination");
        String description = getValue(doc, "description");
        String status = getValue(doc, "status");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(35, 30, 35, 30);

        card.setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 25);
        card.setLayoutParams(cardParams);

        addText(card, "Request ID: " + requestId, true);
        addText(card, "Service: " + serviceType, false);
        addText(card, "Vehicle: " + vehicleType, false);
        addText(card, "Issue: " + issue, false);
        addText(card, "Pickup Location: " + location, false);
        addText(card, "Destination: " + destination, false);
        addText(card, "Description: " + description, false);
        addText(card, "Status: " + status, true);

        if ("assigned".equalsIgnoreCase(status)) {

            Button btnStartWork = new Button(this);
            btnStartWork.setText("Start Work");
            btnStartWork.setAllCaps(false);

            card.addView(btnStartWork);

            btnStartWork.setOnClickListener(v -> {
                btnStartWork.setEnabled(false);
                updateRequestStatus(requestId, "in progress");
            });

        } else if ("in progress".equalsIgnoreCase(status)) {

            TextView progressText = new TextView(this);
            progressText.setText("Work is in progress");
            progressText.setTextColor(
                    android.graphics.Color.rgb(20, 130, 70)
            );
            progressText.setPadding(0, 15, 0, 5);

            card.addView(progressText);
        }

        requestsContainer.addView(card);
    }

    private void addText(LinearLayout parent,
                         String value,
                         boolean bold) {

        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(15);
        textView.setTextColor(
                android.graphics.Color.rgb(35, 55, 75)
        );
        textView.setPadding(0, 7, 0, 7);

        if (bold) {
            textView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        parent.addView(textView);
    }

    private String getValue(DocumentSnapshot doc, String field) {

        String value = doc.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }

        return value;
    }

    private void updateRequestStatus(String requestId,
                                     String newStatus) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("workStartedAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("serviceRequests")
                .document(requestId)
                .update(updates)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Work started successfully!",
                                Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Unable to start work: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    private void goToLogin() {

        Intent intent = new Intent(
                WorkerDashboardActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}

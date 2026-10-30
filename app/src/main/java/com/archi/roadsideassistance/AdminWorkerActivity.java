
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminWorkerActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout workersContainer;
    private TextView tvWorkerCount, tvEmpty;
    private ProgressBar progressBar;

    private Button btnPending, btnApproved, btnRejected;

    private ListenerRegistration workerListener;

    private final List<DocumentSnapshot> allWorkers = new ArrayList<>();

    private String selectedStatus = "pending";

    private static final String ADMIN_UID =
            "mfKj4Ggasqe1aQT9z5fb9JokXV82";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_worker);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        workersContainer = findViewById(R.id.workersContainer);
        tvWorkerCount = findViewById(R.id.tvWorkerCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        btnPending = findViewById(R.id.btnPending);
        btnApproved = findViewById(R.id.btnApproved);
        btnRejected = findViewById(R.id.btnRejected);

        btnPending.setOnClickListener(v -> {
            selectedStatus = "pending";
            updateWorkerList();
        });

        btnApproved.setOnClickListener(v -> {
            selectedStatus = "approved";
            updateWorkerList();
        });

        btnRejected.setOnClickListener(v -> {
            selectedStatus = "rejected";
            updateWorkerList();
        });

        verifyAdmin();
    }

    // Verify Admin account
    private void verifyAdmin() {

        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        if (!uid.equals(ADMIN_UID)) {
            Toast.makeText(this,
                    "Access denied. Admin only.",
                    Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (document.exists()
                            && "admin".equalsIgnoreCase(
                            document.getString("role"))) {

                        loadWorkers();

                    } else {
                        Toast.makeText(this,
                                "Admin verification failed.",
                                Toast.LENGTH_LONG).show();
                        redirectToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Verification error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    redirectToLogin();
                });
    }

    // Load all workers from Firestore
    private void loadWorkers() {

        progressBar.setVisibility(View.VISIBLE);

        workerListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .addSnapshotListener((snapshots, error) -> {

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this,
                                "Unable to load workers: " +
                                        error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    allWorkers.clear();

                    if (snapshots != null) {
                        allWorkers.addAll(snapshots.getDocuments());
                    }

                    updateWorkerList();
                });
    }

    // Display workers according to selected status
    private void updateWorkerList() {

        workersContainer.removeAllViews();

        int count = 0;

        for (DocumentSnapshot worker : allWorkers) {

            String status = worker.getString("approvalStatus");

            if (status == null) {
                status = "pending";
            }

            if (selectedStatus.equalsIgnoreCase(status)) {
                addWorkerCard(worker);
                count++;
            }
        }

        tvWorkerCount.setText(
                selectedStatus.substring(0, 1).toUpperCase(Locale.ROOT)
                        + selectedStatus.substring(1)
                        + " Workers: " + count
        );

        if (count == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        updateTabAppearance();
    }

    // Create worker card
    private void addWorkerCard(DocumentSnapshot worker) {

        String uid = worker.getId();

        String name = getValue(worker, "name");
        String email = getValue(worker, "email");
        String phone = getValue(worker, "phone");
        String status = getValue(worker, "approvalStatus");

        if (status.isEmpty()) {
            status = "pending";
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();

        background.setColor(android.graphics.Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1),
                android.graphics.Color.rgb(220, 228, 238));

        card.setBackground(background);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView tvName = createText(
                name.isEmpty() ? "Worker" : name,
                18, true
        );

        TextView tvEmail = createText(
                "Email: " + displayValue(email),
                14, false
        );

        TextView tvPhone = createText(
                "Phone: " + displayValue(phone),
                14, false
        );

        TextView tvStatus = createText(
                "Status: " + status.toUpperCase(Locale.ROOT),
                14, true
        );

        if (status.equalsIgnoreCase("pending")) {
            tvStatus.setTextColor(
                    android.graphics.Color.rgb(220, 145, 30));
        } else if (status.equalsIgnoreCase("approved")) {
            tvStatus.setTextColor(
                    android.graphics.Color.rgb(35, 145, 90));
        } else {
            tvStatus.setTextColor(
                    android.graphics.Color.RED);
        }

        card.addView(tvName);
        card.addView(tvEmail);
        card.addView(tvPhone);
        card.addView(tvStatus);

        if (status.equalsIgnoreCase("pending")) {

            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setPadding(0, dp(12), 0, 0);

            Button btnApprove = new Button(this);
            btnApprove.setText("Approve");
            btnApprove.setAllCaps(false);
            btnApprove.setTextColor(android.graphics.Color.WHITE);
            btnApprove.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(35, 145, 90)
                    )
            );

            Button btnReject = new Button(this);
            btnReject.setText("Reject");
            btnReject.setAllCaps(false);
            btnReject.setTextColor(android.graphics.Color.WHITE);
            btnReject.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.rgb(200, 55, 55)
                    )
            );

            LinearLayout.LayoutParams buttonParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(48),
                            1
                    );

            buttonParams.setMargins(0, 0, dp(8), 0);
            btnApprove.setLayoutParams(buttonParams);

            LinearLayout.LayoutParams rejectParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(48),
                            1
                    );

            rejectParams.setMargins(dp(8), 0, 0, 0);
            btnReject.setLayoutParams(rejectParams);

            btnApprove.setOnClickListener(v ->
                    confirmWorkerAction(
                            uid, name, "approved"
                    )
            );

            btnReject.setOnClickListener(v ->
                    confirmWorkerAction(
                            uid, name, "rejected"
                    )
            );

            buttonRow.addView(btnApprove);
            buttonRow.addView(btnReject);

            card.addView(buttonRow);
        }

        workersContainer.addView(card);
    }

    // Confirm before approving or rejecting
    private void confirmWorkerAction(
            String uid,
            String name,
            String newStatus) {

        String action = newStatus.equals("approved")
                ? "approve"
                : "reject";

        new AlertDialog.Builder(this)
                .setTitle("Confirm " + action)
                .setMessage("Are you sure you want to " +
                        action + " " + name + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) ->
                        updateWorkerStatus(uid, newStatus))
                .show();
    }

    // Update worker approval status
    private void updateWorkerStatus(
            String uid,
            String newStatus) {

        db.collection("users")
                .document(uid)
                .update(
                        "approvalStatus", newStatus,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,
                            "Worker " + newStatus + " successfully.",
                            Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                });
    }

    private void updateTabAppearance() {

        btnPending.setAlpha(
                selectedStatus.equals("pending") ? 1f : 0.55f);

        btnApproved.setAlpha(
                selectedStatus.equals("approved") ? 1f : 0.55f);

        btnRejected.setAlpha(
                selectedStatus.equals("rejected") ? 1f : 0.55f);
    }

    private String getValue(
            DocumentSnapshot document,
            String field) {

        String value = document.getString(field);
        return value == null ? "" : value;
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty()
                ? "Not available" : value;
    }

    private TextView createText(
            String value,
            int size,
            boolean bold) {

        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(
                android.graphics.Color.rgb(55, 65, 80));
        textView.setPadding(0, dp(4), 0, dp(4));

        if (bold) {
            textView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        return textView;
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private void redirectToLogin() {

        auth.signOut();

        Intent intent = new Intent(
                AdminWorkerActivity.this,
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

        if (workerListener != null) {
            workerListener.remove();
        }
    }
}


package com.archi.roadsideassistance;

import android.os.Bundle;
import android.view.View;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class AdminRequestActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout requestsContainer;
    private TextView tvRequestCount, tvEmpty;
    private ProgressBar progressBar;

    private ListenerRegistration requestListener;

    private static final String ADMIN_UID =
            "mfKj4Ggasqe1aQT9z5fb9JokXV82";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_request);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        requestsContainer = findViewById(R.id.requestsContainer);
        tvRequestCount = findViewById(R.id.tvRequestCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);

        verifyAdmin();
    }

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

                    if (isFinishing()) return;

                    if (document.exists()
                            && "admin".equalsIgnoreCase(
                            document.getString("role"))) {

                        loadRequests();

                    } else {
                        Toast.makeText(this,
                                "Admin profile not verified.",
                                Toast.LENGTH_LONG).show();
                        redirectToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    redirectToLogin();
                });
    }

    private void loadRequests() {

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        requestListener = db.collection("serviceRequests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Toast.makeText(this,
                                "Unable to load requests: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    requestsContainer.removeAllViews();

                    if (snapshots == null || snapshots.isEmpty()) {
                        tvRequestCount.setText("Total Requests: 0");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    tvRequestCount.setText(
                            "Total Requests: " + snapshots.size()
                    );

                    for (DocumentSnapshot document : snapshots) {
                        addRequestCard(document);
                    }
                });
    }

    private void addRequestCard(DocumentSnapshot document) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();

        bg.setColor(android.graphics.Color.WHITE);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1),
                android.graphics.Color.rgb(225, 230, 235));

        card.setBackground(bg);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        String requestId = document.getId();

        String service = getFirstValue(document,
                "serviceType", "requestType", "serviceName", "type");

        String customer = getFirstValue(document,
                "customerName", "name", "userName");

        String status = getFirstValue(document,
                "status");

        String location = getFirstValue(document,
                "location", "address", "pickupLocation");

        TextView tvService = createText(
                service.isEmpty() ? "Service Request" : service,
                18, true
        );

        TextView tvId = createText(
                "Request ID: " + requestId,
                12, false
        );

        TextView tvCustomer = createText(
                "Customer: " +
                        (customer.isEmpty() ? "Not available" : customer),
                14, false
        );

        TextView tvLocation = createText(
                "Location: " +
                        (location.isEmpty() ? "Not available" : location),
                14, false
        );

        TextView tvStatus = createText(
                "Status: " +
                        (status.isEmpty() ? "Not available" : status),
                14, true
        );

        tvService.setTextColor(
                android.graphics.Color.rgb(35, 65, 110));

        tvId.setTextColor(android.graphics.Color.GRAY);

        tvStatus.setTextColor(getStatusColor(status));

        card.addView(tvService);
        card.addView(tvId);
        card.addView(tvCustomer);
        card.addView(tvLocation);
        card.addView(tvStatus);

        card.setOnClickListener(v -> showRequestDetails(document));

        requestsContainer.addView(card);
    }

    private void showRequestDetails(DocumentSnapshot document) {

        StringBuilder details = new StringBuilder();

        details.append("Request ID: ")
                .append(document.getId())
                .append("\n\n");

        Map<String, Object> data = document.getData();

        if (data != null) {

            for (Map.Entry<String, Object> entry : data.entrySet()) {

                String key = entry.getKey();
                Object value = entry.getValue();

                details.append(formatLabel(key))
                        .append(": ")
                        .append(formatValue(value))
                        .append("\n\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Request Details")
                .setMessage(details.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private String getFirstValue(DocumentSnapshot document,
                                 String... keys) {

        for (String key : keys) {
            Object value = document.get(key);

            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString();
            }
        }

        return "";
    }

    private String formatLabel(String key) {

        String result = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return result.substring(0, 1).toUpperCase()
                + result.substring(1);
    }

    private String formatValue(Object value) {

        if (value == null) return "Not available";

        if (value instanceof Timestamp) {
            Date date = ((Timestamp) value).toDate();

            return new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format(date);
        }

        return value.toString();
    }

    private TextView createText(String text,
                                int size,
                                boolean bold) {

        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(android.graphics.Color.rgb(55, 65, 80));
        view.setPadding(0, dp(4), 0, dp(4));

        if (bold) {
            view.setTypeface(null,
                    android.graphics.Typeface.BOLD);
        }

        return view;
    }

    private int getStatusColor(String status) {

        if (status == null) return android.graphics.Color.GRAY;

        switch (status.toLowerCase(Locale.ROOT)) {

            case "pending":
                return android.graphics.Color.rgb(230, 145, 35);

            case "assigned":
                return android.graphics.Color.rgb(45, 115, 190);

            case "in progress":
                return android.graphics.Color.rgb(140, 90, 190);

            case "resolved":
            case "closed":
                return android.graphics.Color.rgb(35, 145, 90);

            case "cancelled":
                return android.graphics.Color.RED;

            default:
                return android.graphics.Color.GRAY;
        }
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private void redirectToLogin() {
        auth.signOut();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (requestListener != null) {
            requestListener.remove();
        }
    }
}

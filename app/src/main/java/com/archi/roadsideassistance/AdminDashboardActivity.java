
package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    private static final String ADMIN_UID =
            "mfKj4Ggasqe1aQT9z5fb9JokXV82";

    private TextView tvAdminName;
    private TextView tvTotalCustomers;
    private TextView tvTotalWorkers;
    private TextView tvPendingWorkers;
    private TextView tvTotalRequests;

    private Button btnManageWorkers;
    private Button btnManageRequests;
    private Button btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration customerListener;
    private ListenerRegistration workerListener;
    private ListenerRegistration pendingWorkerListener;
    private ListenerRegistration requestListener;

    private boolean isAdminVerified = false;
    private boolean isVerifying = false;
    private boolean isRedirecting = false;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable verificationTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        initializeListeners();

        verifyAdmin();
    }

    // =========================================
    // INITIALIZE VIEWS
    // =========================================

    private void initializeViews() {

        tvAdminName = findViewById(R.id.tvAdminName);

        tvTotalCustomers = findViewById(R.id.tvTotalCustomers);
        tvTotalWorkers = findViewById(R.id.tvTotalWorkers);
        tvPendingWorkers = findViewById(R.id.tvPendingWorkers);
        tvTotalRequests = findViewById(R.id.tvTotalRequests);

        btnManageWorkers = findViewById(R.id.btnManageWorkers);
        btnManageRequests = findViewById(R.id.btnManageRequests);
        btnLogout = findViewById(R.id.btnLogout);

        tvAdminName.setText("Verifying Admin...");

        tvTotalCustomers.setText("...");
        tvTotalWorkers.setText("...");
        tvPendingWorkers.setText("...");
        tvTotalRequests.setText("...");

        btnManageWorkers.setEnabled(false);
        btnManageRequests.setEnabled(false);
    }

    // =========================================
    // BUTTON LISTENERS
    // =========================================

    private void initializeListeners() {

        btnLogout.setOnClickListener(v -> logoutAdmin());

        btnManageWorkers.setOnClickListener(v -> {

            if (!isAdminVerified) {
                showMessage("Admin verification is not complete.");
                return;
            }

            startActivity(new Intent(
                    AdminDashboardActivity.this,
                    AdminWorkerActivity.class
            ));
        });

        btnManageRequests.setOnClickListener(v -> {

            if (!isAdminVerified) {
                showMessage("Admin verification is not complete.");
                return;
            }

            startActivity(new Intent(
                    AdminDashboardActivity.this,
                    AdminRequestActivity.class
            ));
        });
    }

    // =========================================
    // VERIFY ADMIN
    // =========================================

    private void verifyAdmin() {

        if (isVerifying || isAdminVerified) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {

            tvAdminName.setText("Please login again.");
            showMessage("No logged-in user found.");

            redirectToLogin();
            return;
        }

        String authUid = currentUser.getUid().trim();

        Log.d(TAG, "Authentication UID = [" + authUid + "]");
        Log.d(TAG, "Expected Admin UID = [" + ADMIN_UID + "]");

        isVerifying = true;

        tvAdminName.setText("Verifying Admin account...");

        btnManageWorkers.setEnabled(false);
        btnManageRequests.setEnabled(false);

        // Verify the configured Admin account
        if (!ADMIN_UID.trim().equals(authUid)) {

            isVerifying = false;

            tvAdminName.setText("Unauthorized account");

            Log.e(TAG, "Admin UID mismatch.");
            showMessage("Unauthorized Admin account.");

            redirectToLogin();
            return;
        }

        // Verification timeout
        verificationTimeout = () -> {

            if (!isAdminVerified && isVerifying) {

                isVerifying = false;

                tvAdminName.setText("Verification timed out");

                showMessage(
                        "Verification timed out. " +
                                "Check internet and Firestore Rules."
                );

                Log.e(TAG, "Admin verification timeout.");
            }
        };

        handler.postDelayed(verificationTimeout, 15000);

        // Fetch Admin document using Authentication UID
        db.collection("users")
                .document(authUid)
                .get()
                .addOnCompleteListener(task -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (verificationTimeout != null) {
                        handler.removeCallbacks(verificationTimeout);
                    }

                    // Ignore a response received after timeout
                    if (!isVerifying) {
                        return;
                    }

                    isVerifying = false;

                    // Firestore error
                    if (!task.isSuccessful()) {

                        Exception error = task.getException();

                        tvAdminName.setText("Verification failed");

                        Log.e(TAG,
                                "Firestore verification error",
                                error);

                        showMessage(
                                "Firestore Error: " +
                                        (error != null
                                                ? error.getMessage()
                                                : "Unknown error")
                        );

                        return;
                    }

                    DocumentSnapshot document = task.getResult();

                    if (document == null || !document.exists()) {

                        tvAdminName.setText("Admin profile not found");

                        Log.e(TAG,
                                "Admin document not found. UID = " +
                                        authUid);

                        showMessage(
                                "Admin profile not found in users collection."
                        );

                        return;
                    }

                    // Read Firestore values
                    String documentId = document.getId().trim();

                    String firestoreUid =
                            document.getString("uid");

                    String role =
                            document.getString("role");

                    String name =
                            document.getString("name");

                    Log.d(TAG,
                            "Authentication UID = [" + authUid + "]");

                    Log.d(TAG,
                            "Firestore Document ID = [" + documentId + "]");

                    Log.d(TAG,
                            "Firestore UID Field = [" + firestoreUid + "]");

                    Log.d(TAG,
                            "Firestore Role = [" + role + "]");

                    // Check document ID
                    if (!authUid.equals(documentId)) {

                        tvAdminName.setText("Document ID mismatch");

                        showMessage(
                                "Firestore document ID does not match Authentication UID."
                        );

                        return;
                    }

                    // Check UID field, ignoring accidental spaces
                    if (firestoreUid == null ||
                            !authUid.equals(firestoreUid.trim())) {

                        tvAdminName.setText("UID field mismatch");

                        showMessage(
                                "Firestore uid field does not match Authentication UID."
                        );

                        Log.e(TAG,
                                "UID field mismatch detected.");

                        return;
                    }

                    // Check role
                    if (role == null ||
                            !role.trim().equalsIgnoreCase("admin")) {

                        tvAdminName.setText("Invalid Admin role");

                        showMessage(
                                "Invalid Admin role: " + role
                        );

                        return;
                    }

                    // =================================
                    // ADMIN VERIFIED SUCCESSFULLY
                    // =================================

                    isAdminVerified = true;

                    btnManageWorkers.setEnabled(true);
                    btnManageRequests.setEnabled(true);

                    String adminName =
                            (name != null && !name.trim().isEmpty())
                                    ? name.trim()
                                    : "Admin";

                    tvAdminName.setText(
                            "Welcome, " + adminName + "!"
                    );

                    Log.d(TAG,
                            "Admin verification successful.");

                    showMessage("Admin verified successfully!");

                    loadDashboardCounts();

                });
    }

    // =========================================
    // LOAD DASHBOARD COUNTS
    // =========================================

    private void loadDashboardCounts() {

        if (!isAdminVerified) {
            return;
        }

        removeDashboardListeners();

        // Total Customers
        customerListener = db.collection("users")
                .whereEqualTo("role", "customer")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        tvTotalCustomers.setText("!");
                        showReadError("Customers", error);
                        return;
                    }

                    if (snapshots != null) {
                        tvTotalCustomers.setText(
                                String.valueOf(snapshots.size())
                        );
                    }
                });

        // Total Workers
        workerListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        tvTotalWorkers.setText("!");
                        showReadError("Workers", error);
                        return;
                    }

                    if (snapshots != null) {
                        tvTotalWorkers.setText(
                                String.valueOf(snapshots.size())
                        );
                    }
                });

        // Pending Workers
        pendingWorkerListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "pending")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        tvPendingWorkers.setText("!");
                        showReadError("Pending Workers", error);
                        return;
                    }

                    if (snapshots != null) {
                        tvPendingWorkers.setText(
                                String.valueOf(snapshots.size())
                        );
                    }
                });

        // Total Service Requests
        requestListener = db.collection("serviceRequests")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        tvTotalRequests.setText("!");
                        showReadError("Service Requests", error);
                        return;
                    }

                    if (snapshots != null) {
                        tvTotalRequests.setText(
                                String.valueOf(snapshots.size())
                        );
                    }
                });
    }

    // =========================================
    // FIRESTORE ERROR HANDLING
    // =========================================

    private void showReadError(
            String section,
            Exception error) {

        Log.e(TAG, section + " loading error", error);

        if (!isFinishing() && !isDestroyed()) {

            Toast.makeText(
                    this,
                    section + " error: " + error.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================
    // LOGOUT
    // =========================================

    private void logoutAdmin() {

        removeDashboardListeners();

        isAdminVerified = false;
        isVerifying = false;

        if (verificationTimeout != null) {
            handler.removeCallbacks(verificationTimeout);
        }

        auth.signOut();

        redirectToLogin();
    }

    // =========================================
    // REMOVE FIRESTORE LISTENERS
    // =========================================

    private void removeDashboardListeners() {

        if (customerListener != null) {
            customerListener.remove();
            customerListener = null;
        }

        if (workerListener != null) {
            workerListener.remove();
            workerListener = null;
        }

        if (pendingWorkerListener != null) {
            pendingWorkerListener.remove();
            pendingWorkerListener = null;
        }

        if (requestListener != null) {
            requestListener.remove();
            requestListener = null;
        }
    }

    // =========================================
    // REDIRECT TO LOGIN
    // =========================================

    private void redirectToLogin() {

        if (isRedirecting) {
            return;
        }

        isRedirecting = true;

        Intent intent = new Intent(
                AdminDashboardActivity.this,
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // =========================================
    // TOAST MESSAGE
    // =========================================

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================
    // ACTIVITY DESTROY
    // =========================================

    @Override
    protected void onDestroy() {

        if (verificationTimeout != null) {
            handler.removeCallbacks(verificationTimeout);
        }

        removeDashboardListeners();

        super.onDestroy();
    }
}

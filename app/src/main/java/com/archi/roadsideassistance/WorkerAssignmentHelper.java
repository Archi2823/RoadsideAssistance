
package com.archi.roadsideassistance;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class WorkerAssignmentHelper {

    public interface AssignmentCallback {
        void onSuccess(String workerName);

        void onNoWorkerAvailable();

        void onFailure(String message);
    }

    private static final FirebaseFirestore db =
            FirebaseFirestore.getInstance();

    private WorkerAssignmentHelper() {
    }

    public static void assignRequest(
            String requestId,
            String serviceCategory,
            String customerUid,
            AssignmentCallback callback
    ) {

        if (requestId == null || requestId.trim().isEmpty()
                || serviceCategory == null || serviceCategory.trim().isEmpty()
                || customerUid == null || customerUid.trim().isEmpty()) {

            callback.onFailure("Invalid request details.");
            return;
        }

        DocumentReference requestRef = db
                .collection("serviceRequests")
                .document(requestId);

        // Query runs outside the transaction because Android
        // Firestore Transaction.get() does not accept Query.
        Query workersQuery = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .whereEqualTo("serviceCategory", serviceCategory)
                .limit(100);

        workersQuery.get()
                .addOnSuccessListener(workerSnapshot -> {

                    if (workerSnapshot.isEmpty()) {
                        callback.onNoWorkerAvailable();
                        return;
                    }

                    db.runTransaction(transaction -> {

                        // 1. Read request inside transaction.
                        DocumentSnapshot requestSnapshot =
                                transaction.get(requestRef);

                        if (!requestSnapshot.exists()) {
                            throw new FirebaseFirestoreException(
                                    "Service request does not exist.",
                                    FirebaseFirestoreException.Code.NOT_FOUND
                            );
                        }

                        String requestOwner =
                                requestSnapshot.getString("userId");

                        String currentStatus =
                                requestSnapshot.getString("status");

                        String existingWorker =
                                requestSnapshot.getString("assignedWorkerId");

                        String existingCategory =
                                requestSnapshot.getString("serviceCategory");

                        if (!customerUid.equals(requestOwner)) {
                            throw new FirebaseFirestoreException(
                                    "You cannot assign another customer's request.",
                                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                            );
                        }

                        if (!"pending".equalsIgnoreCase(currentStatus)) {
                            throw new FirebaseFirestoreException(
                                    "Request is no longer pending.",
                                    FirebaseFirestoreException.Code.ABORTED
                            );
                        }

                        if (existingWorker != null
                                && !existingWorker.trim().isEmpty()) {
                            throw new FirebaseFirestoreException(
                                    "A worker is already assigned.",
                                    FirebaseFirestoreException.Code.ABORTED
                            );
                        }

                        if (existingCategory != null
                                && !existingCategory.trim().isEmpty()
                                && !serviceCategory.equals(existingCategory)) {
                            throw new FirebaseFirestoreException(
                                    "Request category does not match.",
                                    FirebaseFirestoreException.Code.INVALID_ARGUMENT
                            );
                        }

                        DocumentSnapshot selectedWorker = null;
                        DocumentReference selectedStatsRef = null;
                        long lowestCount = Long.MAX_VALUE;

                        // 2. Read each matching worker and stats document.
                        // All reads happen before any transaction writes.
                        for (DocumentSnapshot worker :
                                workerSnapshot.getDocuments()) {

                            String workerUid = worker.getId();

                            DocumentReference workerRef = db
                                    .collection("users")
                                    .document(workerUid);

                            DocumentReference statsRef = db
                                    .collection("workerStats")
                                    .document(workerUid);

                            // Re-read worker profile inside transaction.
                            DocumentSnapshot latestWorker =
                                    transaction.get(workerRef);

                            DocumentSnapshot statsSnapshot =
                                    transaction.get(statsRef);

                            if (!latestWorker.exists()
                                    || !statsSnapshot.exists()) {
                                continue;
                            }

                            String category =
                                    latestWorker.getString("serviceCategory");

                            String approval =
                                    latestWorker.getString("approvalStatus");

                            Long countValue =
                                    statsSnapshot.getLong("activeRequestCount");

                            if (!serviceCategory.equals(category)
                                    || !"approved".equalsIgnoreCase(approval)
                                    || countValue == null
                                    || countValue < 0) {
                                continue;
                            }

                            long activeCount = countValue;

                            if (selectedWorker == null
                                    || activeCount < lowestCount
                                    || (activeCount == lowestCount
                                    && workerUid.compareTo(
                                    selectedWorker.getId()
                            ) < 0)) {

                                selectedWorker = latestWorker;
                                selectedStatsRef = statsRef;
                                lowestCount = activeCount;
                            }
                        }

                        if (selectedWorker == null
                                || selectedStatsRef == null) {
                            return null;
                        }

                        // 3. Selected worker details.
                        String workerUid = selectedWorker.getId();

                        String workerName =
                                selectedWorker.getString("name");

                        String workerPhone =
                                selectedWorker.getString("phone");

                        if (workerName == null
                                || workerName.trim().isEmpty()) {
                            workerName = "Assigned Worker";
                        }

                        if (workerPhone == null) {
                            workerPhone = "";
                        }

                        // 4. Update workload counter.
                        Map<String, Object> statsUpdates =
                                new HashMap<>();

                        statsUpdates.put(
                                "activeRequestCount",
                                lowestCount + 1
                        );

                        statsUpdates.put(
                                "lastAssignedRequestId",
                                requestId
                        );

                        statsUpdates.put(
                                "updatedAt",
                                FieldValue.serverTimestamp()
                        );

                        // 5. Update request.
                        Map<String, Object> requestUpdates =
                                new HashMap<>();

                        requestUpdates.put(
                                "serviceCategory",
                                serviceCategory
                        );

                        requestUpdates.put(
                                "assignedWorkerId",
                                workerUid
                        );

                        requestUpdates.put(
                                "assignedWorkerName",
                                workerName
                        );

                        requestUpdates.put(
                                "technicianAssigned",
                                true
                        );

                        requestUpdates.put(
                                "technicianName",
                                workerName
                        );

                        requestUpdates.put(
                                "technicianPhone",
                                workerPhone
                        );

                        requestUpdates.put(
                                "technicianRating",
                                ""
                        );

                        requestUpdates.put(
                                "technicianType",
                                serviceCategory
                        );

                        requestUpdates.put("status", "assigned");

                        requestUpdates.put(
                                "assignedAt",
                                FieldValue.serverTimestamp()
                        );

                        // Both writes commit atomically.
                        transaction.update(
                                selectedStatsRef,
                                statsUpdates
                        );

                        transaction.update(
                                requestRef,
                                requestUpdates
                        );

                        return workerName;

                    }).addOnSuccessListener(workerName -> {

                        if (workerName == null) {
                            callback.onNoWorkerAvailable();
                        } else {
                            callback.onSuccess(workerName);
                        }

                    }).addOnFailureListener(e -> {

                        callback.onFailure(
                                e.getLocalizedMessage() == null
                                        ? "Unable to assign worker."
                                        : e.getLocalizedMessage()
                        );
                    });

                })
                .addOnFailureListener(e ->
                        callback.onFailure(
                                "Unable to find approved workers: "
                                        + e.getLocalizedMessage()
                        )
                );
    }
}

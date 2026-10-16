package com.archi.roadsideassistance;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etComments;
    private TextView tvRatingLabel, tvMessage;
    private Button btnSubmit;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String requestId;
    private boolean submitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ratingBar = findViewById(R.id.ratingBarFeedback);
        etComments = findViewById(R.id.etFeedbackComments);
        tvRatingLabel = findViewById(R.id.tvRatingLabel);
        tvMessage = findViewById(R.id.tvFeedbackMessage);
        btnSubmit = findViewById(R.id.btnSubmitFeedback);

        requestId = getIntent().getStringExtra("requestId");

        if (auth.getCurrentUser() == null) {
            tvMessage.setText("Please log in again.");
            btnSubmit.setEnabled(false);
            return;
        }

        if (requestId == null || requestId.trim().isEmpty()) {
            tvMessage.setText(
                    "Request ID is missing. Open feedback from your completed request."
            );
            btnSubmit.setEnabled(false);
            return;
        }

        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            int stars = Math.round(rating);

            String[] labels = {
                    "Poor", "Fair", "Good", "Very Good", "Excellent"
            };

            if (stars >= 1 && stars <= 5) {
                tvRatingLabel.setText(labels[stars - 1]);
            } else {
                tvRatingLabel.setText("Rate Your Experience");
            }
        });

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        if (submitting) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvMessage.setText("Please log in again.");
            return;
        }

        int rating = Math.round(ratingBar.getRating());

        if (rating < 1 || rating > 5) {
            tvMessage.setText("Please select a rating.");
            return;
        }

        String comments = etComments.getText().toString().trim();

        if (comments.length() > 2000) {
            tvMessage.setText("Comments cannot exceed 2000 characters.");
            return;
        }

        submitting = true;
        btnSubmit.setEnabled(false);
        tvMessage.setText("Checking service request...");

        // Verify ownership and resolved status first.
        db.collection("serviceRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(request -> {

                    if (!request.exists()) {
                        showFailure("Service request not found.");
                        return;
                    }

                    String ownerId = request.getString("userId");
                    String status = request.getString("status");

                    if (!user.getUid().equals(ownerId)) {
                        showFailure("This service request does not belong to your account.");
                        return;
                    }

                    if (status == null || !"resolved".equals(status)) {
                        showFailure("Feedback is available only for resolved requests.");
                        return;
                    }

                    checkAndSaveFeedback(user, rating, comments);
                })
                .addOnFailureListener(e ->
                        showFailure("Could not verify service request: "
                                + e.getLocalizedMessage())
                );
    }

    private void checkAndSaveFeedback(
            FirebaseUser user,
            int rating,
            String comments
    ) {
        tvMessage.setText("Checking existing feedback...");

        db.collection("serviceFeedback")
                .document(requestId)
                .get()
                .addOnSuccessListener(existing -> {

                    if (existing.exists()) {
                        submitting = false;
                        btnSubmit.setEnabled(false);
                        btnSubmit.setText("Feedback Submitted");
                        tvMessage.setText(
                                "Feedback has already been submitted for this request."
                        );
                        return;
                    }

                    saveFeedback(user, rating, comments);
                })
                .addOnFailureListener(e ->
                        showFailure("Could not check existing feedback: "
                                + e.getLocalizedMessage())
                );
    }

    private void saveFeedback(
            FirebaseUser user,
            int rating,
            String comments
    ) {
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("requestId", requestId);
        feedback.put("userId", user.getUid());
        feedback.put("rating", rating);
        feedback.put("comments", comments);
        feedback.put("createdAt", FieldValue.serverTimestamp());

        tvMessage.setText("Submitting feedback...");

        db.collection("serviceFeedback")
                .document(requestId)
                .set(feedback)
                .addOnSuccessListener(unused -> {
                    submitting = false;
                    tvMessage.setText(
                            "Thank you! Your feedback was submitted successfully."
                    );
                    btnSubmit.setText("Feedback Submitted");
                    btnSubmit.setEnabled(false);
                })
                .addOnFailureListener(e ->
                        showFailure("Could not submit feedback: "
                                + e.getLocalizedMessage())
                );
    }

    private void showFailure(String message) {
        submitting = false;
        btnSubmit.setEnabled(true);
        tvMessage.setText(message);
    }
}

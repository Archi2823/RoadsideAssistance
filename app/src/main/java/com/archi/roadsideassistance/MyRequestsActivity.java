package com.archi.roadsideassistance;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private ListenerRegistration requestsListener;
    private boolean requestUiBuilt = false;

    private final List<DocumentSnapshot> allRequests = new ArrayList<>();
    private String selectedFilter = "All";
    private Long selectedDateStartMillis = null;
    private Long selectedDateEndMillis = null;
    private TextView selectedDateLabel;

    private final String[] filters = {
            "All", "Pending", "Assigned", "In Progress", "Resolved", "Cancelled"
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
    // LOAD REQUESTS FROM FIRESTORE IN REAL TIME
    // =====================================
    private void loadRequests() {

        // Build the screen only once. The Firestore listener below refreshes
        // the request cards whenever a request changes in Firebase.
        if (!requestUiBuilt) {
            container.removeAllViews();

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

            // Filter requests by a selected calendar date.
            LinearLayout dateFilterRow = new LinearLayout(this);
            dateFilterRow.setOrientation(LinearLayout.HORIZONTAL);
            dateFilterRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            dateFilterRow.setPadding(0, dp(12), 0, 0);

            Button chooseDateButton = new Button(this);
            chooseDateButton.setText("Choose Date");
            chooseDateButton.setAllCaps(false);
            dateFilterRow.addView(chooseDateButton, new LinearLayout.LayoutParams(
                    0, dp(44), 1f));

            Button clearDateButton = new Button(this);
            clearDateButton.setText("All Dates");
            clearDateButton.setAllCaps(false);
            dateFilterRow.addView(clearDateButton, new LinearLayout.LayoutParams(
                    0, dp(44), 1f));

            container.addView(dateFilterRow);

            selectedDateLabel = new TextView(this);
            selectedDateLabel.setTextSize(13);
            selectedDateLabel.setTextColor(Color.rgb(55, 65, 80));
            selectedDateLabel.setPadding(0, dp(6), 0, 0);
            container.addView(selectedDateLabel);
            updateSelectedDateLabel();

            chooseDateButton.setOnClickListener(v -> showDatePicker());
            clearDateButton.setOnClickListener(v -> {
                selectedDateStartMillis = null;
                selectedDateEndMillis = null;
                updateSelectedDateLabel();
                displayFilteredRequests();
            });

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
            requestUiBuilt = true;
        }

        // Avoid registering duplicate listeners when the activity resumes or
        // when edit/cancel operations call loadRequests().
        if (requestsListener != null) {
            return;
        }

        requestsListener = db.collection("serviceRequests")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        requestsContainer.removeAllViews();
                        addTextTo(
                                requestsContainer,
                                "Could not load requests: " + error.getMessage()
                        );
                        return;
                    }

                    if (snapshot == null) {
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
                            Timestamp firstTime = first.getTimestamp("createdAt");
                            Timestamp secondTime = second.getTimestamp("createdAt");

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
                });
    }

    @Override
    protected void onDestroy() {
        if (requestsListener != null) {
            requestsListener.remove();
            requestsListener = null;
        }
        super.onDestroy();
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
            boolean matchesStatus = "All".equals(selectedFilter)
                    || matchesFilter(status, selectedFilter);

            boolean matchesDate = true;
            if (selectedDateStartMillis != null && selectedDateEndMillis != null) {
                Timestamp createdAt = doc.getTimestamp("createdAt");
                if (createdAt == null) {
                    matchesDate = false;
                } else {
                    long createdMillis = createdAt.toDate().getTime();
                    matchesDate = createdMillis >= selectedDateStartMillis
                            && createdMillis < selectedDateEndMillis;
                }
            }

            if (matchesStatus && matchesDate) {
                filteredRequests.add(doc);
            }
        }

        TextView count = new TextView(this);
        count.setText(
                selectedFilter + " Requests (" + filteredRequests.size() + ")"
                        + (selectedDateStartMillis == null ? "" : " on selected date")
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

    private void showDatePicker() {
        Calendar initial = Calendar.getInstance();
        if (selectedDateStartMillis != null) {
            initial.setTimeInMillis(selectedDateStartMillis);
        }

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar start = Calendar.getInstance();
                    start.clear();
                    start.set(year, month, dayOfMonth, 0, 0, 0);
                    start.set(Calendar.MILLISECOND, 0);

                    Calendar end = (Calendar) start.clone();
                    end.add(Calendar.DAY_OF_MONTH, 1);

                    selectedDateStartMillis = start.getTimeInMillis();
                    selectedDateEndMillis = end.getTimeInMillis();
                    updateSelectedDateLabel();
                    displayFilteredRequests();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        );
        picker.setTitle("View orders for date");
        picker.show();
    }

    private void updateSelectedDateLabel() {
        if (selectedDateLabel == null) return;

        if (selectedDateStartMillis == null) {
            selectedDateLabel.setText("Showing orders from all dates");
        } else {
            SimpleDateFormat format = new SimpleDateFormat(
                    "dd MMM yyyy", java.util.Locale.getDefault());
            selectedDateLabel.setText("Showing orders for "
                    + format.format(new Date(selectedDateStartMillis)));
        }
    }

    private String formatRequestDate(DocumentSnapshot doc) {
        Timestamp timestamp = doc.getTimestamp("createdAt");
        if (timestamp == null) return "Date unavailable";
        SimpleDateFormat format = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
        return format.format(timestamp.toDate());
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

            case "Assigned":
                return normalized.equals("assigned");

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
        String fuelType = doc.getString("fuelType");
        String fuelQuantity = doc.getString("fuelQuantity");
        String fuelUnit = doc.getString("fuelQuantityUnit");
        Double estimatedPrice = doc.getDouble("estimatedPrice");
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
                        "\nOrder date: " + formatRequestDate(doc) +
                        "\nVehicle: " + safe(vehicle) +
                        "\nIssue: " + safe(issue) +
                        "\nFuel type: " + safe(fuelType) +
                        "\nFuel quantity: " + safe(fuelQuantity) + " " + safe(fuelUnit) +
                        "\nEstimated price: " + (estimatedPrice == null ? "Not available" : String.format(java.util.Locale.getDefault(), "₹%.2f", estimatedPrice)) +
                        "\nLocation: " + safe(location) +
                        "\nDescription: " + safe(description) +
                        "\nTechnician assigned: " +
                        (assigned ? "Yes" : "No")
        );

        details.setTextSize(14);
        details.setTextColor(Color.rgb(55, 65, 80));
        details.setLineSpacing(dp(3), 1.0f);

        card.addView(details);

        // Show assigned towing/technician contact details when available.
        // ACTION_DIAL opens the phone app without requiring CALL_PHONE permission.
        String technicianName = doc.getString("technicianName");
        String technicianPhone = doc.getString("technicianPhone");
        String technicianType = doc.getString("technicianType");
        String technicianRating = doc.getString("technicianRating");
        boolean serviceAssigned = assigned
                || "assigned".equalsIgnoreCase(status)
                || "in progress".equalsIgnoreCase(status)
                || "in_progress".equalsIgnoreCase(status);

        if (serviceAssigned) {
            TextView contactHeading = new TextView(this);
            contactHeading.setText("Assigned Service Contact");
            contactHeading.setTextSize(16);
            contactHeading.setTypeface(null, Typeface.BOLD);
            contactHeading.setTextColor(Color.rgb(28, 59, 104));
            contactHeading.setPadding(0, dp(14), 0, dp(6));
            card.addView(contactHeading);

            TextView contactDetails = new TextView(this);
            StringBuilder contactText = new StringBuilder();
            contactText.append("Service: ").append(safe(technicianName));
            if (technicianType != null && !technicianType.trim().isEmpty()) {
                contactText.append("\nService type: ").append(technicianType.trim());
            }
            if (technicianRating != null && !technicianRating.trim().isEmpty()) {
                contactText.append("\nRating: ").append(technicianRating.trim());
            }
            if (technicianPhone != null && !technicianPhone.trim().isEmpty()) {
                contactText.append("\nPhone: ").append(technicianPhone.trim());
            } else {
                contactText.append("\nContact number: Not available yet");
            }
            contactDetails.setText(contactText.toString());
            contactDetails.setTextSize(14);
            contactDetails.setTextColor(Color.rgb(55, 65, 80));
            contactDetails.setLineSpacing(dp(3), 1.0f);
            card.addView(contactDetails);

            if (technicianPhone != null && !technicianPhone.trim().isEmpty()) {
                Button callButton = new Button(this);
                callButton.setText("Call Assigned Service");
                callButton.setAllCaps(false);
                card.addView(callButton);
                callButton.setOnClickListener(v ->
                        contactTowingService(technicianPhone)
                );
            } else {
                TextView noPhoneNote = new TextView(this);
                noPhoneNote.setText(
                        "Calling is unavailable because this demo service has no phone number saved."
                );
                noPhoneNote.setTextSize(12);
                noPhoneNote.setTextColor(Color.rgb(120, 80, 40));
                noPhoneNote.setPadding(0, dp(4), 0, dp(4));
                card.addView(noPhoneNote);
            }
        }

        // Request Again is available for previous orders of any status.
        // It opens the new-request form and passes the previous order details.
        Button requestAgain = new Button(this);
        requestAgain.setText("Request Again");
        requestAgain.setAllCaps(false);
        card.addView(requestAgain);
        requestAgain.setOnClickListener(v -> requestAgain(doc));

        if (editable) {

            Button edit = new Button(this);
            edit.setText("Edit Request");
            edit.setAllCaps(false);
            card.addView(edit);
            edit.setOnClickListener(v -> editRequest(doc));

            Button cancel = new Button(this);
            cancel.setText("Cancel Request");
            cancel.setAllCaps(false);
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


    // =====================================
    // CONTACT ASSIGNED TOWING SERVICE
    // =====================================
    private void contactTowingService(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Towing service phone number is unavailable.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String dialNumber = phoneNumber.trim();

        Intent dialIntent = new Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + Uri.encode(dialNumber))
        );

        try {
            // ACTION_DIAL does not require CALL_PHONE permission.
            if (dialIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(dialIntent);
                return;
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            // Show the fallback below if the device cannot open a dialer.
        }

        // Some emulators/tablets do not have a dialer app. Let the user copy
        // the saved number instead of showing only an error message.
        EditText phoneInput = new EditText(this);
        phoneInput.setText(dialNumber);
        phoneInput.setSingleLine(true);
        phoneInput.setSelectAllOnFocus(true);
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setFocusable(false);
        phoneInput.setTextIsSelectable(true);

        new AlertDialog.Builder(this)
                .setTitle("Dialer Not Available")
                .setMessage(
                        "This device has no phone dialer installed. " +
                                "You can copy the towing service number and call it " +
                                "from a phone that supports calls."
                )
                .setView(phoneInput)
                .setPositiveButton("Copy Number", (dialog, which) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager)
                                    getSystemService(CLIPBOARD_SERVICE);

                    if (clipboard != null) {
                        android.content.ClipData clip =
                                android.content.ClipData.newPlainText(
                                        "Towing service phone number",
                                        dialNumber
                                );
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(
                                MyRequestsActivity.this,
                                "Phone number copied.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                MyRequestsActivity.this,
                                "Could not copy the number. Number: " + dialNumber,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // =====================================
    // REQUEST AGAIN: PASS PREVIOUS ORDER AS PREFILL DATA
    // =====================================
    private void requestAgain(DocumentSnapshot doc) {
        Intent intent = new Intent(MyRequestsActivity.this, BatteryHelpActivity.class);
        intent.putExtra("prefillRequestAgain", true);
        intent.putExtra("prefillServiceType", safe(doc.getString("serviceType")));
        intent.putExtra("prefillVehicleType", safe(doc.getString("vehicleType")));
        intent.putExtra("prefillBatteryIssue", safe(doc.getString("batteryIssue")));
        intent.putExtra("prefillFuelType", safe(doc.getString("fuelType")));
        intent.putExtra("prefillFuelQuantity", safe(doc.getString("fuelQuantity")));
        intent.putExtra("prefillFuelQuantityUnit", safe(doc.getString("fuelQuantityUnit")));
        intent.putExtra("prefillLocation", safe(doc.getString("location")));
        intent.putExtra("prefillDescription", safe(doc.getString("description")));

        Double previousPrice = doc.getDouble("estimatedPrice");
        if (previousPrice != null) {
            intent.putExtra("prefillEstimatedPrice", previousPrice);
        }

        startActivity(intent);
    }

    private int statusColor(String status) {

        if (status == null) {
            return Color.DKGRAY;
        }

        switch (status.trim().toLowerCase()
                .replace("_", " ")
                .replace("-", " ")) {

            case "pending":
                return Color.rgb(190, 115, 0);

            case "assigned":
                return Color.rgb(120, 70, 170);

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

        if (normalized.equals("assigned")) {
            return "Assigned";
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

        Spinner fuelTypeSpinner = new Spinner(this);
        String[] fuelTypes = {"Petrol", "Diesel", "CNG", "EV Charging"};
        fuelTypeSpinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, fuelTypes));
        String currentFuelType = doc.getString("fuelType");
        for (int i = 0; i < fuelTypes.length; i++) {
            if (fuelTypes[i].equals(currentFuelType)) {
                fuelTypeSpinner.setSelection(i);
                break;
            }
        }

        Spinner quantitySpinner = new Spinner(this);
        String[] quantities = {"2", "5", "10", "15"};
        quantitySpinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, quantities));
        String currentQuantity = doc.getString("fuelQuantity");
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i].equals(currentQuantity)) {
                quantitySpinner.setSelection(i);
                break;
            }
        }

        TextView pricePreview = new TextView(this);
        pricePreview.setTextSize(16);
        pricePreview.setTypeface(null, Typeface.BOLD);
        pricePreview.setTextColor(Color.rgb(35, 86, 190));
        pricePreview.setPadding(0, dp(8), 0, dp(8));

        Runnable refreshPrice = () -> {
            String selectedFuel = fuelTypeSpinner.getSelectedItem().toString();
            double quantity = Double.parseDouble(quantitySpinner.getSelectedItem().toString());
            double rate = getFuelRate(selectedFuel);
            pricePreview.setText(String.format(
                    java.util.Locale.getDefault(), "Estimated price: ₹%.2f", rate * quantity));
        };
        AdapterView.OnItemSelectedListener priceListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshPrice.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                refreshPrice.run();
            }
        };
        fuelTypeSpinner.setOnItemSelectedListener(priceListener);
        quantitySpinner.setOnItemSelectedListener(priceListener);
        refreshPrice.run();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);

        form.addView(vehicleSpinner);
        form.addView(issueSpinner);
        form.addView(labelFor("Fuel type"));
        form.addView(fuelTypeSpinner);
        form.addView(labelFor("Fuel quantity"));
        form.addView(quantitySpinner);
        form.addView(pricePreview);
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

                    String selectedFuel = fuelTypeSpinner.getSelectedItem().toString();
                    String selectedQuantity = quantitySpinner.getSelectedItem().toString();
                    String selectedUnit;
                    if ("CNG".equals(selectedFuel)) {
                        selectedUnit = "kg";
                    } else if ("EV Charging".equals(selectedFuel)) {
                        selectedUnit = "kWh";
                    } else {
                        selectedUnit = "litres";
                    }
                    double updatedPrice = getFuelRate(selectedFuel)
                            * Double.parseDouble(selectedQuantity);

                    updates.put("fuelType", selectedFuel);
                    updates.put("fuelQuantity", selectedQuantity);
                    updates.put("fuelQuantityUnit", selectedUnit);
                    updates.put("estimatedPrice", updatedPrice);
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
    // FUEL PRICE HELPERS
    // =====================================
    private double getFuelRate(String fuelType) {
        switch (fuelType) {
            case "Petrol": return 95.0;
            case "Diesel": return 91.0;
            case "CNG": return 80.0;
            case "EV Charging": return 15.0;
            default: return 0.0;
        }
    }

    private TextView labelFor(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(Color.rgb(35, 55, 85));
        label.setPadding(0, dp(8), 0, dp(4));
        return label;
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

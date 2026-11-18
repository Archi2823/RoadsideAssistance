
package com.archi.roadsideassistance;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyBookingsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout bookingsContainer;
    private TextView tvBookingMessage;

    private Button btnUpcoming;
    private Button btnPast;

    private final List<BookingItem> allBookings = new ArrayList<>();

    private boolean showUpcoming = true;

    private final SimpleDateFormat inputFormat =
            new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private final SimpleDateFormat displayTimeFormat =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFormat.setLenient(false);

        bookingsContainer = findViewById(R.id.bookingsContainer);
        tvBookingMessage = findViewById(R.id.tvBookingMessage);

        btnUpcoming = findViewById(R.id.btnUpcoming);
        btnPast = findViewById(R.id.btnPast);

        Button btnBookingBack = findViewById(R.id.btnBookingBack);

        btnUpcoming.setOnClickListener(v -> {
            showUpcoming = true;
            updateTabButtons();
            displayBookings();
        });

        btnPast.setOnClickListener(v -> {
            showUpcoming = false;
            updateTabButtons();
            displayBookings();
        });

        btnBookingBack.setOnClickListener(v -> finish());

        updateTabButtons();
        loadBookings();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (db != null && auth != null && auth.getCurrentUser() != null) {
            loadBookings();
        }
    }

    private void loadBookings() {

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        tvBookingMessage.setVisibility(View.VISIBLE);
        tvBookingMessage.setText("Loading your bookings...");
        bookingsContainer.removeAllViews();

        String userId = auth.getCurrentUser().getUid();

        db.collection("serviceRequests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (isFinishing() || isDestroyed()) return;

                    allBookings.clear();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {

                        String preferredDate =
                                document.getString("preferredDate");

                        String preferredTime =
                                document.getString("preferredTime");

                        if (preferredDate == null || preferredTime == null
                                || preferredDate.trim().isEmpty()
                                || preferredTime.trim().isEmpty()) {
                            continue;
                        }

                        try {
                            Date appointmentDate = inputFormat.parse(
                                    preferredDate + " " + preferredTime
                            );

                            if (appointmentDate == null) continue;

                            BookingItem item = new BookingItem();

                            item.documentId = document.getId();

                            item.serviceType = getText(
                                    document, "serviceType", "Scheduled Service");

                            item.vehicleType = getText(
                                    document, "vehicleType", "Not specified");

                            item.location = getText(
                                    document, "location", "Not specified");

                            item.status = getText(
                                    document, "status", "pending");

                            item.technicianName = getText(
                                    document, "technicianName", "");

                            item.technicianPhone = getText(
                                    document, "technicianPhone", "");

                            Boolean assigned =
                                    document.getBoolean("technicianAssigned");

                            item.technicianAssigned =
                                    assigned != null && assigned;

                            item.preferredDate = preferredDate;
                            item.preferredTime = preferredTime;
                            item.appointmentDate = appointmentDate;

                            allBookings.add(item);

                        } catch (ParseException e) {
                            // Ignore invalid appointment date/time.
                        }
                    }

                    displayBookings();

                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) return;

                    tvBookingMessage.setVisibility(View.VISIBLE);
                    tvBookingMessage.setText(
                            "Unable to load bookings. Please try again."
                    );

                    Toast.makeText(
                            MyBookingsActivity.this,
                            "Error: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String getText(
            DocumentSnapshot document,
            String field,
            String defaultValue) {

        String value = document.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private void displayBookings() {

        bookingsContainer.removeAllViews();

        Date now = new Date();

        List<BookingItem> filteredBookings = new ArrayList<>();

        for (BookingItem item : allBookings) {

            String status = item.status.toLowerCase(Locale.ROOT);

            boolean completedOrClosed =
                    status.equals("resolved")
                            || status.equals("completed")
                            || status.equals("cancelled")
                            || status.equals("closed");

            boolean isPast =
                    item.appointmentDate.before(now)
                            || completedOrClosed;

            if (showUpcoming && !isPast) {
                filteredBookings.add(item);
            } else if (!showUpcoming && isPast) {
                filteredBookings.add(item);
            }
        }

        if (showUpcoming) {
            Collections.sort(
                    filteredBookings,
                    Comparator.comparing(item -> item.appointmentDate)
            );
        } else {
            filteredBookings.sort(
                    (a, b) -> b.appointmentDate.compareTo(a.appointmentDate)
            );
        }

        if (filteredBookings.isEmpty()) {

            tvBookingMessage.setVisibility(View.VISIBLE);

            tvBookingMessage.setText(
                    showUpcoming
                            ? "No upcoming bookings found."
                            : "No past bookings found."
            );

            return;
        }

        tvBookingMessage.setVisibility(View.GONE);

        for (BookingItem item : filteredBookings) {
            addBookingCard(item);
        }
    }

    private void addBookingCard(BookingItem item) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), Color.rgb(225, 232, 241));

        card.setBackground(background);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        TextView tvService = createText(
                item.serviceType,
                19,
                Color.rgb(23, 59, 108),
                true
        );

        card.addView(tvService);

        TextView tvStatus = createText(
                "Status: " + capitalize(item.status),
                14,
                getStatusColor(item.status),
                true
        );

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statusParams.topMargin = dp(8);
        statusParams.bottomMargin = dp(12);

        tvStatus.setLayoutParams(statusParams);
        card.addView(tvStatus);

        addDetail(card, "Date",
                displayDateFormat.format(item.appointmentDate));

        addDetail(card, "Time",
                displayTimeFormat.format(item.appointmentDate));

        addDetail(card, "Vehicle", item.vehicleType);

        addDetail(card, "Location", item.location);

        if (!item.technicianName.isEmpty()) {
            addDetail(card, "Assigned Technician", item.technicianName);
        }

        if (!item.technicianPhone.isEmpty()) {
            addDetail(card, "Technician Phone", item.technicianPhone);
        }

        // Actions are available only while the booking is still pending
        // and no worker has been assigned.
        if (isModifiable(item)) {

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);
            actionRow.setWeightSum(2);

            LinearLayout.LayoutParams rowParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );

            rowParams.topMargin = dp(10);
            actionRow.setLayoutParams(rowParams);

            Button btnReschedule = new Button(this);
            btnReschedule.setText("Reschedule");
            btnReschedule.setAllCaps(false);
            btnReschedule.setTextColor(Color.WHITE);
            btnReschedule.setTextSize(13);
            btnReschedule.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(23, 59, 108))
            );

            LinearLayout.LayoutParams rescheduleParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(48),
                            1
                    );

            rescheduleParams.setMargins(0, 0, dp(5), 0);
            btnReschedule.setLayoutParams(rescheduleParams);

            btnReschedule.setOnClickListener(v ->
                    showRescheduleDialog(item));

            Button btnCancel = new Button(this);
            btnCancel.setText("Cancel Booking");
            btnCancel.setAllCaps(false);
            btnCancel.setTextColor(Color.WHITE);
            btnCancel.setTextSize(13);
            btnCancel.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(190, 55, 55))
            );

            LinearLayout.LayoutParams cancelParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(48),
                            1
                    );

            cancelParams.setMargins(dp(5), 0, 0, 0);
            btnCancel.setLayoutParams(cancelParams);

            btnCancel.setOnClickListener(v ->
                    showCancelConfirmation(item));

            actionRow.addView(btnReschedule);
            actionRow.addView(btnCancel);

            card.addView(actionRow);
        }

        bookingsContainer.addView(card);
    }

    private boolean isModifiable(BookingItem item) {

        return showUpcoming
                && "pending".equalsIgnoreCase(item.status)
                && !item.technicianAssigned;
    }

    private void showCancelConfirmation(BookingItem item) {

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage(
                        "Are you sure you want to cancel your "
                                + item.serviceType + " booking?"
                )
                .setNegativeButton("No", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Yes, Cancel", (dialog, which) ->
                        cancelBooking(item))
                .show();
    }

    private void cancelBooking(BookingItem item) {

        if (!isModifiable(item)) {
            showBookingLockedMessage();
            loadBookings();
            return;
        }

        db.collection("serviceRequests")
                .document(item.documentId)
                .update("status", "cancelled")
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Booking cancelled successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadBookings();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to cancel booking. The worker may have accepted it.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadBookings();
                });
    }

    private void showRescheduleDialog(BookingItem item) {

        if (!isModifiable(item)) {
            showBookingLockedMessage();
            loadBookings();
            return;
        }

        Calendar current = Calendar.getInstance();

        try {
            Date existingDate = inputFormat.parse(
                    item.preferredDate + " " + item.preferredTime
            );

            if (existingDate != null) {
                current.setTime(existingDate);
            }
        } catch (ParseException ignored) {
        }

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, day) -> {

                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, day);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (selectedDate.before(today)) {
                        Toast.makeText(
                                this,
                                "Please select today or a future date.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    showTimePicker(item, selectedDate);
                },
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH)
        );

        dateDialog.getDatePicker().setMinDate(
                System.currentTimeMillis() - 1000
        );

        dateDialog.setTitle("Select New Date");
        dateDialog.show();
    }

    private void showTimePicker(
            BookingItem item,
            Calendar selectedDate) {

        Calendar current = Calendar.getInstance();

        int hour = current.get(Calendar.HOUR_OF_DAY);
        int minute = current.get(Calendar.MINUTE);

        TimePickerDialog timeDialog = new TimePickerDialog(
                this,
                (timePicker, selectedHour, selectedMinute) -> {

                    Calendar selectedDateTime =
                            (Calendar) selectedDate.clone();

                    selectedDateTime.set(
                            Calendar.HOUR_OF_DAY, selectedHour);

                    selectedDateTime.set(
                            Calendar.MINUTE, selectedMinute);

                    selectedDateTime.set(
                            Calendar.SECOND, 0);

                    selectedDateTime.set(
                            Calendar.MILLISECOND, 0);

                    if (selectedDateTime.getTime().before(new Date())) {
                        Toast.makeText(
                                this,
                                "Please select a future time.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String newDate = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    ).format(selectedDateTime.getTime());

                    String newTime = new SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    ).format(selectedDateTime.getTime());

                    confirmReschedule(item, newDate, newTime);
                },
                hour,
                minute,
                false
        );

        timeDialog.setTitle("Select New Time");
        timeDialog.show();
    }

    private void confirmReschedule(
            BookingItem item,
            String newDate,
            String newTime) {

        String readableDate;

        try {
            Date parsedDate = inputFormat.parse(newDate + " " + newTime);
            readableDate = displayDateFormat.format(parsedDate);
        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Invalid date or time.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Reschedule")
                .setMessage(
                        "New date: " + readableDate
                                + "\nNew time: " + newTime
                                + "\n\nDo you want to update this booking?"
                )
                .setNegativeButton("Back", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Confirm", (dialog, which) ->
                        updateBookingDateTime(item, newDate, newTime))
                .show();
    }

    private void updateBookingDateTime(
            BookingItem item,
            String newDate,
            String newTime) {

        if (!isModifiable(item)) {
            showBookingLockedMessage();
            loadBookings();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("preferredDate", newDate);
        updates.put("preferredTime", newTime);

        db.collection("serviceRequests")
                .document(item.documentId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Booking rescheduled successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    loadBookings();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to reschedule. The worker may have accepted the booking.",
                            Toast.LENGTH_LONG
                    ).show();

                    loadBookings();
                });
    }

    private void showBookingLockedMessage() {

        Toast.makeText(
                this,
                "This booking can no longer be changed because a worker has accepted it.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void addDetail(
            LinearLayout parent,
            String label,
            String value) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(10);
        row.setLayoutParams(params);

        TextView tvLabel = createText(
                label,
                12,
                Color.GRAY,
                false
        );

        TextView tvValue = createText(
                value,
                15,
                Color.rgb(45, 52, 65),
                true
        );

        row.addView(tvLabel);
        row.addView(tvValue);

        parent.addView(row);
    }

    private TextView createText(
            String value,
            int size,
            int color,
            boolean bold) {

        TextView textView = new TextView(this);

        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(color);

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        return textView;
    }

    private int getStatusColor(String status) {

        switch (status.toLowerCase(Locale.ROOT)) {

            case "resolved":
            case "completed":
            case "closed":
                return Color.rgb(39, 139, 83);

            case "cancelled":
                return Color.rgb(210, 65, 65);

            case "assigned":
            case "in progress":
                return Color.rgb(35, 105, 190);

            default:
                return Color.rgb(210, 135, 35);
        }
    }

    private String capitalize(String value) {

        if (value == null || value.isEmpty()) return "Pending";

        String result = value.replace("_", " ");

        return result.substring(0, 1).toUpperCase(Locale.ROOT)
                + result.substring(1);
    }

    private void updateTabButtons() {

        if (showUpcoming) {

            btnUpcoming.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(23, 59, 108))
            );

            btnUpcoming.setTextColor(Color.WHITE);

            btnPast.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(230, 235, 243))
            );

            btnPast.setTextColor(Color.rgb(45, 52, 65));

        } else {

            btnPast.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(23, 59, 108))
            );

            btnPast.setTextColor(Color.WHITE);

            btnUpcoming.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.rgb(230, 235, 243))
            );

            btnUpcoming.setTextColor(Color.rgb(45, 52, 65));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private static class BookingItem {

        String documentId;
        String serviceType;
        String vehicleType;
        String location;
        String status;
        String technicianName;
        String technicianPhone;
        String preferredDate;
        String preferredTime;
        Date appointmentDate;
        boolean technicianAssigned;
    }
}

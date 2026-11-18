
package com.archi.roadsideassistance;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private void loadBookings() {

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

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

                        // Scheduled bookings have both appointment fields.
                        // This also supports future scheduled service types.
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

                            item.preferredDate = preferredDate;
                            item.preferredTime = preferredTime;
                            item.appointmentDate = appointmentDate;

                            allBookings.add(item);

                        } catch (ParseException e) {
                            // Ignore bookings with an invalid date/time format.
                        }
                    }

                    displayBookings();

                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) return;

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
            addDetail(card, "Assigned Technician",
                    item.technicianName);
        }

        if (!item.technicianPhone.isEmpty()) {
            addDetail(card, "Technician Phone",
                    item.technicianPhone);
        }

        bookingsContainer.addView(card);
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

        String serviceType;
        String vehicleType;
        String location;
        String status;
        String technicianName;
        String technicianPhone;
        String preferredDate;
        String preferredTime;
        Date appointmentDate;
    }
}

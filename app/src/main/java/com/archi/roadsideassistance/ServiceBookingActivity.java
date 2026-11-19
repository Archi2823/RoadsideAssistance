
package com.archi.roadsideassistance;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ServiceBookingActivity extends AppCompatActivity {

    private TextView tvSelectedService;
    private TextView tvSelectedDate;
    private TextView tvSelectedTime;
    private TextView tvBookingMessage;

    private Spinner spVehicleType;

    private EditText etBookingLocation;
    private EditText etBookingDescription;
    private EditText etBookingNotes;

    private Button btnSelectDate;
    private Button btnSelectTime;
    private Button btnConfirmBooking;
    private Button btnBookingBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String serviceType;
    private String preferredDate = "";
    private String preferredTime = "";

    private Calendar selectedCalendar;

    private boolean submitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_booking);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        serviceType = getIntent().getStringExtra("serviceType");

        if (serviceType == null ||
                !(serviceType.equals("General Service")
                        || serviceType.equals("Annual Service")
                        || serviceType.equals("Inspection"))) {

            Toast.makeText(
                    this,
                    "Invalid service selected",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        tvSelectedService = findViewById(R.id.tvSelectedService);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        tvBookingMessage = findViewById(R.id.tvBookingMessage);

        spVehicleType = findViewById(R.id.spBookingVehicleType);

        etBookingLocation = findViewById(R.id.etBookingLocation);
        etBookingDescription = findViewById(R.id.etBookingDescription);
        etBookingNotes = findViewById(R.id.etBookingNotes);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnBookingBack = findViewById(R.id.btnBookingBack);

        tvSelectedService.setText(serviceType);

        setupVehicleSpinner();

        selectedCalendar = Calendar.getInstance();

        btnSelectDate.setOnClickListener(v -> openDatePicker());

        btnSelectTime.setOnClickListener(v -> openTimePicker());

        btnConfirmBooking.setOnClickListener(v -> confirmBooking());

        btnBookingBack.setOnClickListener(v -> finish());
    }

    private void setupVehicleSpinner() {

        String[] vehicleTypes = {
                "Select vehicle type",
                "Bike",
                "Car",
                "SUV",
                "Truck",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleTypes
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spVehicleType.setAdapter(adapter);
    }

    private void openDatePicker() {

        Calendar today = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    selectedCalendar.set(
                            year,
                            month,
                            dayOfMonth
                    );

                    selectedCalendar.set(Calendar.SECOND, 0);

                    preferredDate = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    ).format(selectedCalendar.getTime());

                    tvSelectedDate.setText(
                            new SimpleDateFormat(
                                    "dd MMM yyyy",
                                    Locale.getDefault()
                            ).format(selectedCalendar.getTime())
                    );

                    // Reset previously selected time
                    preferredTime = "";
                    tvSelectedTime.setText("No time selected");

                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMinDate(
                today.getTimeInMillis()
        );

        datePicker.show();
    }

    private void openTimePicker() {

        if (preferredDate.isEmpty()) {
            tvBookingMessage.setText(
                    "Please select a booking date first."
            );
            return;
        }

        Calendar now = Calendar.getInstance();

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {

                    Calendar chosenTime = Calendar.getInstance();

                    chosenTime.set(
                            selectedCalendar.get(Calendar.YEAR),
                            selectedCalendar.get(Calendar.MONTH),
                            selectedCalendar.get(Calendar.DAY_OF_MONTH),
                            hourOfDay,
                            minute,
                            0
                    );

                    // Prevent selecting a past time
                    if (chosenTime.before(Calendar.getInstance())) {
                        tvBookingMessage.setText(
                                "Please select a future time."
                        );
                        return;
                    }

                    selectedCalendar.set(
                            Calendar.HOUR_OF_DAY,
                            hourOfDay
                    );

                    selectedCalendar.set(
                            Calendar.MINUTE,
                            minute
                    );

                    selectedCalendar.set(
                            Calendar.SECOND,
                            0
                    );

                    preferredTime = new SimpleDateFormat(
                            "hh:mm a",
                            Locale.getDefault()
                    ).format(selectedCalendar.getTime());

                    tvSelectedTime.setText(preferredTime);

                    tvBookingMessage.setText("");

                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );

        timePicker.show();
    }

    private void confirmBooking() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvBookingMessage.setText(
                    "Please log in before booking."
            );
            return;
        }

        if (spVehicleType.getSelectedItemPosition() == 0) {
            tvBookingMessage.setText(
                    "Please select your vehicle type."
            );
            return;
        }

        if (preferredDate.isEmpty()) {
            tvBookingMessage.setText(
                    "Please select your preferred date."
            );
            return;
        }

        if (preferredTime.isEmpty()) {
            tvBookingMessage.setText(
                    "Please select your preferred time."
            );
            return;
        }

        String location = etBookingLocation.getText()
                .toString().trim();

        if (location.isEmpty()) {
            etBookingLocation.setError("Location is required");
            etBookingLocation.requestFocus();
            return;
        }

        String description = etBookingDescription.getText()
                .toString().trim();

        String notes = etBookingNotes.getText()
                .toString().trim();

        String vehicleType =
                spVehicleType.getSelectedItem().toString();

        new AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage(
                        "Service: " + serviceType +
                                "\nVehicle: " + vehicleType +
                                "\nDate: " + preferredDate +
                                "\nTime: " + preferredTime +
                                "\nLocation: " + location +
                                "\nNotes for mechanic: " +
                                (notes.isEmpty() ? "None" : notes) +
                                "\n\nDo you want to confirm this booking?"
                )
                .setNegativeButton("Review", (dialog, which) ->
                        dialog.dismiss())
                .setPositiveButton("Confirm", (dialog, which) ->
                        saveBooking(
                                user,
                                vehicleType,
                                location,
                                description,
                                notes
                        ))
                .show();
    }

    private void saveBooking(
            FirebaseUser user,
            String vehicleType,
            String location,
            String description,
            String notes
    ) {

        if (submitting) {
            return;
        }

        submitting = true;

        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Saving Booking...");
        tvBookingMessage.setText("");

        String category = "Mechanical Assistance";

        Map<String, Object> request = new HashMap<>();

        request.put("userId", user.getUid());

        request.put(
                "userEmail",
                user.getEmail() == null ? "" : user.getEmail()
        );

        request.put("serviceType", serviceType);
        request.put("serviceCategory", category);

        request.put("vehicleType", vehicleType);

        request.put("preferredDate", preferredDate);
        request.put("preferredTime", preferredTime);

        request.put("location", location);
        request.put("description", description);
        request.put("notes", notes);

        request.put("status", "pending");

        request.put("technicianAssigned", false);
        request.put("technicianName", "");
        request.put("technicianPhone", "");
        request.put("technicianRating", "");
        request.put("technicianType", "");

        request.put("createdAt", FieldValue.serverTimestamp());

        db.collection("serviceRequests")
                .add(request)
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String requestId = document.getId();

                    tvBookingMessage.setText(
                            "Booking saved. Finding an approved mechanic..."
                    );

                    WorkerAssignmentHelper.assignRequest(
                            requestId,
                            category,
                            user.getUid(),
                            new WorkerAssignmentHelper.AssignmentCallback() {

                                @Override
                                public void onSuccess(String workerName) {

                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    // Fetch assigned worker's charges
                                    fetchAssignedWorkerCharges(
                                            requestId,
                                            workerName,
                                            vehicleType
                                    );
                                }

                                @Override
                                public void onNoWorkerAvailable() {

                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    tvBookingMessage.setText(
                                            "Booking saved successfully, but no approved " +
                                                    "mechanic is currently available. " +
                                                    "Your request remains pending."
                                    );

                                    Toast.makeText(
                                            ServiceBookingActivity.this,
                                            "Booking saved. No mechanic available right now.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    btnConfirmBooking.setText("Booking Saved");
                                }

                                @Override
                                public void onFailure(String message) {

                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    tvBookingMessage.setText(
                                            "Booking saved, but assignment failed: "
                                                    + message
                                    );

                                    btnConfirmBooking.setText("Booking Saved");
                                }
                            }
                    );
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    submitting = false;

                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Confirm Booking");

                    tvBookingMessage.setText(
                            "Booking could not be saved: "
                                    + e.getLocalizedMessage()
                    );
                });
    }

    // --------------------------------------------------
    // Fetch the assigned worker's service charge
    // --------------------------------------------------

    private void fetchAssignedWorkerCharges(
            String requestId,
            String workerName,
            String vehicleType
    ) {

        tvBookingMessage.setText(
                "Worker assigned. Fetching service charges..."
        );

        // Read actual assigned worker UID from booking document
        db.collection("serviceRequests")
                .document(requestId)
                .get()
                .addOnSuccessListener(requestDocument -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!requestDocument.exists()) {
                        showWorkerBookingResult(
                                workerName,
                                "Price unavailable",
                                "Booking saved, but request details could not be read."
                        );
                        return;
                    }

                    String assignedWorkerId =
                            requestDocument.getString("assignedWorkerId");

                    if (assignedWorkerId == null
                            || assignedWorkerId.trim().isEmpty()) {

                        showWorkerBookingResult(
                                workerName,
                                "Price unavailable",
                                "The assigned worker ID was not found."
                        );
                        return;
                    }

                    // Read charges from this specific worker
                    db.collection("serviceCharges")
                            .document(assignedWorkerId)
                            .collection("vehicleCharges")
                            .document(vehicleType)
                            .get()
                            .addOnSuccessListener(chargeDocument -> {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                if (!chargeDocument.exists()) {

                                    showWorkerBookingResult(
                                            workerName,
                                            "Price not set by worker",
                                            "The worker has not set charges for "
                                                    + vehicleType + "."
                                    );

                                    return;
                                }

                                String chargeField =
                                        getChargeFieldForService(serviceType);

                                Object chargeValue =
                                        chargeDocument.get(chargeField);

                                if (chargeValue == null) {

                                    showWorkerBookingResult(
                                            workerName,
                                            "Price not set by worker",
                                            "The worker has not set a price for "
                                                    + serviceType + "."
                                    );

                                    return;
                                }

                                String formattedPrice =
                                        formatPrice(chargeValue);

                                showWorkerBookingResult(
                                        workerName,
                                        formattedPrice,
                                        "Your booking has been assigned successfully."
                                );
                            })
                            .addOnFailureListener(e -> {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                showWorkerBookingResult(
                                        workerName,
                                        "Price unavailable",
                                        "Unable to load worker charges."
                                );
                            });
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    showWorkerBookingResult(
                            workerName,
                            "Price unavailable",
                            "Unable to read assigned worker details."
                    );
                });
    }

    // Map selected service to Firestore price field
    private String getChargeFieldForService(String selectedService) {

        if ("General Service".equals(selectedService)) {
            return "generalServiceCharge";
        }

        if ("Annual Service".equals(selectedService)) {
            return "annualServiceCharge";
        }

        if ("Inspection".equals(selectedService)) {
            return "inspectionCharge";
        }

        return "";
    }

    // Format price for display
    private String formatPrice(Object priceValue) {

        if (priceValue instanceof Number) {

            double amount = ((Number) priceValue).doubleValue();

            return String.format(
                    Locale.getDefault(),
                    "₹%.2f",
                    amount
            );
        }

        return "₹" + String.valueOf(priceValue);
    }

    // Show assigned worker and charge to customer
    private void showWorkerBookingResult(
            String workerName,
            String priceText,
            String message
    ) {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        String displayWorker =
                (workerName == null || workerName.trim().isEmpty())
                        ? "Assigned Mechanic"
                        : workerName;

        String bookingDetails =
                "Service: " + serviceType +
                        "\nWorker: " + displayWorker +
                        "\nVehicle: " +
                        spVehicleType.getSelectedItem().toString() +
                        "\nService Charge: " + priceText +
                        "\nDate: " + preferredDate +
                        "\nTime: " + preferredTime +
                        "\n\n" + message;

        tvBookingMessage.setText(
                "Booking assigned successfully."
        );

        new AlertDialog.Builder(this)
                .setTitle("Booking Assigned")
                .setMessage(bookingDetails)
                .setCancelable(false)
                .setPositiveButton("Done", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();

        Toast.makeText(
                this,
                "Booking assigned successfully!",
                Toast.LENGTH_SHORT
        ).show();
    }
}

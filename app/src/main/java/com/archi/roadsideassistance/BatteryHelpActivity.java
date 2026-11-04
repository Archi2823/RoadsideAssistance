package com.archi.roadsideassistance;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BatteryHelpActivity extends AppCompatActivity {

    private Spinner spVehicleType, spBatteryIssue, spFuelType, spFuelQuantity;
    private EditText etBatteryLocation, etBatteryDescription;
    private TextView tvBatteryMessage, tvFuelQuantityLabel, tvEstimatedPrice, tvPriceNote;
    private Button btnSubmitBatteryRequest;
    private LinearLayout tireSpecialistsContainer;
    private TextView tvTireSpecialistsMessage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final double PETROL_RATE = 95.0;
    private static final double DIESEL_RATE = 91.0;
    private static final double CNG_RATE = 80.0;
    private static final double EV_RATE = 15.0;
    private double estimatedPrice = 0.0;
    private boolean loadingTireSpecialists = false;

    private final String[] vehicleIssues = {
            "Select vehicle issue", "Engine Problem", "Brake Failure",
            "Battery Problem", "Flat Tyre", "Clutch Problem",
            "Steering Problem", "Overheating", "Fuel Problem",
            "Accident / Breakdown", "Vehicle not starting", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery_help);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spVehicleType = findViewById(R.id.spVehicleType);
        spBatteryIssue = findViewById(R.id.spBatteryIssue);
        spFuelType = findViewById(R.id.spFuelType);
        spFuelQuantity = findViewById(R.id.spFuelQuantity);
        etBatteryLocation = findViewById(R.id.etBatteryLocation);
        etBatteryDescription = findViewById(R.id.etBatteryDescription);
        tvBatteryMessage = findViewById(R.id.tvBatteryMessage);
        tvFuelQuantityLabel = findViewById(R.id.tvFuelQuantityLabel);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        tvPriceNote = findViewById(R.id.tvPriceNote);
        btnSubmitBatteryRequest = findViewById(R.id.btnSubmitBatteryRequest);
        tireSpecialistsContainer = findViewById(R.id.tireSpecialistsContainer);
        tvTireSpecialistsMessage = findViewById(R.id.tvTireSpecialistsMessage);

        setupSpinner(spVehicleType, new String[]{
                "Select vehicle type", "Bike", "Car", "SUV", "Truck", "Other"
        });
        setupSpinner(spBatteryIssue, vehicleIssues);
        setupSpinner(spFuelType, new String[]{
                "Select fuel type", "Petrol", "Diesel", "CNG", "EV Charging"
        });
        setupSpinner(spFuelQuantity, new String[]{
                "Select quantity", "2", "5", "10", "15"
        });

        spBatteryIssue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && "Flat Tyre".equals(spBatteryIssue.getSelectedItem().toString())) {
                    tireSpecialistsContainer.setVisibility(View.VISIBLE);
                    loadTireSpecialists();
                } else {
                    tireSpecialistsContainer.setVisibility(View.GONE);
                    tireSpecialistsContainer.removeAllViews();
                    loadingTireSpecialists = false;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                tireSpecialistsContainer.setVisibility(View.GONE);
            }
        });
        AdapterView.OnItemSelectedListener fuelListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateQuantityUnit();
                updateEstimatedPrice();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                updateEstimatedPrice();
            }
        };
        spFuelType.setOnItemSelectedListener(fuelListener);
        spFuelQuantity.setOnItemSelectedListener(fuelListener);

        btnSubmitBatteryRequest.setOnClickListener(v -> submitRequest());
        applyRequestAgainPrefill();
        updateQuantityUnit();
        updateEstimatedPrice();
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void applyRequestAgainPrefill() {
        if (!getIntent().getBooleanExtra("prefillRequestAgain", false)) return;
        selectSpinnerValue(spVehicleType, getIntent().getStringExtra("prefillVehicleType"));
        String issue = getIntent().getStringExtra("prefillVehicleIssue");
        if (issue == null || issue.trim().isEmpty())
            issue = getIntent().getStringExtra("prefillBatteryIssue");
        selectSpinnerValue(spBatteryIssue, issue);
        selectSpinnerValue(spFuelType, getIntent().getStringExtra("prefillFuelType"));
        selectSpinnerValue(spFuelQuantity, getIntent().getStringExtra("prefillFuelQuantity"));

        String location = getIntent().getStringExtra("prefillLocation");
        if (location != null && !location.trim().isEmpty() && !"-".equals(location.trim()))
            etBatteryLocation.setText(location);
        String description = getIntent().getStringExtra("prefillDescription");
        if (description != null && !"-".equals(description))
            etBatteryDescription.setText(description);
        tvBatteryMessage.setText("Previous request details loaded. Review and submit to create a new request.");
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        if (spinner == null || value == null || value.trim().isEmpty() || "-".equals(value.trim())) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item != null && value.trim().equalsIgnoreCase(item.toString().trim())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private double getFuelRate(String fuelType) {
        switch (fuelType) {
            case "Petrol": return PETROL_RATE;
            case "Diesel": return DIESEL_RATE;
            case "CNG": return CNG_RATE;
            case "EV Charging": return EV_RATE;
            default: return 0.0;
        }
    }

    private void updateQuantityUnit() {
        if (spFuelType == null || tvFuelQuantityLabel == null || spFuelType.getSelectedItem() == null) return;
        String type = spFuelType.getSelectedItem().toString();
        if ("CNG".equals(type)) tvFuelQuantityLabel.setText("Fuel Quantity (kg) *");
        else if ("EV Charging".equals(type)) tvFuelQuantityLabel.setText("Charging Quantity (kWh) *");
        else tvFuelQuantityLabel.setText("Fuel Quantity (litres) *");
    }

    private void updateEstimatedPrice() {
        if (spFuelType == null || spFuelQuantity == null || tvEstimatedPrice == null) return;
        if (spFuelType.getSelectedItemPosition() == 0 || spFuelQuantity.getSelectedItemPosition() == 0) {
            estimatedPrice = 0.0;
            tvEstimatedPrice.setText("Select fuel type and quantity");
            return;
        }
        String fuel = spFuelType.getSelectedItem().toString();
        try {
            double quantity = Double.parseDouble(spFuelQuantity.getSelectedItem().toString());
            double rate = getFuelRate(fuel);
            estimatedPrice = quantity * rate;
            tvEstimatedPrice.setText(String.format(Locale.getDefault(), "₹%.2f", estimatedPrice));
            String unit = "CNG".equals(fuel) ? "kg" : ("EV Charging".equals(fuel) ? "kWh" : "litre");
            tvPriceNote.setText(String.format(Locale.getDefault(),
                    "Rate: ₹%.2f per %s. Illustrative estimate only.", rate, unit));
        } catch (NumberFormatException e) {
            estimatedPrice = 0.0;
            tvEstimatedPrice.setText("Unable to calculate price");
        }
    }

    // Show only approved workers whose saved category is exactly Tyre Assistance.
    private void loadTireSpecialists() {
        if (loadingTireSpecialists) return;
        loadingTireSpecialists = true;
        tireSpecialistsContainer.removeAllViews();
        addInfoText("Loading approved tire specialists...");

        db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .whereEqualTo("serviceCategory", "Tyre Assistance")
                .get()
                .addOnSuccessListener(snapshot -> {
                    loadingTireSpecialists = false;
                    tireSpecialistsContainer.removeAllViews();

                    if (spBatteryIssue.getSelectedItemPosition() == 0 ||
                            !"Flat Tyre".equals(spBatteryIssue.getSelectedItem().toString())) return;

                    if (snapshot.isEmpty()) {
                        addInfoText("No approved tire specialists are available right now.");
                        return;
                    }

                    for (DocumentSnapshot worker : snapshot.getDocuments()) {
                        addSpecialistCard(worker);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingTireSpecialists = false;
                    tireSpecialistsContainer.removeAllViews();
                    addInfoText("Could not load tire specialists. " +
                            (e.getLocalizedMessage() == null ? "" : e.getLocalizedMessage()));
                });
    }

    private void addInfoText(String message) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.rgb(101, 116, 139));
        text.setTextSize(13);
        text.setPadding(4, 12, 4, 12);
        tireSpecialistsContainer.addView(text);
    }

    private void addSpecialistCard(DocumentSnapshot worker) {
        String name = worker.getString("name");
        String phone = worker.getString("phone");
        if (name == null || name.trim().isEmpty()) name = "Tire Specialist";
        if (phone == null || phone.trim().isEmpty()) phone = "Phone number not provided";

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(224, 232, 243));
        card.setBackground(bg);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.rgb(23, 43, 77));
        nameView.setTextSize(16);
        nameView.setTypeface(null, Typeface.BOLD);
        card.addView(nameView);

        TextView categoryView = new TextView(this);
        categoryView.setText("Approved • Tyre Assistance");
        categoryView.setTextColor(Color.rgb(37, 99, 235));
        categoryView.setTextSize(12);
        categoryView.setPadding(0, dp(3), 0, dp(3));
        card.addView(categoryView);

        TextView phoneView = new TextView(this);
        phoneView.setText("Phone: " + phone);
        phoneView.setTextColor(Color.rgb(101, 116, 139));
        phoneView.setTextSize(13);
        card.addView(phoneView);

        TextView workloadView = new TextView(this);
        workloadView.setText("Checking current workload...");
        workloadView.setTextColor(Color.rgb(101, 116, 139));
        workloadView.setTextSize(12);
        card.addView(workloadView);

        tireSpecialistsContainer.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View spacer = new View(this);
        tireSpecialistsContainer.addView(spacer, new LinearLayout.LayoutParams(1, dp(8)));

        db.collection("workerStats").document(worker.getId()).get()
                .addOnSuccessListener(stats -> {
                    Long count = stats.getLong("activeRequestCount");
                    workloadView.setText("Active requests: " + (count == null ? 0 : count));
                })
                .addOnFailureListener(e -> workloadView.setText("Workload currently unavailable"));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String getServiceCategory(String issue, String fuelType) {
        if ("Flat Tyre".equals(issue)) return "Tyre Assistance";
        if ("Battery Problem".equals(issue) || "Vehicle not starting".equals(issue))
            return "Battery Assistance";
        if ("Fuel Problem".equals(issue)) return "Fuel Assistance";
        if ("EV Charging".equals(fuelType)) return "EV Assistance";
        return "Mechanical Assistance";
    }

    private void submitRequest() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvBatteryMessage.setText("Please log in before requesting assistance.");
            return;
        }
        if (spVehicleType.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText("Please select your vehicle type.");
            return;
        }
        if (spBatteryIssue.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText("Please select your vehicle issue.");
            return;
        }
        if (spFuelType.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText("Please select a fuel type.");
            return;
        }
        if (spFuelQuantity.getSelectedItemPosition() == 0) {
            tvBatteryMessage.setText("Please select the fuel quantity.");
            return;
        }

        String location = etBatteryLocation.getText().toString().trim();
        if (location.isEmpty()) {
            etBatteryLocation.setError("Location is required");
            etBatteryLocation.requestFocus();
            return;
        }

        String description = etBatteryDescription.getText().toString().trim();
        String issue = spBatteryIssue.getSelectedItem().toString();
        String fuelType = spFuelType.getSelectedItem().toString();
        String fuelQuantity = spFuelQuantity.getSelectedItem().toString();
        String quantityUnit = "CNG".equals(fuelType) ? "kg" :
                ("EV Charging".equals(fuelType) ? "kWh" : "litres");
        String category = getServiceCategory(issue, fuelType);
        updateEstimatedPrice();

        Map<String, Object> request = new HashMap<>();
        request.put("userId", user.getUid());
        request.put("userEmail", user.getEmail() == null ? "" : user.getEmail());
        request.put("serviceType", "Battery Help");
        request.put("serviceCategory", category);
        request.put("vehicleType", spVehicleType.getSelectedItem().toString());
        request.put("batteryIssue", issue);
        request.put("fuelType", fuelType);
        request.put("fuelQuantity", fuelQuantity);
        request.put("fuelQuantityUnit", quantityUnit);
        request.put("estimatedPrice", estimatedPrice);
        request.put("location", location);
        request.put("description", description);
        request.put("status", "pending");
        request.put("technicianAssigned", false);
        request.put("technicianName", "");
        request.put("technicianPhone", "");
        request.put("technicianRating", "");
        request.put("technicianType", "");
        request.put("createdAt", FieldValue.serverTimestamp());

        btnSubmitBatteryRequest.setEnabled(false);
        btnSubmitBatteryRequest.setText("Submitting...");
        tvBatteryMessage.setText("");

        db.collection("serviceRequests").add(request)
                .addOnSuccessListener(document -> {
                    String requestId = document.getId();
                    tvBatteryMessage.setText("Request saved. Finding an approved specialist...");
                    WorkerAssignmentHelper.assignRequest(requestId, category, user.getUid(),
                            new WorkerAssignmentHelper.AssignmentCallback() {
                                @Override public void onSuccess(String workerName) {
                                    Toast.makeText(BatteryHelpActivity.this,
                                            "Request submitted and assigned to " + workerName,
                                            Toast.LENGTH_LONG).show();
                                    tvBatteryMessage.setText("Request assigned to " + workerName);
                                    finish();
                                }
                                @Override public void onNoWorkerAvailable() {
                                    btnSubmitBatteryRequest.setEnabled(true);
                                    btnSubmitBatteryRequest.setText("Submit Request");
                                    tvBatteryMessage.setText("Request saved, but no approved " +
                                            category + " worker is currently available. Please try again later.");
                                    Toast.makeText(BatteryHelpActivity.this,
                                            "No matching approved worker found.", Toast.LENGTH_LONG).show();
                                }
                                @Override public void onFailure(String message) {
                                    btnSubmitBatteryRequest.setEnabled(true);
                                    btnSubmitBatteryRequest.setText("Submit Request");
                                    tvBatteryMessage.setText("Request saved, but assignment failed: " + message);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    btnSubmitBatteryRequest.setEnabled(true);
                    btnSubmitBatteryRequest.setText("Submit Request");
                    tvBatteryMessage.setText("Request could not be saved: " + e.getMessage());
                });
    }
}

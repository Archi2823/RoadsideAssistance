package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyVehiclesActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout vehicleContainer;
    private TextView tvEmptyVehicles;
    private Button btnAddVehicle;
    private String currentUid;

    private static class VehicleItem {
        String documentId;
        String model;
        String number;
        String type;
        long createdAt;

        VehicleItem(String documentId, String model, String number,
                    String type, long createdAt) {
            this.documentId = documentId;
            this.model = model;
            this.number = number;
            this.type = type;
            this.createdAt = createdAt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_vehicles);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        vehicleContainer = findViewById(R.id.vehicleContainer);
        tvEmptyVehicles = findViewById(R.id.tvEmptyVehicles);
        btnAddVehicle = findViewById(R.id.btnAddVehicle);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            openLoginScreen();
            return;
        }
        currentUid = user.getUid();

        btnAddVehicle.setOnClickListener(v -> {
            startActivity(new Intent(this, AddVehicleActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth != null && auth.getCurrentUser() != null) {
            currentUid = auth.getCurrentUser().getUid();
            loadVehicles();
        }
    }

    private void loadVehicles() {
        tvEmptyVehicles.setVisibility(View.GONE);
        vehicleContainer.removeAllViews();

        db.collection("vehicles")
                .whereEqualTo("userId", currentUid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    vehicleContainer.removeAllViews();

                    List<VehicleItem> vehicles = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String model = document.getString("model");
                        String number = document.getString("vehicleNumber");
                        String type = document.getString("vehicleType");
                        Long createdAt = document.getLong("createdAt");

                        vehicles.add(new VehicleItem(
                                document.getId(),
                                model == null ? "" : model,
                                number == null ? "" : number,
                                type == null ? "" : type,
                                createdAt == null ? 0L : createdAt
                        ));
                    }

                    Collections.sort(vehicles,
                            (a, b) -> Long.compare(b.createdAt, a.createdAt));

                    if (vehicles.isEmpty()) {
                        tvEmptyVehicles.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmptyVehicles.setVisibility(View.GONE);

                    for (VehicleItem vehicle : vehicles) {
                        addVehicleCard(vehicle);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Unable to load vehicles: " + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                    tvEmptyVehicles.setVisibility(View.VISIBLE);
                    tvEmptyVehicles.setText("Could not load vehicles. Please try again.");
                });
    }

    private void addVehicleCard(VehicleItem vehicle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.signup_input_bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        TextView tvModel = new TextView(this);
        tvModel.setText(vehicle.model);
        tvModel.setTextColor(0xFF172B4D);
        tvModel.setTextSize(18);
        tvModel.setTypeface(null, Typeface.BOLD);

        TextView tvNumber = new TextView(this);
        tvNumber.setText("Registration Number: " + vehicle.number);
        tvNumber.setTextColor(0xFF344563);
        tvNumber.setTextSize(14);
        tvNumber.setPadding(0, dp(7), 0, 0);

        TextView tvType = new TextView(this);
        tvType.setText("Vehicle Type: " + vehicle.type);
        tvType.setTextColor(0xFF344563);
        tvType.setTextSize(14);
        tvType.setPadding(0, dp(5), 0, dp(12));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        Button btnEdit = new Button(this);
        btnEdit.setText("Edit");
        btnEdit.setAllCaps(false);
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddVehicleActivity.class);
            intent.putExtra(AddVehicleActivity.EXTRA_DOCUMENT_ID, vehicle.documentId);
            intent.putExtra(AddVehicleActivity.EXTRA_MODEL, vehicle.model);
            intent.putExtra(AddVehicleActivity.EXTRA_NUMBER, vehicle.number);
            intent.putExtra(AddVehicleActivity.EXTRA_TYPE, vehicle.type);
            startActivity(intent);
        });

        Button btnDelete = new Button(this);
        btnDelete.setText("Delete");
        btnDelete.setAllCaps(false);
        btnDelete.setTextColor(0xFFC62828);
        btnDelete.setOnClickListener(v -> confirmDelete(vehicle));

        actions.addView(btnEdit);
        actions.addView(btnDelete);

        card.addView(tvModel);
        card.addView(tvNumber);
        card.addView(tvType);
        card.addView(actions);

        vehicleContainer.addView(card);
    }

    private void confirmDelete(VehicleItem vehicle) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Vehicle")
                .setMessage("Are you sure you want to delete "
                        + vehicle.model + " (" + vehicle.number + ")?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) ->
                        deleteVehicle(vehicle.documentId))
                .show();
    }

    private void deleteVehicle(String documentId) {
        db.collection("vehicles")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Vehicle deleted successfully.",
                            Toast.LENGTH_SHORT).show();
                    loadVehicles();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Delete failed: " + e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

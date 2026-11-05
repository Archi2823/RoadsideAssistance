package com.archi.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddVehicleActivity extends AppCompatActivity {

    public static final String EXTRA_DOCUMENT_ID = "vehicleDocumentId";
    public static final String EXTRA_MODEL = "vehicleModel";
    public static final String EXTRA_NUMBER = "vehicleNumber";
    public static final String EXTRA_TYPE = "vehicleType";

    private EditText etVehicleModel;
    private EditText etVehicleNumber;
    private Spinner spVehicleType;
    private Button btnSaveVehicle;
    private TextView tvVehicleFormTitle;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;
    private String oldDocumentId;
    private boolean saving = false;

    private final String[] vehicleTypes = {
            "Car", "Bike", "Scooter", "SUV", "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etVehicleModel = findViewById(R.id.etVehicleModel);
        etVehicleNumber = findViewById(R.id.etVehicleNumber);
        spVehicleType = findViewById(R.id.spVehicleType);
        btnSaveVehicle = findViewById(R.id.btnSaveVehicle);
        tvVehicleFormTitle = findViewById(R.id.tvVehicleFormTitle);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            openLoginScreen();
            return;
        }
        uid = user.getUid();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                vehicleTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVehicleType.setAdapter(adapter);

        oldDocumentId = getIntent().getStringExtra(EXTRA_DOCUMENT_ID);

        if (oldDocumentId != null) {
            tvVehicleFormTitle.setText("Edit Vehicle");
            btnSaveVehicle.setText("Update Vehicle");

            etVehicleModel.setText(getIntent().getStringExtra(EXTRA_MODEL));
            etVehicleNumber.setText(getIntent().getStringExtra(EXTRA_NUMBER));

            String selectedType = getIntent().getStringExtra(EXTRA_TYPE);
            if (selectedType != null) {
                for (int i = 0; i < vehicleTypes.length; i++) {
                    if (vehicleTypes[i].equalsIgnoreCase(selectedType)) {
                        spVehicleType.setSelection(i);
                        break;
                    }
                }
            }
        }

        btnSaveVehicle.setOnClickListener(v -> saveVehicle());
    }

    private void saveVehicle() {
        if (saving) return;

        String model = etVehicleModel.getText().toString().trim();
        String vehicleNumber = etVehicleNumber.getText()
                .toString().trim().toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "");
        String vehicleType = spVehicleType.getSelectedItem().toString();

        if (TextUtils.isEmpty(model) || model.length() < 2) {
            etVehicleModel.setError("Enter a valid vehicle model");
            etVehicleModel.requestFocus();
            return;
        }

        // Indian vehicle registration format validation.
        if (!vehicleNumber.matches("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{1,4}$")) {
            etVehicleNumber.setError("Enter a valid number, e.g. GJ01AB1234");
            etVehicleNumber.requestFocus();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            openLoginScreen();
            return;
        }
        uid = user.getUid();

        String normalizedNumber = vehicleNumber.replaceAll("[^A-Z0-9]", "");
        String newDocumentId = uid + "_" + normalizedNumber;

        DocumentReference newRef = db.collection("vehicles")
                .document(newDocumentId);

        DocumentReference oldRef = oldDocumentId == null
                ? null
                : db.collection("vehicles").document(oldDocumentId);

        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("userId", uid);
        vehicle.put("model", model);
        vehicle.put("vehicleNumber", vehicleNumber);
        vehicle.put("vehicleType", vehicleType);
        vehicle.put("createdAt", System.currentTimeMillis());

        setSaving(true);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            if (oldRef != null && oldDocumentId.equals(newDocumentId)) {
                // Updating the existing vehicle without changing its registration number.
                transaction.update(oldRef, "model", model);
                transaction.update(oldRef, "vehicleType", vehicleType);
                transaction.update(oldRef, "vehicleNumber", vehicleNumber);
            } else {
                // A deterministic document ID prevents duplicate registrations
                // for the same customer and normalized vehicle number.
                if (transaction.get(newRef).exists()) {
                    throw new FirebaseFirestoreException(
                            "This vehicle number is already registered in your account.",
                            FirebaseFirestoreException.Code.ALREADY_EXISTS
                    );
                }

                transaction.set(newRef, vehicle);

                if (oldRef != null) {
                    transaction.delete(oldRef);
                }
            }
            return null;
        }).addOnSuccessListener(unused -> {
            setSaving(false);
            Toast.makeText(this,
                    oldDocumentId == null
                            ? "Vehicle added successfully!"
                            : "Vehicle updated successfully!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            setSaving(false);
            Toast.makeText(this,
                    e.getLocalizedMessage() == null
                            ? "Unable to save vehicle."
                            : e.getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void setSaving(boolean isSaving) {
        saving = isSaving;
        btnSaveVehicle.setEnabled(!isSaving);
        btnSaveVehicle.setText(isSaving ? "Please wait..." :
                (oldDocumentId == null ? "Save Vehicle" : "Update Vehicle"));
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

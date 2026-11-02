
package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MechanicProfileActivity extends AppCompatActivity {

    private TextView tvMechanicName;
    private TextView tvMechanicEmail;
    private TextView tvMechanicPhone;
    private TextView tvMechanicSpecialization;
    private TextView tvMechanicAvailability;

    private Button btnCallMechanic;
    private Button btnFavouriteMechanic;

    private FirebaseFirestore db;
    private String customerUid;
    private String mechanicUid;
    private String mechanicPhone;

    private boolean isFavourite = false;
    private ListenerRegistration favouriteListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_profile);

        db = FirebaseFirestore.getInstance();

        tvMechanicName = findViewById(R.id.tvMechanicName);
        tvMechanicEmail = findViewById(R.id.tvMechanicEmail);
        tvMechanicPhone = findViewById(R.id.tvMechanicPhone);
        tvMechanicSpecialization =
                findViewById(R.id.tvMechanicSpecialization);
        tvMechanicAvailability =
                findViewById(R.id.tvMechanicAvailability);

        btnCallMechanic = findViewById(R.id.btnCallMechanic);
        btnFavouriteMechanic = findViewById(R.id.btnFavouriteMechanic);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        customerUid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        Intent intent = getIntent();

        mechanicUid = intent.getStringExtra("mechanicUid");

        String name = intent.getStringExtra("mechanicName");
        String email = intent.getStringExtra("mechanicEmail");
        mechanicPhone = intent.getStringExtra("mechanicPhone");
        String specialization =
                intent.getStringExtra("mechanicSpecialization");
        String availability =
                intent.getStringExtra("mechanicAvailability");

        tvMechanicName.setText(valueOrDefault(name, "Mechanic"));
        tvMechanicEmail.setText(valueOrDefault(email, "Not provided"));
        tvMechanicPhone.setText(
                valueOrDefault(mechanicPhone, "Not provided"));
        tvMechanicSpecialization.setText(
                valueOrDefault(specialization, "Not specified"));
        tvMechanicAvailability.setText(
                valueOrDefault(availability, "Not specified"));

        if ("available".equalsIgnoreCase(availability)) {
            tvMechanicAvailability.setTextColor(0xFF16865B);
        } else if ("busy".equalsIgnoreCase(availability)) {
            tvMechanicAvailability.setTextColor(0xFFD64545);
        } else {
            tvMechanicAvailability.setTextColor(0xFF64748B);
        }

        if (mechanicUid != null && !mechanicUid.trim().isEmpty()) {
            listenToFavouriteStatus();
        } else {
            btnFavouriteMechanic.setEnabled(false);
        }

        btnFavouriteMechanic.setOnClickListener(v -> toggleFavourite());

        btnCallMechanic.setOnClickListener(v -> callMechanic());
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private DocumentReference getFavouriteReference() {
        return db.collection("users")
                .document(customerUid)
                .collection("favoriteMechanics")
                .document(mechanicUid);
    }

    private void listenToFavouriteStatus() {

        favouriteListener = getFavouriteReference()
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        Toast.makeText(
                                this,
                                "Unable to check favourite status",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    isFavourite = snapshot != null && snapshot.exists();
                    updateFavouriteButton();
                });
    }

    private void updateFavouriteButton() {
        btnFavouriteMechanic.setText(
                isFavourite
                        ? "♥  Remove from Favourites"
                        : "♡  Save as Favourite"
        );

        btnFavouriteMechanic.setTextColor(
                isFavourite ? Color.WHITE : Color.rgb(220, 53, 69)
        );

        btnFavouriteMechanic.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        isFavourite
                                ? Color.rgb(220, 53, 69)
                                : Color.rgb(255, 235, 238)
                )
        );
    }

    private void toggleFavourite() {

        if (mechanicUid == null || customerUid == null) return;

        DocumentReference ref = getFavouriteReference();

        if (isFavourite) {

            ref.delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(
                                    this,
                                    "Removed from favourites",
                                    Toast.LENGTH_SHORT
                            ).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "Unable to remove favourite: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());

        } else {

            Map<String, Object> data = new HashMap<>();
            data.put("mechanicUid", mechanicUid);
            data.put("savedAt", FieldValue.serverTimestamp());

            ref.set(data)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(
                                    this,
                                    "Added to favourites",
                                    Toast.LENGTH_SHORT
                            ).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "Unable to save favourite: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
        }
    }

    private void callMechanic() {

        if (mechanicPhone == null
                || mechanicPhone.trim().isEmpty()
                || mechanicPhone.equalsIgnoreCase("Not provided")) {

            Toast.makeText(
                    this,
                    "Mechanic phone number is not available",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent dialIntent = new Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:" + Uri.encode(mechanicPhone))
        );

        startActivity(dialIntent);
    }

    @Override
    protected void onDestroy() {

        if (favouriteListener != null) {
            favouriteListener.remove();
        }

        super.onDestroy();
    }
}

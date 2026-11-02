
package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AvailableMechanicsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout mechanicsContainer;
    private ProgressBar progressBar;
    private TextView tvMessage;

    private ListenerRegistration mechanicsListener;
    private ListenerRegistration favouritesListener;

    private final Set<String> favouriteIds = new HashSet<>();
    private final List<DocumentSnapshot> mechanicsList = new ArrayList<>();

    private String customerUid;
    private boolean mechanicsLoaded = false;
    private boolean favouritesLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_mechanics);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        mechanicsContainer = findViewById(R.id.mechanicsContainer);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        customerUid = user.getUid();

        findViewById(R.id.btnMyFavourites).setOnClickListener(v ->
                startActivity(new Intent(
                        AvailableMechanicsActivity.this,
                        MyFavouriteMechanicsActivity.class)));

        // Open Mechanic History
        findViewById(R.id.btnMechanicHistory).setOnClickListener(v ->
                startActivity(new Intent(
                        AvailableMechanicsActivity.this,
                        MechanicHistoryActivity.class)));

        loadFavourites();
        loadAvailableMechanics();
    }

    private void loadFavourites() {
        favouritesListener = db.collection("users")
                .document(customerUid)
                .collection("favoriteMechanics")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        Toast.makeText(this,
                                "Unable to load favourites",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    favouriteIds.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot document :
                                snapshots.getDocuments()) {
                            favouriteIds.add(document.getId());
                        }
                    }

                    favouritesLoaded = true;
                    displayMechanics();
                });
    }

    private void loadAvailableMechanics() {
        progressBar.setVisibility(View.VISIBLE);
        tvMessage.setVisibility(View.GONE);

        mechanicsListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        progressBar.setVisibility(View.GONE);
                        tvMessage.setText(
                                "Unable to load mechanics. Please try again.");
                        tvMessage.setVisibility(View.VISIBLE);

                        Toast.makeText(this,
                                error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    mechanicsList.clear();

                    if (snapshots != null) {
                        mechanicsList.addAll(snapshots.getDocuments());
                    }

                    mechanicsLoaded = true;
                    progressBar.setVisibility(View.GONE);
                    displayMechanics();
                });
    }

    private void displayMechanics() {
        if (!mechanicsLoaded || !favouritesLoaded) return;

        mechanicsContainer.removeAllViews();

        if (mechanicsList.isEmpty()) {
            tvMessage.setText("No approved mechanics found currently.");
            tvMessage.setVisibility(View.VISIBLE);
            return;
        }

        tvMessage.setVisibility(View.GONE);

        for (DocumentSnapshot document : mechanicsList) {

            String uid = document.getId();

            String name = getValue(document.getString("name"), "Mechanic");
            String email = getValue(document.getString("email"), "Not provided");
            String phone = getValue(document.getString("phone"), "Not provided");
            String specialization = getValue(
                    document.getString("specialization"), "Not specified");
            String availability = getValue(
                    document.getString("availability"), "Not specified");

            addMechanicCard(uid, name, email, phone,
                    specialization, availability);
        }
    }

    private String getValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private void addMechanicCard(
            String uid,
            String name,
            String email,
            String phone,
            String specialization,
            String availability) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(14));
        cardBackground.setStroke(dp(1), Color.rgb(225, 232, 240));

        card.setBackground(cardBackground);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(19);
        tvName.setTextColor(Color.rgb(23, 43, 77));
        tvName.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);

        nameRow.addView(tvName, nameParams);

        TextView btnFavourite = new TextView(this);
        btnFavourite.setTextSize(27);
        btnFavourite.setGravity(Gravity.CENTER);
        btnFavourite.setPadding(dp(8), 0, dp(4), 0);

        updateFavouriteIcon(btnFavourite, favouriteIds.contains(uid));

        btnFavourite.setOnClickListener(v ->
                toggleFavourite(uid, btnFavourite));

        nameRow.addView(btnFavourite);
        card.addView(nameRow);

        card.addView(createInfoText(
                "Specialization: " + specialization));

        card.addView(createInfoText("Phone: " + phone));

        TextView tvAvailability = createInfoText(
                "Availability: " + availability);

        tvAvailability.setTypeface(null, Typeface.BOLD);
        tvAvailability.setPadding(0, dp(8), 0, dp(16));

        if (availability.equalsIgnoreCase("available")) {
            tvAvailability.setTextColor(Color.rgb(22, 134, 91));
        } else if (availability.equalsIgnoreCase("busy")) {
            tvAvailability.setTextColor(Color.rgb(214, 69, 69));
        } else {
            tvAvailability.setTextColor(Color.GRAY);
        }

        card.addView(tvAvailability);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnProfile = createActionButton(
                "View Profile", Color.rgb(23, 105, 170));

        LinearLayout.LayoutParams profileParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);
        profileParams.setMarginEnd(dp(8));

        buttonRow.addView(btnProfile, profileParams);

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AvailableMechanicsActivity.this,
                    MechanicProfileActivity.class);

            intent.putExtra("mechanicUid", uid);
            intent.putExtra("mechanicName", name);
            intent.putExtra("mechanicEmail", email);
            intent.putExtra("mechanicPhone", phone);
            intent.putExtra("mechanicSpecialization", specialization);
            intent.putExtra("mechanicAvailability", availability);

            startActivity(intent);
        });

        TextView btnCall = createActionButton(
                "Call Directly", Color.rgb(22, 134, 91));

        LinearLayout.LayoutParams callParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);
        callParams.setMarginStart(dp(8));

        buttonRow.addView(btnCall, callParams);

        if (phone.equals("Not provided")) {
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);
        } else {
            btnCall.setOnClickListener(v -> {
                recordContacted(uid);

                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + Uri.encode(phone)));

                startActivity(intent);
            });
        }

        card.addView(buttonRow);
        mechanicsContainer.addView(card);
    }

    private void recordContacted(String mechanicUid) {
        Map<String, Object> data = new HashMap<>();
        data.put("mechanicUid", mechanicUid);
        data.put("contactedAt", FieldValue.serverTimestamp());
        data.put("callCount", FieldValue.increment(1));

        db.collection("users")
                .document(customerUid)
                .collection("contactedMechanics")
                .document(mechanicUid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not update contact history: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private TextView createInfoText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(0, dp(8), 0, 0);
        return view;
    }

    private TextView createActionButton(String text, int color) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(5), 0, dp(5), 0);

        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(8));

        button.setBackground(background);
        return button;
    }

    private void updateFavouriteIcon(TextView button, boolean isFavourite) {
        button.setText(isFavourite ? "♥" : "♡");
        button.setTextColor(
                isFavourite ? Color.rgb(220, 53, 69) : Color.GRAY);
        button.setContentDescription(
                isFavourite ? "Remove from favourites" : "Save as favourite");
    }

    private void toggleFavourite(String mechanicUid, TextView button) {

        if (customerUid == null) return;

        com.google.firebase.firestore.DocumentReference favouriteRef =
                db.collection("users")
                        .document(customerUid)
                        .collection("favoriteMechanics")
                        .document(mechanicUid);

        if (favouriteIds.contains(mechanicUid)) {

            favouriteRef.delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    "Removed from favourites",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Unable to remove favourite: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG).show());

        } else {

            Map<String, Object> data = new HashMap<>();
            data.put("mechanicUid", mechanicUid);
            data.put("savedAt", FieldValue.serverTimestamp());

            favouriteRef.set(data)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    "Mechanic added to favourites",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Unable to save favourite: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (mechanicsListener != null) mechanicsListener.remove();
        if (favouritesListener != null) favouritesListener.remove();
        super.onDestroy();
    }
}

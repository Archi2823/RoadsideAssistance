
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyFavouriteMechanicsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String customerUid;

    private LinearLayout favouritesContainer;
    private ProgressBar progressBar;
    private TextView tvMessage;

    private ListenerRegistration favouritesListener;
    private ListenerRegistration mechanicsListener;

    private final Set<String> favouriteIds = new HashSet<>();
    private final List<DocumentSnapshot> approvedMechanics =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_favourite_mechanics);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        customerUid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        favouritesContainer = findViewById(R.id.favouritesContainer);
        progressBar = findViewById(R.id.progressBar);
        tvMessage = findViewById(R.id.tvMessage);

        loadFavourites();
        loadApprovedMechanics();
    }

    private void loadFavourites() {

        favouritesListener = db.collection("users")
                .document(customerUid)
                .collection("favoriteMechanics")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        showMessage("Unable to load your favourites.");
                        return;
                    }

                    favouriteIds.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot document :
                                snapshots.getDocuments()) {
                            favouriteIds.add(document.getId());
                        }
                    }

                    displayFavourites();
                });
    }

    private void loadApprovedMechanics() {

        progressBar.setVisibility(View.VISIBLE);

        mechanicsListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        showMessage("Unable to load mechanic details.");
                        return;
                    }

                    approvedMechanics.clear();

                    if (snapshots != null) {
                        approvedMechanics.addAll(
                                snapshots.getDocuments()
                        );
                    }

                    displayFavourites();
                });
    }

    private void displayFavourites() {

        if (progressBar.getVisibility() == View.VISIBLE) return;

        favouritesContainer.removeAllViews();

        if (favouriteIds.isEmpty()) {
            showMessage(
                    "You have not saved any favourite mechanics yet."
            );
            return;
        }

        int displayedCount = 0;

        for (DocumentSnapshot document : approvedMechanics) {

            String uid = document.getId();

            if (!favouriteIds.contains(uid)) {
                continue;
            }

            displayedCount++;

            String name = getValue(
                    document.getString("name"), "Mechanic");

            String email = getValue(
                    document.getString("email"), "Not provided");

            String phone = getValue(
                    document.getString("phone"), "Not provided");

            String specialization = getValue(
                    document.getString("specialization"), "Not specified");

            String availability = getValue(
                    document.getString("availability"), "Not specified");

            addFavouriteCard(
                    uid, name, email, phone,
                    specialization, availability
            );
        }

        if (displayedCount == 0) {
            showMessage(
                    "Your saved mechanics are no longer approved or available."
            );
        } else {
            tvMessage.setVisibility(View.GONE);
        }
    }

    private String getValue(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private void addFavouriteCard(
            String uid,
            String name,
            String email,
            String phone,
            String specialization,
            String availability) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(225, 232, 240));

        card.setBackground(background);
        card.setElevation(dp(3));

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        TextView tvName = new TextView(this);
        tvName.setText("♥  " + name);
        tvName.setTextSize(19);
        tvName.setTextColor(Color.rgb(220, 53, 69));
        tvName.setTypeface(null, Typeface.BOLD);
        card.addView(tvName);

        addInfo(card, "Specialization: " + specialization);
        addInfo(card, "Phone: " + phone);

        TextView tvAvailability = addInfo(
                card, "Availability: " + availability);

        tvAvailability.setTypeface(null, Typeface.BOLD);

        if (availability.equalsIgnoreCase("available")) {
            tvAvailability.setTextColor(Color.rgb(22, 134, 91));
        } else if (availability.equalsIgnoreCase("busy")) {
            tvAvailability.setTextColor(Color.rgb(214, 69, 69));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnProfile = createButton(
                "View Profile", Color.rgb(23, 105, 170));

        LinearLayout.LayoutParams p1 =
                new LinearLayout.LayoutParams(0, dp(46), 1);
        p1.setMarginEnd(dp(5));
        row.addView(btnProfile, p1);

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    this, MechanicProfileActivity.class);

            intent.putExtra("mechanicUid", uid);
            intent.putExtra("mechanicName", name);
            intent.putExtra("mechanicEmail", email);
            intent.putExtra("mechanicPhone", phone);
            intent.putExtra("mechanicSpecialization", specialization);
            intent.putExtra("mechanicAvailability", availability);

            startActivity(intent);
        });

        TextView btnCall = createButton(
                "Call Directly", Color.rgb(22, 134, 91));

        LinearLayout.LayoutParams p2 =
                new LinearLayout.LayoutParams(0, dp(46), 1);
        p2.setMarginStart(dp(5));
        row.addView(btnCall, p2);

        if (phone.equals("Not provided")) {
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);
        } else {
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + Uri.encode(phone))
                );
                startActivity(intent);
            });
        }

        card.addView(row);

        TextView btnRemove = createButton(
                "Remove from Favourites",
                Color.rgb(220, 53, 69)
        );

        LinearLayout.LayoutParams removeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(44)
                );

        removeParams.setMargins(0, dp(10), 0, 0);
        card.addView(btnRemove, removeParams);

        btnRemove.setOnClickListener(v -> {

            db.collection("users")
                    .document(customerUid)
                    .collection("favoriteMechanics")
                    .document(uid)
                    .delete()
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
        });

        favouritesContainer.addView(card);
    }

    private TextView addInfo(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(0, dp(9), 0, 0);
        parent.addView(view);
        return view;
    }

    private TextView createButton(String text, int color) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));

        button.setBackground(bg);
        return button;
    }

    private void showMessage(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {

        if (favouritesListener != null) favouritesListener.remove();
        if (mechanicsListener != null) mechanicsListener.remove();

        super.onDestroy();
    }
}

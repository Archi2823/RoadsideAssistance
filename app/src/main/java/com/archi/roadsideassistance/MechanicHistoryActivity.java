
package com.archi.roadsideassistance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class MechanicHistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String customerUid;

    private LinearLayout mainLayout;
    private LinearLayout contentContainer;
    private ProgressBar progressBar;
    private TextView tvMessage;
    private Button btnFavourites;
    private Button btnContacted;

    private ListenerRegistration favouritesListener;
    private ListenerRegistration contactedListener;
    private ListenerRegistration mechanicsListener;

    private final Set<String> favouriteIds = new HashSet<>();
    private final Set<String> contactedIds = new HashSet<>();

    private final List<DocumentSnapshot> mechanicsList =
            new ArrayList<>();

    private boolean favouritesLoaded = false;
    private boolean contactedLoaded = false;
    private boolean mechanicsLoaded = false;

    private String selectedTab = "favourites";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        customerUid = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();

        createScreen();

        loadFavourites();
        loadContactedMechanics();
        loadApprovedMechanics();
    }

    // Create screen without requiring an XML layout
    private void createScreen() {

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(18), dp(20), dp(18), dp(12));
        mainLayout.setBackgroundColor(Color.rgb(245, 247, 250));

        setContentView(mainLayout);

        TextView title = new TextView(this);
        title.setText("Mechanic History");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(23, 43, 77));
        title.setTypeface(null, Typeface.BOLD);

        mainLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("View your favourite and contacted mechanics");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setPadding(0, dp(6), 0, dp(20));

        mainLayout.addView(subtitle);

        LinearLayout tabLayout = new LinearLayout(this);
        tabLayout.setOrientation(LinearLayout.HORIZONTAL);

        btnFavourites = new Button(this);
        btnFavourites.setText("♥ Favourites");

        btnContacted = new Button(this);
        btnContacted.setText("Contacted");

        tabLayout.addView(btnFavourites,
                new LinearLayout.LayoutParams(0, dp(50), 1));

        LinearLayout.LayoutParams contactedParams =
                new LinearLayout.LayoutParams(0, dp(50), 1);

        contactedParams.setMarginStart(dp(8));

        tabLayout.addView(btnContacted, contactedParams);

        mainLayout.addView(tabLayout);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.VISIBLE);

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        progressParams.gravity = Gravity.CENTER;
        progressParams.setMargins(0, dp(20), 0, dp(10));

        mainLayout.addView(progressBar, progressParams);

        tvMessage = new TextView(this);
        tvMessage.setTextSize(15);
        tvMessage.setTextColor(Color.GRAY);
        tvMessage.setGravity(Gravity.CENTER);
        tvMessage.setPadding(dp(10), dp(20), dp(10), dp(20));
        tvMessage.setVisibility(View.GONE);

        mainLayout.addView(tvMessage);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                );

        scrollParams.setMargins(0, dp(12), 0, 0);

        mainLayout.addView(scrollView, scrollParams);

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(contentContainer);

        btnFavourites.setOnClickListener(v -> {
            selectedTab = "favourites";
            updateTabs();
            displayHistory();
        });

        btnContacted.setOnClickListener(v -> {
            selectedTab = "contacted";
            updateTabs();
            displayHistory();
        });

        updateTabs();
    }

    // Load saved favourites
    private void loadFavourites() {

        favouritesListener = db.collection("users")
                .document(customerUid)
                .collection("favoriteMechanics")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        showError("Unable to load favourites.");
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
                    displayHistory();
                });
    }

    // Load contacted mechanics
    private void loadContactedMechanics() {

        contactedListener = db.collection("users")
                .document(customerUid)
                .collection("contactedMechanics")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        showError("Unable to load contacted mechanics.");
                        return;
                    }

                    contactedIds.clear();

                    if (snapshots != null) {
                        for (DocumentSnapshot document :
                                snapshots.getDocuments()) {

                            contactedIds.add(document.getId());
                        }
                    }

                    contactedLoaded = true;
                    displayHistory();
                });
    }

    // Load approved workers from existing users collection
    private void loadApprovedMechanics() {

        mechanicsListener = db.collection("users")
                .whereEqualTo("role", "worker")
                .whereEqualTo("approvalStatus", "approved")
                .addSnapshotListener((snapshots, error) -> {

                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        showError("Unable to load mechanic details.");
                        return;
                    }

                    mechanicsList.clear();

                    if (snapshots != null) {
                        mechanicsList.addAll(
                                snapshots.getDocuments()
                        );
                    }

                    mechanicsLoaded = true;
                    displayHistory();
                });
    }

    // Display selected history category
    private void displayHistory() {

        if (!favouritesLoaded ||
                !contactedLoaded ||
                !mechanicsLoaded) {
            return;
        }

        progressBar.setVisibility(View.GONE);
        contentContainer.removeAllViews();
        tvMessage.setVisibility(View.GONE);

        Set<String> selectedIds = selectedTab.equals("favourites")
                ? favouriteIds : contactedIds;

        if (selectedIds.isEmpty()) {
            tvMessage.setText(
                    selectedTab.equals("favourites")
                            ? "You have not saved any favourite mechanics yet."
                            : "You have not contacted any mechanics yet."
            );

            tvMessage.setVisibility(View.VISIBLE);
            return;
        }

        int count = 0;

        for (DocumentSnapshot document : mechanicsList) {

            String uid = document.getId();

            if (!selectedIds.contains(uid)) {
                continue;
            }

            count++;

            String name = getValue(
                    document.getString("name"), "Mechanic");

            String email = getValue(
                    document.getString("email"), "Not provided");

            String phone = getValue(
                    document.getString("phone"), "Not provided");

            String specialization = getValue(
                    document.getString("specialization"),
                    "Not specified");

            String availability = getValue(
                    document.getString("availability"),
                    "Not specified");

            addMechanicCard(
                    uid,
                    name,
                    email,
                    phone,
                    specialization,
                    availability
            );
        }

        if (count == 0) {
            tvMessage.setText(
                    "No approved mechanics found in this history."
            );
            tvMessage.setVisibility(View.VISIBLE);
        }
    }

    // Create mechanic card
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

        tvName.setText(
                (selectedTab.equals("favourites") ? "♥  " : "●  ")
                        + name
        );

        tvName.setTextSize(19);
        tvName.setTextColor(
                selectedTab.equals("favourites")
                        ? Color.rgb(220, 53, 69)
                        : Color.rgb(23, 105, 170)
        );

        tvName.setTypeface(null, Typeface.BOLD);
        card.addView(tvName);

        addInfo(card, "Specialization: " + specialization);
        addInfo(card, "Phone: " + phone);

        TextView tvAvailability =
                addInfo(card, "Availability: " + availability);

        tvAvailability.setTypeface(null, Typeface.BOLD);

        if (availability.equalsIgnoreCase("available")) {
            tvAvailability.setTextColor(Color.rgb(22, 134, 91));
        } else if (availability.equalsIgnoreCase("busy")) {
            tvAvailability.setTextColor(Color.rgb(214, 69, 69));
        }

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnProfile = createButton(
                "View Profile",
                Color.rgb(23, 105, 170)
        );

        LinearLayout.LayoutParams profileParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);

        profileParams.setMarginEnd(dp(5));

        buttonRow.addView(btnProfile, profileParams);

        btnProfile.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MechanicHistoryActivity.this,
                    MechanicProfileActivity.class
            );

            intent.putExtra("mechanicUid", uid);
            intent.putExtra("mechanicName", name);
            intent.putExtra("mechanicEmail", email);
            intent.putExtra("mechanicPhone", phone);
            intent.putExtra("mechanicSpecialization", specialization);
            intent.putExtra("mechanicAvailability", availability);

            startActivity(intent);
        });

        TextView btnCall = createButton(
                "Call Directly",
                Color.rgb(22, 134, 91)
        );

        LinearLayout.LayoutParams callParams =
                new LinearLayout.LayoutParams(0, dp(46), 1);

        callParams.setMarginStart(dp(5));

        buttonRow.addView(btnCall, callParams);

        if (phone.equals("Not provided")) {

            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);

        } else {

            btnCall.setOnClickListener(v -> {

                recordContacted(uid);

                Intent intent = new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:" + Uri.encode(phone))
                );

                startActivity(intent);
            });
        }

        card.addView(buttonRow);

        TextView btnRemove = createButton(
                selectedTab.equals("favourites")
                        ? "Remove from Favourites"
                        : "Remove from Contacted History",
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

            String collectionName =
                    selectedTab.equals("favourites")
                            ? "favoriteMechanics"
                            : "contactedMechanics";

            db.collection("users")
                    .document(customerUid)
                    .collection(collectionName)
                    .document(uid)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(
                                    this,
                                    "History entry removed",
                                    Toast.LENGTH_SHORT
                            ).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "Unable to remove: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );
        });

        contentContainer.addView(card);
    }

    // Record a dialer opening in contacted history
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
                        Toast.makeText(
                                this,
                                "Could not update contact history: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
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
        button.setPadding(dp(5), 0, dp(5), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(8));

        button.setBackground(bg);

        return button;
    }

    private void updateTabs() {

        if (btnFavourites == null || btnContacted == null) return;

        btnFavourites.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        selectedTab.equals("favourites")
                                ? Color.rgb(220, 53, 69)
                                : Color.LTGRAY
                )
        );

        btnFavourites.setTextColor(
                selectedTab.equals("favourites")
                        ? Color.WHITE : Color.DKGRAY
        );

        btnContacted.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        selectedTab.equals("contacted")
                                ? Color.rgb(23, 105, 170)
                                : Color.LTGRAY
                )
        );

        btnContacted.setTextColor(
                selectedTab.equals("contacted")
                        ? Color.WHITE : Color.DKGRAY
        );
    }

    private String getValue(String value, String defaultValue) {

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }

    private void showError(String message) {

        progressBar.setVisibility(View.GONE);
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

    @Override
    protected void onDestroy() {

        if (favouritesListener != null) {
            favouritesListener.remove();
        }

        if (contactedListener != null) {
            contactedListener.remove();
        }

        if (mechanicsListener != null) {
            mechanicsListener.remove();
        }

        super.onDestroy();
    }
}

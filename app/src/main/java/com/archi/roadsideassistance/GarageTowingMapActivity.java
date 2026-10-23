
package com.archi.roadsideassistance;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GarageTowingMapActivity extends Activity {

    private LinearLayout servicePointsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage_towing_map);

        servicePointsContainer = findViewById(R.id.servicePointsContainer);

        addService(
                "Garage 1",
                "Car Repair Garage",
                "Ahmedabad, Gujarat",
                "GARAGE"
        );

        addService(
                "Garage 2",
                "Vehicle Repair Service",
                "Ahmedabad, Gujarat",
                "GARAGE"
        );

        addService(
                "Towing Point 1",
                "Vehicle Towing Service",
                "Ahmedabad, Gujarat",
                "TOWING"
        );

        addService(
                "Towing Point 2",
                "Roadside Towing Assistance",
                "Ahmedabad, Gujarat",
                "TOWING"
        );
    }

    private void addService(
            String name,
            String serviceType,
            String address,
            String category
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(225, 230, 238));
        card.setBackground(background);

        TextView categoryView = new TextView(this);
        categoryView.setText(
                category.equals("GARAGE") ? "GARAGE" : "TOWING SERVICE"
        );
        categoryView.setTextSize(11);
        categoryView.setTypeface(null, Typeface.BOLD);
        categoryView.setTextColor(Color.rgb(40, 95, 180));

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(18);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(Color.rgb(30, 41, 59));
        nameView.setPadding(0, dp(8), 0, dp(4));

        TextView typeView = new TextView(this);
        typeView.setText(serviceType);
        typeView.setTextSize(14);
        typeView.setTextColor(Color.rgb(75, 85, 99));

        TextView addressView = new TextView(this);
        addressView.setText(address);
        addressView.setTextSize(13);
        addressView.setTextColor(Color.rgb(107, 114, 128));
        addressView.setPadding(0, dp(6), 0, 0);

        card.addView(categoryView);
        card.addView(nameView);
        card.addView(typeView);
        card.addView(addressView);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, dp(12));
        servicePointsContainer.addView(card, params);
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}

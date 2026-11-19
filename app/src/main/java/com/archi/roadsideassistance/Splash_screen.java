package com.archi.roadsideassistance;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash_screen extends AppCompatActivity {

    private static final String TAG = "Splash_screen";

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e("SPLASH_CHECK", "Splash_screen onCreate started");
        Log.d(TAG, "Splash onCreate started");

        setContentView(R.layout.activity_splash_screen);

        TextView logo = findViewById(R.id.tvLogo);
        TextView appName = findViewById(R.id.tvAppName);
        TextView tagline = findViewById(R.id.tvTagline);

        if (logo == null || appName == null || tagline == null) {
            Log.e(TAG, "Splash views missing. Check XML IDs.");
            return;
        }

        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);

        appName.setAlpha(0f);
        tagline.setAlpha(0f);

        ObjectAnimator logoFade =
                ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        ObjectAnimator logoX =
                ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1f);

        ObjectAnimator logoY =
                ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1f);

        AnimatorSet logoAnimation = new AnimatorSet();
        logoAnimation.playTogether(logoFade, logoX, logoY);
        logoAnimation.setDuration(800);
        logoAnimation.setInterpolator(new DecelerateInterpolator());
        logoAnimation.start();

        appName.setTranslationY(35f);

        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(350)
                .setDuration(700)
                .start();

        tagline.animate()
                .alpha(1f)
                .setStartDelay(650)
                .setDuration(700)
                .start();

        Log.d(TAG, "Splash UI initialized successfully");

        handler.postDelayed(() -> {
            Log.d(TAG, "Opening MainActivity");

            if (isFinishing() || isDestroyed()) {
                return;
            }

            Intent intent = new Intent(
                    Splash_screen.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();

        }, 2500);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
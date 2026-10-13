
package com.archi.roadsideassistance;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Splash_screen extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        TextView logo = findViewById(R.id.tvLogo);
        TextView appName = findViewById(R.id.tvAppName);
        TextView tagline = findViewById(R.id.tvTagline);

        // Logo fade-in and zoom animation
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);

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

        // App name slides upward
        appName.setTranslationY(35f);
        appName.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(350)
                .setDuration(700)
                .start();

        // Tagline fade-in
        tagline.animate()
                .alpha(1f)
                .setStartDelay(650)
                .setDuration(700)
                .start();

        // Navigate after the splash animation
        handler.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            Intent intent;

            if (auth.getCurrentUser() != null) {
                intent = new Intent(
                        Splash_screen.this, MainActivity.class);
            } else {
                intent = new Intent(
                        Splash_screen.this, activity_signup.class);
            }

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

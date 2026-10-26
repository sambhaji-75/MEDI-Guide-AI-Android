
package com.mediguide.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private View homeContent;
    private View fragmentContainer;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        homeContent = findViewById(R.id.homeContent);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        requestNotificationPermission();
        loadGreeting();
        setupHomeButtons();
        setupBottomNavigation();

        showHomeScreen();
    }

    private void loadGreeting() {
        TextView greeting = findViewById(R.id.txtGreeting);

        if (greeting == null || auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String name = doc.getString("name");

                    if (name != null && !name.trim().isEmpty()) {
                        greeting.setText("Welcome, " + name);
                    }
                });
    }

    private void setupHomeButtons() {

        View motherCare = findViewById(R.id.btnMotherCare);
        if (motherCare != null) {
            motherCare.setOnClickListener(v ->
                    startActivity(new Intent(
                            MainActivity.this,
                            MotherCareActivity.class
                    )));
        }

        View babyCare = findViewById(R.id.btnBabyCare);
        if (babyCare != null) {
            babyCare.setOnClickListener(v ->
                    startActivity(new Intent(
                            MainActivity.this,
                            BabyCareActivity.class
                    )));
        }

        View medicine = findViewById(R.id.btnAddMedicine);
        if (medicine != null) {
            medicine.setOnClickListener(v ->
                    startActivity(new Intent(
                            MainActivity.this,
                            AddMedicineActivity.class
                    )));
        }

        View aiAssistant = findViewById(R.id.btnOpenAi);
        if (aiAssistant != null) {
            aiAssistant.setOnClickListener(v -> openAiAssistant());
        }

        View elderCare = findViewById(R.id.btnElderCare);
        if (elderCare != null) {
            elderCare.setOnClickListener(v ->
                    startActivity(new Intent(
                            MainActivity.this,
                            ElderCareActivity.class
                    )));
        }

        // My Profile button
        View myProfile = findViewById(R.id.btnMyProfile);
        if (myProfile != null) {
            myProfile.setOnClickListener(v ->
                    startActivity(new Intent(
                            MainActivity.this,
                            MyProfileActivity.class
                    )));
        }
    }

    private void openAiAssistant() {
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(
                    MainActivity.this,
                    AIChatActivity.class
            );
            intent.putExtra("preferred_language", "English");
            intent.putExtra("personalization_enabled", true);
            startActivity(intent);
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    String language = doc.getString("preferredLanguage");

                    if (language == null || language.trim().isEmpty()) {
                        language = doc.getString("language");
                    }

                    if (language == null || language.trim().isEmpty()) {
                        language = "English";
                    }

                    Boolean savedPersonalization =
                            doc.getBoolean("personalizationEnabled");

                    boolean enabled = savedPersonalization == null
                            || savedPersonalization;

                    Intent intent = new Intent(
                            MainActivity.this,
                            AIChatActivity.class
                    );

                    intent.putExtra("preferred_language", language);
                    intent.putExtra("personalization_enabled", enabled);

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Intent intent = new Intent(
                            MainActivity.this,
                            AIChatActivity.class
                    );

                    intent.putExtra("preferred_language", "English");
                    intent.putExtra("personalization_enabled", true);

                    startActivity(intent);
                });
    }

    private void showBandFragment() {
        if (homeContent == null || fragmentContainer == null) {
            return;
        }

        homeContent.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        if (current instanceof BandFragment) {
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new BandFragment())
                .commit();
    }

    private void showHomeScreen() {
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
        }

        if (homeContent != null) {
            homeContent.setVisibility(View.VISIBLE);
        }

        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        if (current != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(current)
                    .commit();
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            return;
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_band) {
                showBandFragment();
                return true;
            }

            if (id == R.id.nav_home) {
                showHomeScreen();
                return true;
            }

            if (id == R.id.nav_ai) {
                openAiAssistant();
                return true;
            }

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(
                        MainActivity.this,
                        DashboardActivity.class
                ));
                return true;
            }

            return false;
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    100
            );
        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer != null
                && fragmentContainer.getVisibility() == View.VISIBLE) {
            showHomeScreen();

            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }

            return;
        }

        super.onBackPressed();
    }
}

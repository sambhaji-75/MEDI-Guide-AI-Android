package com.mediguide.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private EditText etAge;
    private Spinner spLanguage;

    private CheckBox cbGeneral, cbMedicine, cbMother;
    private CheckBox cbBaby, cbElder, cbFitness;

    private Button btnContinue;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private SharedPreferences preferences;

    private static final String PREF_NAME = "MEDI_GUIDE_PREFS";
    private static final String KEY_PROFILE_COMPLETED = "profile_completed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // If profile was already completed on this device, go Home.
        if (preferences.getBoolean(KEY_PROFILE_COMPLETED, false)) {
            goToHome();
            return;
        }

        etAge = findViewById(R.id.etAge);
        spLanguage = findViewById(R.id.spLanguage);

        cbGeneral = findViewById(R.id.cbGeneral);
        cbMedicine = findViewById(R.id.cbMedicine);
        cbMother = findViewById(R.id.cbMother);
        cbBaby = findViewById(R.id.cbBaby);
        cbElder = findViewById(R.id.cbElder);
        cbFitness = findViewById(R.id.cbFitness);

        btnContinue = findViewById(R.id.btnContinue);

        String[] languages = {"English", "Hindi", "Marathi"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spLanguage.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {

        String ageText = etAge.getText().toString().trim();

        if (ageText.isEmpty()) {
            etAge.setError("Please enter your age");
            etAge.requestFocus();
            return;
        }

        int age;

        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            etAge.setError("Enter a valid age");
            etAge.requestFocus();
            return;
        }

        if (age < 1 || age > 120) {
            etAge.setError("Enter age between 1 and 120");
            etAge.requestFocus();
            return;
        }

        ArrayList<String> needs = new ArrayList<>();

        if (cbGeneral.isChecked()) {
            needs.add("General Health");
        }

        if (cbMedicine.isChecked()) {
            needs.add("Medicine Reminders");
        }

        if (cbMother.isChecked()) {
            needs.add("Mother Care");
        }

        if (cbBaby.isChecked()) {
            needs.add("Baby Care");
        }

        if (cbElder.isChecked()) {
            needs.add("Elder Care");
        }

        if (cbFitness.isChecked()) {
            needs.add("Fitness and Wellness");
        }

        if (needs.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please select at least one health need",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String language = spLanguage.getSelectedItem().toString();

        btnContinue.setEnabled(false);
        btnContinue.setText("Saving profile...");

        if (auth.getCurrentUser() != null) {
            saveProfile(age, language, needs);
        } else {
            auth.signInAnonymously()
                    .addOnSuccessListener(result ->
                            saveProfile(age, language, needs)
                    )
                    .addOnFailureListener(e ->
                            showError("Authentication failed: " + e.getMessage())
                    );
        }
    }

    private void saveProfile(
            int age,
            String language,
            ArrayList<String> needs
    ) {
        if (auth.getCurrentUser() == null) {
            showError("User session not found. Please try again.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> profile = new HashMap<>();
        profile.put("age", age);
        profile.put("language", language);
        profile.put("healthNeeds", needs);
        profile.put("profileCompleted", true);

        db.collection("users")
                .document(uid)
                .set(profile, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    // Save completion only after Firebase succeeds.
                    preferences.edit()
                            .putBoolean(KEY_PROFILE_COMPLETED, true)
                            .apply();

                    goToHome();
                })
                .addOnFailureListener(e ->
                        showError("Profile save failed: " + e.getMessage())
                );
    }

    private void goToHome() {

        Intent intent = new Intent(
                UserProfileActivity.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showError(String message) {

        if (btnContinue != null) {
            btnContinue.setEnabled(true);
            btnContinue.setText("Continue to Home");
        }

        Toast.makeText(
                UserProfileActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}

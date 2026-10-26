
package com.mediguide.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MyProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView txtName, txtEmail, txtAge, txtLanguage, txtHealthNeeds;
    private Button btnLogout, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtName = findViewById(R.id.txtProfileName);
        txtEmail = findViewById(R.id.txtProfileEmail);
        txtAge = findViewById(R.id.txtProfileAge);
        txtLanguage = findViewById(R.id.txtProfileLanguage);
        txtHealthNeeds = findViewById(R.id.txtProfileHealthNeeds);
        btnLogout = findViewById(R.id.btnProfileLogout);
        btnBack = findViewById(R.id.btnProfileBack);

        btnBack.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> logout());

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth != null && auth.getCurrentUser() != null) {
            loadProfile();
        }
    }

    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        txtName.setText("Loading...");
        txtEmail.setText(user.getEmail() == null ? "Not available" : user.getEmail());

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;

                    String name = doc.getString("name");
                    if (name == null || name.trim().isEmpty()) {
                        name = "User";
                    }
                    txtName.setText(name);

                    Long age = doc.getLong("age");
                    txtAge.setText(age == null ? "Not added" : age + " years");

                    String language = doc.getString("preferredLanguage");
                    if (language == null || language.trim().isEmpty()) {
                        language = doc.getString("language");
                    }
                    txtLanguage.setText(
                            language == null || language.trim().isEmpty()
                                    ? "Not selected" : language
                    );

                    List<String> needs = (List<String>) doc.get("healthNeeds");
                    if (needs == null || needs.isEmpty()) {
                        txtHealthNeeds.setText("Not added");
                    } else {
                        txtHealthNeeds.setText(android.text.TextUtils.join(", ", needs));
                    }

                    String email = doc.getString("email");
                    if (email == null || email.trim().isEmpty()) {
                        email = user.getEmail();
                    }
                    txtEmail.setText(
                            email == null || email.trim().isEmpty()
                                    ? "Not available" : email
                    );

                    String mobile = doc.getString("mobile");
                    TextView txtMobile = findViewById(R.id.txtProfileMobile);
                    txtMobile.setText(
                            mobile == null || mobile.trim().isEmpty()
                                    ? "Not added" : mobile
                    );
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(this,
                                "Could not load profile. Check your internet connection.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logout() {
        auth.signOut();

        getSharedPreferences("MEDI_GUIDE_PREFS", MODE_PRIVATE)
                .edit()
                .remove("profile_completed")
                .apply();

        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

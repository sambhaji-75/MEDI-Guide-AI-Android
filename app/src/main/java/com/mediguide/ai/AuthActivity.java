package com.mediguide.ai;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;

    private Button loginButton;
    private Button registerButton;
    private TextView statusText;

    private final int blue = Color.rgb(21, 101, 192);
    private final int dark = Color.rgb(30, 41, 59);
    private final int muted = Color.rgb(100, 116, 139);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            routeSignedInUser();
        } else {
            buildScreen();
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 250, 255));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(26), dp(36), dp(26), dp(28));
        page.setGravity(Gravity.CENTER_HORIZONTAL);

        scroll.addView(page);

        TextView logo = new TextView(this);
        logo.setText("✚");
        logo.setTextSize(46);
        logo.setTextColor(blue);
        logo.setGravity(Gravity.CENTER);
        page.addView(logo, matchWrap());

        TextView title = new TextView(this);
        title.setText("MEDI CALLER-AI");
        title.setTextSize(29);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(dark);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(6);
        page.addView(title, titleParams);

        TextView tagline = new TextView(this);
        tagline.setText(
                "One App. Mother to Child.\nInformation to Adherence."
        );
        tagline.setTextSize(15);
        tagline.setLineSpacing(dp(3), 1f);
        tagline.setTextColor(muted);
        tagline.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams tagParams = matchWrap();
        tagParams.topMargin = dp(8);
        tagParams.bottomMargin = dp(28);
        page.addView(tagline, tagParams);

        TextView heading = new TextView(this);
        heading.setText("Welcome to your care journey");
        heading.setTextSize(21);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(dark);
        page.addView(heading, matchWrap());

        TextView subheading = new TextView(this);
        subheading.setText(
                "Sign in or create an account to continue."
        );
        subheading.setTextSize(14);
        subheading.setTextColor(muted);

        LinearLayout.LayoutParams subParams = matchWrap();
        subParams.topMargin = dp(6);
        subParams.bottomMargin = dp(18);
        page.addView(subheading, subParams);

        nameInput = field(
                "Full name (for new account)",
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        page.addView(nameInput, fieldParams());

        emailInput = field(
                "Email address",
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        page.addView(emailInput, fieldParams());

        passwordInput = field(
                "Password (minimum 6 characters)",
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        page.addView(passwordInput, fieldParams());

        loginButton = button("Sign In", blue, Color.WHITE);
        page.addView(loginButton, buttonParams());

        registerButton = button(
                "Create Account",
                Color.WHITE,
                blue
        );
        registerButton.setBackground(makeBorderBackground());
        registerButton.setTextColor(blue);
        page.addView(registerButton, buttonParams());

        TextView forgot = new TextView(this);
        forgot.setText("Forgot password?");
        forgot.setTextColor(blue);
        forgot.setTextSize(14);
        forgot.setGravity(Gravity.CENTER);
        forgot.setPadding(0, dp(14), 0, dp(10));
        forgot.setOnClickListener(v -> resetPassword());
        page.addView(forgot, matchWrap());

        statusText = new TextView(this);
        statusText.setTextColor(muted);
        statusText.setTextSize(13);
        statusText.setGravity(Gravity.CENTER);
        page.addView(statusText, matchWrap());

        TextView note = new TextView(this);
        note.setText(
                "Health information is educational and does not replace "
                        + "advice from a qualified healthcare professional."
        );
        note.setTextColor(muted);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(24);
        page.addView(note, noteParams);

        setContentView(scroll);

        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> register());
    }

    private EditText field(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setTextSize(15);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setPadding(dp(14), dp(10), dp(14), dp(10));
        edit.setBackground(makeBorderBackground());
        return edit;
    }

    private GradientDrawable makeBorderBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), Color.rgb(214, 225, 239));
        return drawable;
    }

    private Button button(String text, int bg, int fg) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setTextColor(fg);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(bg)
        );
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(54));
        params.bottomMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(8);
        return params;
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    private String email() {
        return emailInput.getText().toString().trim();
    }

    private String password() {
        return passwordInput.getText().toString();
    }

    private boolean validateCredentials() {
        String address = email();

        if (address.isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(address).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return false;
        }

        if (password().length() < 6) {
            passwordInput.setError(
                    "Password must contain at least 6 characters"
            );
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void setBusy(boolean busy, String message) {
        if (loginButton != null) {
            loginButton.setEnabled(!busy);
        }

        if (registerButton != null) {
            registerButton.setEnabled(!busy);
        }

        if (statusText != null) {
            statusText.setText(message == null ? "" : message);
        }
    }

    private void login() {
        if (!validateCredentials()) {
            return;
        }

        setBusy(true, "Signing in...");

        auth.signInWithEmailAndPassword(email(), password())
                .addOnSuccessListener(result -> routeSignedInUser())
                .addOnFailureListener(e -> {
                    setBusy(false, "");
                    showError("Sign in failed: " + safeMessage(e));
                });
    }

    private void register() {
        if (!validateCredentials()) {
            return;
        }

        String name = nameInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError(
                    "Enter your full name to create an account"
            );
            nameInput.requestFocus();
            return;
        }

        setBusy(true, "Creating your account...");

        auth.createUserWithEmailAndPassword(email(), password())
                .addOnSuccessListener(result -> {
                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        setBusy(false, "Account created. Please sign in.");
                        return;
                    }

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", name);
                    profile.put("email", user.getEmail());
                    profile.put(
                            "createdAt",
                            com.google.firebase.firestore.FieldValue
                                    .serverTimestamp()
                    );

                    db.collection("users")
                            .document(user.getUid())
                            .set(profile)
                            .addOnSuccessListener(unused -> openProfile())
                            .addOnFailureListener(e -> {
                                setBusy(false, "");
                                showError(
                                        "Account created, but profile "
                                                + "could not be saved: "
                                                + safeMessage(e)
                                );
                                openProfile();
                            });
                })
                .addOnFailureListener(e -> {
                    setBusy(false, "");
                    showError(
                            "Registration failed: " + safeMessage(e)
                    );
                });
    }

    private void resetPassword() {
        String address = email();

        if (address.isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(address).matches()) {
            emailInput.setError("Enter your email address first");
            emailInput.requestFocus();
            return;
        }

        setBusy(true, "Sending password reset email...");

        auth.sendPasswordResetEmail(address)
                .addOnSuccessListener(unused ->
                        setBusy(false,
                                "Password reset email sent. Check your inbox.")
                )
                .addOnFailureListener(e -> {
                    setBusy(false, "");
                    showError(
                            "Could not send reset email: " + safeMessage(e)
                    );
                });
    }

    private void routeSignedInUser() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            if (nameInput == null) {
                buildScreen();
            }
            setBusy(false, "Please sign in again.");
            return;
        }

        setBusy(true, "Loading your account...");

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Long age = doc.getLong("age");

                    Intent intent;

                    if (doc.exists() && age != null) {
                        intent = new Intent(
                                AuthActivity.this,
                                MainActivity.class
                        );
                    } else {
                        intent = new Intent(
                                AuthActivity.this,
                                UserProfileActivity.class
                        );
                    }

                    intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    setBusy(false, "");

                    showError(
                            "Signed in, but profile could not load: "
                                    + safeMessage(e)
                    );
                });
    }

    private void openProfile() {
        Intent intent = new Intent(
                AuthActivity.this,
                UserProfileActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        if (statusText != null) {
            statusText.setText(message);
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private String safeMessage(Exception e) {
        String message = e.getLocalizedMessage();

        if (message == null || message.trim().isEmpty()) {
            return "Check your internet connection and Firebase settings.";
        }

        return message;
    }
}

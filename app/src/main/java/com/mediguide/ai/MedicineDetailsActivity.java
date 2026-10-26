
package com.mediguide.ai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class MedicineDetailsActivity extends AppCompatActivity {

    private LinearLayout detailsContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(244, 247, 251));

        detailsContainer = new LinearLayout(this);
        detailsContainer.setOrientation(LinearLayout.VERTICAL);
        detailsContainer.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(detailsContainer);

        setContentView(scrollView);

        addText("Medicine Information", 26, true);
        addText("Loading saved medicine details...", 16, false);

        loadDetails();
    }

    private void loadDetails() {
        if (auth.getCurrentUser() == null) {
            showMessage("Please sign in to view this medicine.");
            return;
        }

        String medicineId = getIntent().getStringExtra("medicineId");

        if (medicineId == null || medicineId.trim().isEmpty()) {
            showMessage("Medicine ID is missing.");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("medicines")
                .document(medicineId)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) return;

                    detailsContainer.removeAllViews();

                    if (!document.exists()) {
                        showMessage("This medicine could not be found.");
                        return;
                    }

                    Map<String, Object> data = document.getData();

                    addText(
                            String.valueOf(data != null
                                    ? data.getOrDefault("medicineName", "Medicine")
                                    : "Medicine"),
                            24,
                            true
                    );

                    addText("Saved medicine details", 15, false);

                    if (data == null || data.isEmpty()) {
                        addText("No details are saved for this medicine.",
                                16, false);
                        return;
                    }

                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        String label = makeLabel(entry.getKey());
                        String value = formatValue(entry.getValue());

                        if (!value.trim().isEmpty()) {
                            addDetailRow(label, value);
                        }
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;

                    showMessage("Could not load medicine details.");
                    Toast.makeText(
                            this,
                            error.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String makeLabel(String key) {
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace("_", " ");

        if (spaced.isEmpty()) return key;

        return spaced.substring(0, 1).toUpperCase()
                + spaced.substring(1) + ":";
    }

    private String formatValue(Object value) {
        if (value == null) return "";

        if (value instanceof Map) {
            return value.toString();
        }

        if (value instanceof Iterable) {
            StringBuilder result = new StringBuilder();
            for (Object item : (Iterable<?>) value) {
                if (result.length() > 0) result.append(", ");
                result.append(String.valueOf(item));
            }
            return result.toString();
        }

        return String.valueOf(value);
    }

    private void addDetailRow(String label, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.rgb(23, 107, 135));
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);

        TextView content = new TextView(this);
        content.setText(value);
        content.setTextColor(Color.rgb(38, 50, 56));
        content.setTextSize(16);
        content.setPadding(0, dp(5), 0, 0);

        card.addView(title);
        card.addView(content);
        detailsContainer.addView(card);
    }

    private void showMessage(String message) {
        detailsContainer.removeAllViews();
        addText(message, 17, false);
    }

    private void addText(String message, int size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.rgb(38, 50, 56));
        text.setTextSize(size);
        text.setPadding(0, dp(8), 0, dp(8));

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        detailsContainer.addView(text);
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}

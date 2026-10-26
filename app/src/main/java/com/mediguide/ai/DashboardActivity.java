
package com.mediguide.ai;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration medicineListener;

    private TextView txtMedicineCount;
    private TextView txtToday;
    private TextView txtStatus;
    private TextView txtUpcoming;
    private LinearLayout layoutMedicineList;

    private final List<Map<String, Object>> medicines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        txtMedicineCount = findViewById(R.id.txtMedicineCount);
        txtToday = findViewById(R.id.txtToday);
        txtStatus = findViewById(R.id.txtStatus);
        txtUpcoming = findViewById(R.id.txtUpcoming);
        layoutMedicineList = findViewById(R.id.layoutMedicineList);

        txtToday.setText(new SimpleDateFormat(
                "EEEE, dd MMMM yyyy",
                Locale.getDefault()
        ).format(new Date()));

        View btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    startActivity(new Intent(
                            DashboardActivity.this,
                            NotificationActivity.class
                    )));
        }

        View btnAddMedicine = findViewById(R.id.btnDashboardAddMedicine);
        if (btnAddMedicine != null) {
            btnAddMedicine.setOnClickListener(v ->
                    startActivity(new Intent(
                            DashboardActivity.this,
                            AddMedicineActivity.class
                    )));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToMedicines();
    }

    private void listenToMedicines() {
        if (auth.getCurrentUser() == null) {
            txtMedicineCount.setText("0");
            txtStatus.setText("Please sign in to view your medicines.");
            layoutMedicineList.removeAllViews();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        if (medicineListener != null) {
            medicineListener.remove();
        }

        txtStatus.setText("Loading your medicines...");

        medicineListener = db.collection("users")
                .document(uid)
                .collection("medicines")
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (error != null) {
                        txtStatus.setText("Unable to load medicines.");
                        Toast.makeText(
                                this,
                                error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    medicines.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            Map<String, Object> data = document.getData();

                            Map<String, Object> item = data == null
                                    ? new HashMap<>()
                                    : new HashMap<>(data);

                            item.put("documentId", document.getId());
                            medicines.add(item);
                        }
                    }

                    Collections.sort(medicines,
                            Comparator.comparingInt(this::getTimeValue));

                    txtMedicineCount.setText(String.valueOf(medicines.size()));
                    displayMedicines();
                });
    }

    private void displayMedicines() {
        layoutMedicineList.removeAllViews();

        if (medicines.isEmpty()) {
            txtStatus.setText("No medicines saved yet.");
            txtUpcoming.setText("No upcoming medicine reminder");
            addText(layoutMedicineList,
                    "Add a medicine to see its name, dosage and schedule here.",
                    15, false);
            return;
        }

        txtStatus.setText("Your saved medicines");

        Map<String, Object> next = getNextMedicine();
        if (next == null) {
            txtUpcoming.setText(
                    "No later reminder time today. Check your saved schedules."
            );
        } else {
            txtUpcoming.setText(
                    "Next: " + getValue(next, "medicineName", "Medicine")
                            + " • " + formatTime(next)
            );
        }

        for (Map<String, Object> medicine : medicines) {
            addMedicineCard(medicine);
        }
    }

    private Map<String, Object> getNextMedicine() {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60
                + now.get(Calendar.MINUTE);

        for (Map<String, Object> medicine : medicines) {
            if (getTimeValue(medicine) >= currentMinutes) {
                return medicine;
            }
        }

        return null;
    }

    private void addMedicineCard(Map<String, Object> medicine) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
        );
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        addText(card,
                getValue(medicine, "medicineName", "Unnamed medicine"),
                19, true);

        addText(card,
                "Dosage: " + getValue(medicine, "dosage", "Not provided"),
                15, false);

        addText(card,
                "Frequency: " + getValue(medicine, "frequency", "Not specified"),
                15, false);

        addText(card, "Reminder: " + formatTime(medicine), 15, false);

        TextView details = new TextView(this);
        details.setText("Tap to view full medicine information  →");
        details.setTextColor(Color.rgb(23, 107, 135));
        details.setTextSize(14);
        details.setPadding(0, dp(8), 0, 0);
        card.addView(details);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    DashboardActivity.this,
                    MedicineDetailsActivity.class
            );
            intent.putExtra("medicineId",
                    String.valueOf(medicine.get("documentId")));
            startActivity(intent);
        });

        layoutMedicineList.addView(card);
    }

    private int getTimeValue(Map<String, Object> medicine) {
        return getNumber(medicine.get("hour")) * 60
                + getNumber(medicine.get("minute"));
    }

    private int getNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String formatTime(Map<String, Object> medicine) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,
                getNumber(medicine.get("hour")));
        calendar.set(Calendar.MINUTE,
                getNumber(medicine.get("minute")));

        return new SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
        ).format(calendar.getTime());
    }

    private String getValue(
            Map<String, Object> data,
            String key,
            String fallback
    ) {
        Object value = data.get(key);

        if (value == null || value.toString().trim().isEmpty()) {
            return fallback;
        }

        return value.toString();
    }

    private void addText(
            LinearLayout parent,
            String message,
            int size,
            boolean bold
    ) {
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(size);
        text.setTextColor(Color.rgb(38, 50, 56));
        text.setPadding(0, dp(4), 0, dp(4));

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        parent.addView(text);
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (medicineListener != null) {
            medicineListener.remove();
            medicineListener = null;
        }
    }
}

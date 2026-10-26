
package com.mediguide.ai;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private LinearLayout notificationList;
    private TextView txtNotificationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(244, 247, 251));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(22));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Notifications & Reminders");
        title.setTextColor(Color.rgb(23, 107, 135));
        title.setTextSize(25);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView date = new TextView(this);
        date.setText(new SimpleDateFormat(
                "EEEE, dd MMMM yyyy",
                Locale.getDefault()
        ).format(new Date()));
        date.setTextColor(Color.rgb(82, 97, 107));
        date.setTextSize(14);
        date.setPadding(0, dp(6), 0, dp(16));
        root.addView(date);

        txtNotificationStatus = new TextView(this);
        txtNotificationStatus.setText("Loading reminders...");
        txtNotificationStatus.setTextColor(Color.rgb(38, 50, 56));
        txtNotificationStatus.setTextSize(15);
        root.addView(txtNotificationStatus);

        notificationList = new LinearLayout(this);
        notificationList.setOrientation(LinearLayout.VERTICAL);
        root.addView(notificationList);

        setContentView(scrollView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForReminders();
    }

    private void listenForReminders() {
        if (auth.getCurrentUser() == null) {
            txtNotificationStatus.setText("Please sign in to view reminders.");
            return;
        }

        if (listener != null) {
            listener.remove();
        }

        String uid = auth.getCurrentUser().getUid();

        listener = db.collection("users")
                .document(uid)
                .collection("medicines")
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) return;

                    notificationList.removeAllViews();

                    if (error != null) {
                        txtNotificationStatus.setText(
                                "Could not load reminders: " + error.getMessage()
                        );
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        txtNotificationStatus.setText(
                                "No medicine reminders saved yet."
                        );
                        addCard(
                                "No reminders",
                                "Add a medicine and save its reminder time."
                        );
                        return;
                    }

                    List<DocumentSnapshot> documents =
                            new ArrayList<>(snapshot.getDocuments());

                    documents.sort((a, b) ->
                            getTimeValue(a.getData())
                                    - getTimeValue(b.getData()));

                    int currentMinutes = getCurrentMinutes();
                    int upcomingCount = 0;

                    for (DocumentSnapshot document : documents) {
                        Map<String, Object> data = document.getData();
                        if (data == null) continue;

                        String name = getValue(
                                data, "medicineName", "Medicine"
                        );

                        String time = formatTime(data);
                        int reminderMinutes = getTimeValue(data);

                        String message;
                        String heading;

                        if (reminderMinutes >= currentMinutes) {
                            heading = "Upcoming medicine reminder";
                            message = name + "\nTime: " + time
                                    + "\nCheck your prescribed schedule.";
                            upcomingCount++;
                        } else {
                            heading = "Earlier scheduled time";
                            message = name + "\nSaved time: " + time
                                    + "\nThis screen does not confirm whether "
                                    + "the dose was taken or missed.";
                        }

                        addCard(heading, message);
                    }

                    txtNotificationStatus.setText(
                            "Saved medicine schedules: " + documents.size()
                                    + " • Upcoming today: " + upcomingCount
                    );
                });
    }

    private void addCard(String heading, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(Color.rgb(23, 107, 135));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);

        TextView body = new TextView(this);
        body.setText(message);
        body.setTextColor(Color.rgb(38, 50, 56));
        body.setTextSize(15);
        body.setPadding(0, dp(8), 0, 0);

        card.addView(title);
        card.addView(body);
        notificationList.addView(card);
    }

    private int getCurrentMinutes() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60
                + now.get(Calendar.MINUTE);
    }

    private int getTimeValue(Map<String, Object> data) {
        return getNumber(data.get("hour")) * 60
                + getNumber(data.get("minute"));
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

    private String formatTime(Map<String, Object> data) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getNumber(data.get("hour")));
        calendar.set(Calendar.MINUTE, getNumber(data.get("minute")));

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

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }
}


package com.mediguide.ai;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ElderCareActivity extends AppCompatActivity {

    private EditText etElderName;
    private TextView txtElderMedicines;
    private TextView txtElderAppointments;
    private TextView txtDailyObservations;

    private SharedPreferences preferences;

    private static final String PREFS = "elder_care_data";
    private static final String MEDICINE_CHANNEL =
            "elder_medicine_reminders";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elder_care);

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        etElderName = findViewById(R.id.etElderName);
        txtElderMedicines = findViewById(R.id.txtElderMedicines);
        txtElderAppointments = findViewById(R.id.txtElderAppointments);
        txtDailyObservations = findViewById(R.id.txtDailyObservations);

        Button btnAddMedicine = findViewById(R.id.btnAddElderMedicine);
        Button btnAddAppointment = findViewById(R.id.btnAddElderAppointment);
        Button btnAddObservation = findViewById(R.id.btnAddDailyObservation);

        etElderName.setText(preferences.getString("elder_name", ""));

        etElderName.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                preferences.edit()
                        .putString(
                                "elder_name",
                                etElderName.getText().toString().trim()
                        )
                        .apply();
            }
        });

        btnAddMedicine.setOnClickListener(v -> showAddMedicineDialog());
        txtElderMedicines.setOnClickListener(v -> showMedicineActions());

        btnAddAppointment.setOnClickListener(v -> showAddAppointmentDialog());
        btnAddObservation.setOnClickListener(v -> showAddObservationDialog());

        createMedicineNotificationChannel();
        requestMedicineNotificationPermission();

        refreshMedicines();
        refreshAppointments();
        refreshObservations();

        restoreMedicineReminders();
    }

    private void showAddMedicineDialog() {
        LinearLayout layout = createDialogLayout();

        EditText name = createInput("Medicine name");
        EditText dose = createInput("Dose exactly as prescribed");
        EditText schedule = createInput(
                "Instructions (e.g. after breakfast)"
        );

        Button timeButton = new Button(this);
        timeButton.setText("Choose daily reminder time");

        final int[] selectedHour = {-1};
        final int[] selectedMinute = {-1};

        timeButton.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();

            new TimePickerDialog(
                    this,
                    (picker, hour, minute) -> {
                        selectedHour[0] = hour;
                        selectedMinute[0] = minute;

                        Calendar chosen = Calendar.getInstance();
                        chosen.set(Calendar.HOUR_OF_DAY, hour);
                        chosen.set(Calendar.MINUTE, minute);

                        timeButton.setText(
                                new SimpleDateFormat(
                                        "hh:mm a",
                                        Locale.getDefault()
                                ).format(chosen.getTime())
                        );
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    false
            ).show();
        });

        layout.addView(name);
        layout.addView(dose);
        layout.addView(schedule);
        layout.addView(timeButton);

        new AlertDialog.Builder(this)
                .setTitle("Add Medicine Reminder")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String medicineName = name.getText().toString().trim();
                    String medicineDose = dose.getText().toString().trim();
                    String medicineSchedule =
                            schedule.getText().toString().trim();

                    if (medicineName.isEmpty()
                            || medicineDose.isEmpty()
                            || medicineSchedule.isEmpty()) {
                        showMessage("Please complete all medicine details.");
                        return;
                    }

                    if (selectedHour[0] < 0) {
                        showMessage("Please choose a reminder time.");
                        return;
                    }

                    try {
                        JSONArray medicines = getArray("medicines");

                        JSONObject item = new JSONObject();
                        long medicineId = System.currentTimeMillis();

                        item.put("id", medicineId);
                        item.put("name", medicineName);
                        item.put("dose", medicineDose);
                        item.put("schedule", medicineSchedule);
                        item.put("hour", selectedHour[0]);
                        item.put("minute", selectedMinute[0]);
                        item.put("taken", false);
                        item.put("lastTakenDate", "");

                        medicines.put(item);
                        saveArray("medicines", medicines);

                        scheduleDailyMedicineReminder(item);
                        refreshMedicines();

                        showMessage("Medicine and daily reminder saved.");
                    } catch (Exception e) {
                        showMessage("Could not save medicine.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshMedicines() {
        JSONArray medicines = getArray("medicines");

        if (medicines.length() == 0) {
            txtElderMedicines.setText(
                    "No medicines added yet.\nTap Add Medicine to begin."
            );
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < medicines.length(); i++) {
            JSONObject item = medicines.optJSONObject(i);
            if (item == null) continue;

            int hour = item.optInt("hour", -1);
            int minute = item.optInt("minute", -1);
            String reminderTime = "Not scheduled";

            if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                reminderTime = new SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                ).format(calendar.getTime());
            }

            text.append(i + 1).append(". ")
                    .append(item.optString("name"))
                    .append("\nDose: ")
                    .append(item.optString("dose"))
                    .append("\nInstructions: ")
                    .append(item.optString("schedule"))
                    .append("\nDaily reminder: ")
                    .append(reminderTime)
                    .append("\nStatus: ")
                    .append(item.optBoolean("taken", false)
                            ? "Marked taken" : "Pending")
                    .append("\n\n");
        }

        text.append("Tap this list to update a medicine's status.");
        txtElderMedicines.setText(text.toString());
    }

    private void showMedicineActions() {
        JSONArray medicines = getArray("medicines");

        if (medicines.length() == 0) {
            showMessage("Add a medicine first.");
            return;
        }

        String[] choices = new String[medicines.length()];

        for (int i = 0; i < medicines.length(); i++) {
            JSONObject item = medicines.optJSONObject(i);

            choices[i] = item == null
                    ? "Medicine " + (i + 1)
                    : item.optString("name") + " — "
                    + (item.optBoolean("taken", false)
                    ? "Marked taken" : "Pending");
        }

        new AlertDialog.Builder(this)
                .setTitle("Medicine Actions")
                .setItems(choices, (dialog, index) -> {
                    JSONObject item = medicines.optJSONObject(index);
                    if (item == null) return;

                    String[] actions = {
                            item.optBoolean("taken", false)
                                    ? "Mark Pending" : "Mark Taken",
                            "Delete Medicine"
                    };

                    new AlertDialog.Builder(this)
                            .setTitle(item.optString("name"))
                            .setItems(actions, (d, action) -> {
                                if (action == 0) {
                                    toggleMedicineStatus(item, medicines);
                                } else {
                                    confirmDeleteMedicine(item);
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleMedicineStatus(
            JSONObject item,
            JSONArray medicines
    ) {
        boolean newTakenStatus = !item.optBoolean("taken", false);

        new AlertDialog.Builder(this)
                .setTitle("Confirm medicine status")
                .setMessage(
                        newTakenStatus
                                ? "Confirm that this dose was actually taken."
                                : "Mark this dose as pending?"
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    try {
                        item.put("taken", newTakenStatus);

                        String today = new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.getDefault()
                        ).format(new Date());

                        item.put(
                                "lastTakenDate",
                                newTakenStatus ? today : ""
                        );

                        saveArray("medicines", medicines);
                        refreshMedicines();
                    } catch (Exception e) {
                        showMessage("Could not update medicine status.");
                    }
                })
                .show();
    }

    private void confirmDeleteMedicine(JSONObject item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete medicine?")
                .setMessage("This will delete the medicine and cancel its reminder.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    long id = item.optLong("id", 0L);
                    JSONArray medicines = getArray("medicines");
                    JSONArray updated = new JSONArray();

                    for (int i = 0; i < medicines.length(); i++) {
                        JSONObject current = medicines.optJSONObject(i);
                        if (current == null) continue;

                        if (current.optLong("id", -1L) != id) {
                            updated.put(current);
                        }
                    }

                    saveArray("medicines", updated);
                    cancelMedicineReminder(id);
                    refreshMedicines();
                    showMessage("Medicine deleted.");
                })
                .show();
    }

    private void scheduleDailyMedicineReminder(JSONObject item) {
        int hour = item.optInt("hour", -1);
        int minute = item.optInt("minute", -1);
        long id = item.optLong("id", 0L);

        if (hour < 0 || hour > 23
                || minute < 0 || minute > 59 || id == 0L) {
            return;
        }

        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (next.getTimeInMillis() <= System.currentTimeMillis()) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(
                this,
                ElderMedicineReminderReceiver.class
        );

        intent.setAction("com.mediguide.ai.ELDER_MEDICINE_REMINDER");
        intent.putExtra("medicineId", id);
        intent.putExtra("medicineName", item.optString("name"));
        intent.putExtra("medicineDose", item.optString("dose"));
        intent.putExtra("medicineSchedule", item.optString("schedule"));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                getMedicineRequestCode(id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    next.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    private void cancelMedicineReminder(long id) {
        Intent intent = new Intent(
                this,
                ElderMedicineReminderReceiver.class
        );

        intent.setAction("com.mediguide.ai.ELDER_MEDICINE_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                getMedicineRequestCode(id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        pendingIntent.cancel();
    }

    private int getMedicineRequestCode(long id) {
        return (int) (id & 0x7fffffff);
    }

    private void restoreMedicineReminders() {
        JSONArray medicines = getArray("medicines");

        for (int i = 0; i < medicines.length(); i++) {
            JSONObject item = medicines.optJSONObject(i);
            if (item != null && item.has("hour") && item.has("minute")) {
                scheduleDailyMedicineReminder(item);
            }
        }
    }

    private void createMedicineNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MEDICINE_CHANNEL,
                    "Elder Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Daily elder medicine reminders");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void requestMedicineNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private void showAddAppointmentDialog() {
        LinearLayout layout = createDialogLayout();

        EditText doctor = createInput("Doctor's name");
        EditText date = createInput("Date (e.g. 25 Oct 2026)");
        EditText time = createInput("Time (e.g. 10:30 AM)");
        EditText reason = createInput("Reason for visit");

        layout.addView(doctor);
        layout.addView(date);
        layout.addView(time);
        layout.addView(reason);

        new AlertDialog.Builder(this)
                .setTitle("Add Doctor Appointment")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String doctorName = doctor.getText().toString().trim();
                    String appointmentDate = date.getText().toString().trim();
                    String appointmentTime = time.getText().toString().trim();
                    String appointmentReason = reason.getText().toString().trim();

                    if (doctorName.isEmpty() || appointmentDate.isEmpty()
                            || appointmentTime.isEmpty()
                            || appointmentReason.isEmpty()) {
                        showMessage("Please complete all appointment details.");
                        return;
                    }

                    try {
                        JSONArray appointments = getArray("appointments");
                        JSONObject item = new JSONObject();

                        item.put("doctor", doctorName);
                        item.put("date", appointmentDate);
                        item.put("time", appointmentTime);
                        item.put("reason", appointmentReason);

                        appointments.put(item);
                        saveArray("appointments", appointments);
                        refreshAppointments();
                        showMessage("Appointment saved.");
                    } catch (Exception e) {
                        showMessage("Could not save appointment.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshAppointments() {
        JSONArray appointments = getArray("appointments");

        if (appointments.length() == 0) {
            txtElderAppointments.setText("No appointments added yet.");
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < appointments.length(); i++) {
            JSONObject item = appointments.optJSONObject(i);
            if (item == null) continue;

            text.append(i + 1).append(". ")
                    .append(item.optString("doctor"))
                    .append("\nDate: ")
                    .append(item.optString("date"))
                    .append("\nTime: ")
                    .append(item.optString("time"))
                    .append("\nReason: ")
                    .append(item.optString("reason"))
                    .append("\n\n");
        }

        txtElderAppointments.setText(text.toString());
    }

    private void showAddObservationDialog() {
        LinearLayout layout = createDialogLayout();

        EditText observation = createInput(
                "Observation (e.g. appetite, sleep, mood)"
        );

        observation.setSingleLine(false);
        observation.setMinLines(3);
        observation.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        layout.addView(observation);

        new AlertDialog.Builder(this)
                .setTitle("Daily Observation")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String note = observation.getText().toString().trim();

                    if (note.isEmpty()) {
                        showMessage("Enter an observation first.");
                        return;
                    }

                    try {
                        JSONArray records = getArray("observations");
                        JSONObject item = new JSONObject();

                        item.put(
                                "date",
                                DateFormat.getDateTimeInstance().format(new Date())
                        );
                        item.put("note", note);

                        records.put(item);
                        saveArray("observations", records);
                        refreshObservations();
                        showMessage("Observation saved.");
                    } catch (Exception e) {
                        showMessage("Could not save observation.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshObservations() {
        JSONArray records = getArray("observations");

        if (records.length() == 0) {
            txtDailyObservations.setText(
                    "No daily observations recorded yet."
            );
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject item = records.optJSONObject(i);
            if (item == null) continue;

            text.append(item.optString("date"))
                    .append("\n")
                    .append(item.optString("note"))
                    .append("\n\n");
        }

        txtDailyObservations.setText(text.toString());
    }

    private JSONArray getArray(String key) {
        try {
            return new JSONArray(preferences.getString(key, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveArray(String key, JSONArray array) {
        preferences.edit().putString(key, array.toString()).apply();
    }

    private LinearLayout createDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        layout.setPadding(padding, dp(8), padding, dp(8));
        return layout;
    }

    private EditText createInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return input;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

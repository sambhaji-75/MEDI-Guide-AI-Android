
package com.mediguide.ai;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class BabyCareActivity extends AppCompatActivity {

    private EditText etBabyName;
    private TextView txtVaccinationList;
    private TextView txtMilestoneInfo;
    private TextView txtAppointmentList;
    private Spinner spBabyAge;
    private SharedPreferences preferences;

    private static final String PREFS = "baby_care_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_baby_care);

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

        etBabyName = findViewById(R.id.etBabyName);
        txtVaccinationList = findViewById(R.id.txtVaccinationList);
        txtMilestoneInfo = findViewById(R.id.txtMilestoneInfo);
        txtAppointmentList = findViewById(R.id.txtAppointmentList);
        spBabyAge = findViewById(R.id.spBabyAge);

        Button btnAddVaccination = findViewById(R.id.btnAddVaccination);
        Button btnSaveGrowth = findViewById(R.id.btnSaveGrowth);
        Button btnAddAppointment = findViewById(R.id.btnAddAppointment);

        etBabyName.setText(preferences.getString("baby_name", ""));

        String[] ages = {
                "Select baby's age",
                "0–2 months",
                "3–5 months",
                "6–8 months",
                "9–11 months",
                "12–18 months",
                "18–24 months",
                "2–3 years"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ages);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spBabyAge.setAdapter(adapter);

        spBabyAge.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               android.view.View view,
                                               int position, long id) {
                        showMilestoneInfo(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        etBabyName.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                saveBabyName();
            }
        });

        btnAddVaccination.setOnClickListener(
                v -> showAddVaccinationDialog());
        txtVaccinationList.setOnClickListener(
                v -> showVaccinationActions());
        btnSaveGrowth.setOnClickListener(v -> showGrowthDialog());
        btnAddAppointment.setOnClickListener(
                v -> showAddAppointmentDialog());

        refreshVaccinations();
        refreshAppointments();
    }

    @Override
    protected void onPause() {
        saveBabyName();
        super.onPause();
    }

    private void saveBabyName() {
        if (preferences != null && etBabyName != null) {
            preferences.edit().putString(
                    "baby_name",
                    etBabyName.getText().toString().trim()
            ).apply();
        }
    }

    private void showMilestoneInfo(int position) {
        String info;

        switch (position) {
            case 1:
                info = "0–2 months\n\nLooks at faces\nReacts to sounds\nMay briefly lift the head\n\nDevelopment varies between babies.";
                break;
            case 2:
                info = "3–5 months\n\nSmiles at people\nHolds the head more steadily\nReaches for objects\n\nObserve your baby's progress.";
                break;
            case 3:
                info = "6–8 months\n\nMay sit with support\nExplores objects with hands\nResponds to familiar people\n\nSkills develop at different rates.";
                break;
            case 4:
                info = "9–11 months\n\nMay sit without support\nMay move in different ways\nResponds to their name\n\nNot every baby crawls.";
                break;
            case 5:
                info = "12–18 months\n\nMay take first steps\nMay use simple words\nExplores surroundings\n\nDiscuss concerns with your pediatrician.";
                break;
            case 6:
                info = "18–24 months\n\nUses more words over time\nBegins pretend play\nFollows some simple instructions\n\nDevelopment varies.";
                break;
            case 7:
                info = "2–3 years\n\nUses short phrases\nPlays with others\nImproves coordination\n\nAsk your pediatrician about concerns.";
                break;
            default:
                info = "Select an age range to view general developmental milestones.";
        }

        txtMilestoneInfo.setText(info);
    }

    private void showAddVaccinationDialog() {
        LinearLayout layout = createDialogLayout();

        EditText nameInput = createInput("Vaccine name");
        TextView dateLabel = new TextView(this);
        TextView timeLabel = new TextView(this);

        dateLabel.setText("Due date: Not selected");
        dateLabel.setTextColor(Color.DKGRAY);
        dateLabel.setPadding(0, 16, 0, 8);

        timeLabel.setText("Reminder time: Not selected");
        timeLabel.setTextColor(Color.DKGRAY);
        timeLabel.setPadding(0, 12, 0, 8);

        layout.addView(nameInput);
        layout.addView(dateLabel);
        layout.addView(makePickerButton("Choose due date", v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (picker, year, month, day) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(year, month, day, 0, 0, 0);
                chosen.set(Calendar.MILLISECOND, 0);
                dateLabel.setTag(chosen.getTimeInMillis());
                dateLabel.setText("Due date: " + DateFormat.getDateInstance()
                        .format(chosen.getTime()));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)).show();
        }));

        layout.addView(timeLabel);
        layout.addView(makePickerButton("Choose reminder time", v -> {
            Calendar now = Calendar.getInstance();
            new TimePickerDialog(this, (picker, hour, minute) -> {
                timeLabel.setTag(hour * 60 + minute);
                Calendar display = Calendar.getInstance();
                display.set(Calendar.HOUR_OF_DAY, hour);
                display.set(Calendar.MINUTE, minute);
                timeLabel.setText("Reminder time: " +
                        DateFormat.getTimeInstance(DateFormat.SHORT)
                                .format(display.getTime()));
            }, now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE), false).show();
        }));

        new AlertDialog.Builder(this)
                .setTitle("Add Vaccination Reminder")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();

                    if (name.isEmpty()
                            || !(dateLabel.getTag() instanceof Long)
                            || !(timeLabel.getTag() instanceof Integer)) {
                        showMessage("Enter the vaccine name and select date and time.");
                        return;
                    }

                    long dueDate = (Long) dateLabel.getTag();
                    int minutes = (Integer) timeLabel.getTag();

                    Calendar reminder = Calendar.getInstance();
                    reminder.setTimeInMillis(dueDate);
                    reminder.set(Calendar.HOUR_OF_DAY, minutes / 60);
                    reminder.set(Calendar.MINUTE, minutes % 60);
                    reminder.set(Calendar.SECOND, 0);
                    reminder.set(Calendar.MILLISECOND, 0);

                    long triggerAt = reminder.getTimeInMillis();

                    if (triggerAt <= System.currentTimeMillis()) {
                        showMessage("Please choose a future date and time.");
                        return;
                    }

                    try {
                        JSONArray list = getArray("vaccinations");
                        JSONObject item = new JSONObject();
                        long id = System.currentTimeMillis();

                        item.put("id", id);
                        item.put("name", name);
                        item.put("dueAt", triggerAt);
                        item.put("completed", false);
                        list.put(item);
                        saveArray("vaccinations", list);

                        scheduleVaccinationReminder(id, name, triggerAt);
                        refreshVaccinations();
                        showMessage("Vaccination reminder saved.");
                    } catch (Exception e) {
                        showMessage("Could not save vaccination reminder.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Button makePickerButton(String label,
                                    android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private void scheduleVaccinationReminder(long id, String name,
                                             long triggerAt) {
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            showMessage("Alarm service is unavailable on this device.");
            return;
        }

        Intent intent = new Intent(this, BabyVaccinationReminderReceiver.class);
        intent.putExtra("vaccinationId", id);
        intent.putExtra("vaccinationName", name);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode(id), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } catch (SecurityException e) {
            showMessage("Android blocked the reminder. Check notification settings.");
        }
    }

    private void cancelVaccinationReminder(long id) {
        AlarmManager alarmManager =
                (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, BabyVaccinationReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode(id), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    private int requestCode(long id) {
        return (int) (id ^ (id >>> 32));
    }

    private void refreshVaccinations() {
        JSONArray list = getArray("vaccinations");

        if (list.length() == 0) {
            txtVaccinationList.setText("No vaccinations added yet.\nTap Add Vaccination Reminder to begin.");
            return;
        }

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) continue;

            long dueAt = item.optLong("dueAt", 0);
            String due = dueAt > 0
                    ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                    DateFormat.SHORT).format(new Date(dueAt))
                    : "Date not available";

            text.append(i + 1).append(". ")
                    .append(item.optString("name"))
                    .append("\nDue: ").append(due)
                    .append("\nStatus: ")
                    .append(item.optBoolean("completed", false)
                            ? "Completed" : "Pending")
                    .append("\n\n");
        }

        text.append("Tap this list to manage reminders.");
        txtVaccinationList.setText(text.toString());
    }

    private void showVaccinationActions() {
        JSONArray list = getArray("vaccinations");
        if (list.length() == 0) {
            showMessage("Add a vaccination first.");
            return;
        }

        String[] choices = new String[list.length()];
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            choices[i] = item == null ? "Vaccination " + (i + 1)
                    : item.optString("name") + " — "
                    + (item.optBoolean("completed", false)
                    ? "Completed" : "Pending");
        }

        new AlertDialog.Builder(this)
                .setTitle("Manage Vaccination")
                .setItems(choices, (dialog, index) -> {
                    JSONObject item = list.optJSONObject(index);
                    if (item == null) return;

                    long id = item.optLong("id", 0);
                    boolean completed = item.optBoolean("completed", false);

                    String[] actions = completed
                            ? new String[]{"Mark Pending", "Delete"}
                            : new String[]{"Mark Completed", "Delete"};

                    new AlertDialog.Builder(this)
                            .setTitle(item.optString("name"))
                            .setItems(actions, (d, action) -> {
                                if (action == 0) {
                                    try {
                                        item.put("completed", !completed);
                                        saveArray("vaccinations", list);

                                        if (!completed) {
                                            cancelVaccinationReminder(id);
                                        } else {
                                            long dueAt = item.optLong("dueAt", 0);
                                            if (dueAt > System.currentTimeMillis()) {
                                                scheduleVaccinationReminder(
                                                        id, item.optString("name"), dueAt);
                                            }
                                        }

                                        refreshVaccinations();
                                    } catch (Exception e) {
                                        showMessage("Could not update vaccination.");
                                    }
                                } else {
                                    cancelVaccinationReminder(id);
                                    list.remove(index);
                                    saveArray("vaccinations", list);
                                    refreshVaccinations();
                                    showMessage("Vaccination deleted.");
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGrowthDialog() {
        LinearLayout layout = createDialogLayout();
        EditText heightInput = createInput("Height in cm");
        EditText weightInput = createInput("Weight in kg");

        heightInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        layout.addView(heightInput);
        layout.addView(weightInput);

        new AlertDialog.Builder(this)
                .setTitle("Record Growth")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double height = Double.parseDouble(
                                heightInput.getText().toString().trim());
                        double weight = Double.parseDouble(
                                weightInput.getText().toString().trim());

                        if (height <= 0 || weight <= 0) {
                            showMessage("Measurements must be greater than zero.");
                            return;
                        }

                        JSONArray records = getArray("growth");
                        JSONObject record = new JSONObject();
                        record.put("age", spBabyAge.getSelectedItem().toString());
                        record.put("height", height);
                        record.put("weight", weight);
                        record.put("date", DateFormat.getDateInstance()
                                .format(new Date()));

                        records.put(record);
                        saveArray("growth", records);
                        showGrowthRecords();
                    } catch (Exception e) {
                        showMessage("Enter valid height and weight.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showGrowthRecords() {
        JSONArray records = getArray("growth");
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < records.length(); i++) {
            JSONObject item = records.optJSONObject(i);
            if (item == null) continue;

            text.append(item.optString("date"))
                    .append(" — ").append(item.optString("age"))
                    .append("\nHeight: ").append(item.optDouble("height"))
                    .append(" cm | Weight: ").append(item.optDouble("weight"))
                    .append(" kg\n\n");
        }

        if (records.length() == 0) text.append("No growth records yet.");

        new AlertDialog.Builder(this)
                .setTitle("Growth Records")
                .setMessage(text.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAddAppointmentDialog() {
        LinearLayout layout = createDialogLayout();
        EditText doctorInput = createInput("Doctor's name");
        EditText dateInput = createInput("Date (e.g. 15 Oct 2026)");
        EditText timeInput = createInput("Time (e.g. 10:30 AM)");
        EditText reasonInput = createInput("Purpose of visit");

        layout.addView(doctorInput);
        layout.addView(dateInput);
        layout.addView(timeInput);
        layout.addView(reasonInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Doctor Appointment")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String doctor = doctorInput.getText().toString().trim();
                    String date = dateInput.getText().toString().trim();
                    String time = timeInput.getText().toString().trim();
                    String reason = reasonInput.getText().toString().trim();

                    if (doctor.isEmpty() || date.isEmpty()
                            || time.isEmpty() || reason.isEmpty()) {
                        showMessage("Fill in all appointment details.");
                        return;
                    }

                    try {
                        JSONArray list = getArray("appointments");
                        JSONObject appointment = new JSONObject();
                        appointment.put("doctor", doctor);
                        appointment.put("date", date);
                        appointment.put("time", time);
                        appointment.put("reason", reason);
                        list.put(appointment);
                        saveArray("appointments", list);
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
        JSONArray list = getArray("appointments");

        if (list.length() == 0) {
            txtAppointmentList.setText("No appointments added yet.");
            return;
        }

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            if (item == null) continue;

            text.append(i + 1).append(". ")
                    .append(item.optString("doctor"))
                    .append("\nDate: ").append(item.optString("date"))
                    .append("\nTime: ").append(item.optString("time"))
                    .append("\nPurpose: ").append(item.optString("reason"))
                    .append("\n\n");
        }
        txtAppointmentList.setText(text.toString());
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

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, 8, padding, 8);

        return layout;
    }

    private EditText createInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.GRAY);
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return input;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}


package com.mediguide.ai;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MotherCareActivity extends AppCompatActivity {

    private Spinner spPregnancyStage;
    private TextView txtStageInfo, txtAppointmentEmpty;
    private Button btnNutrition, btnActivities, btnHealth;
    private Button btnPickDate, btnPickTime, btnSaveAppointment;
    private EditText edtDoctorName, edtClinic, edtAppointmentNotes;
    private LinearLayout layoutAppointments;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration appointmentListener;

    private long selectedAppointmentTime = 0L;

    private final String[] stages = {
            "First Trimester (Weeks 1–12)",
            "Second Trimester (Weeks 13–27)",
            "Third Trimester (Weeks 28–Birth)"
    };

    private final String[] stageInfo = {
            "Weeks 1–12\n\nYour body begins adjusting to pregnancy. Arrange prenatal care, discuss supplements with your healthcare professional, eat balanced meals and get adequate rest.",
            "Weeks 13–27\n\nContinue prenatal check-ups, eat a variety of nutritious foods, stay hydrated and discuss suitable physical activity with your healthcare professional.",
            "Weeks 28–Birth\n\nContinue prenatal visits, follow your care team's advice, rest when needed and learn the warning signs that require urgent medical attention."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mother_care);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupPregnancyStages();
        setupInformationButtons();
        setupAppointmentButtons();
        createNotificationChannel();
        requestNotificationPermission();
    }

    private void bindViews() {
        spPregnancyStage = findViewById(R.id.spPregnancyStage);
        txtStageInfo = findViewById(R.id.txtStageInfo);
        btnNutrition = findViewById(R.id.btnNutrition);
        btnActivities = findViewById(R.id.btnActivities);
        btnHealth = findViewById(R.id.btnHealth);

        edtDoctorName = findViewById(R.id.edtDoctorName);
        edtClinic = findViewById(R.id.edtClinic);
        edtAppointmentNotes = findViewById(R.id.edtAppointmentNotes);

        btnPickDate = findViewById(R.id.btnPickDate);
        btnPickTime = findViewById(R.id.btnPickTime);
        btnSaveAppointment = findViewById(R.id.btnSaveAppointment);

        txtAppointmentEmpty = findViewById(R.id.txtAppointmentEmpty);
        layoutAppointments = findViewById(R.id.layoutAppointments);
    }

    private void setupPregnancyStages() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                stages
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spPregnancyStage.setAdapter(adapter);

        spPregnancyStage.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        txtStageInfo.setText(stageInfo[position]);
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                }
        );
    }

    private void setupInformationButtons() {
        btnNutrition.setOnClickListener(v -> showInfo(
                "Nutrition & Healthy Eating",
                "• Eat a variety of vegetables, fruits, whole grains, protein foods and pasteurized dairy or suitable alternatives.\n\n"
                        + "• Drink water regularly.\n\n"
                        + "• Ask your healthcare professional about folic acid, iron and other prenatal supplements.\n\n"
                        + "• Avoid alcohol and foods that may carry infection risks, including unpasteurized dairy and undercooked meat or eggs.\n\n"
                        + "• Follow individual advice if you have diabetes, allergies or other medical conditions."
        ));

        btnActivities.setOnClickListener(v -> showInfo(
                "Safe Activities",
                "• Walking and gentle, pregnancy-appropriate movement may be suitable for many uncomplicated pregnancies.\n\n"
                        + "• Start gradually, stay hydrated and avoid overheating.\n\n"
                        + "• Avoid activities with a high risk of falling, abdominal injury or collision.\n\n"
                        + "• Ask your healthcare professional which activities are appropriate for your pregnancy.\n\n"
                        + "• Stop exercising and seek medical advice if you experience warning symptoms."
        ));

        btnHealth.setOnClickListener(v -> showInfo(
                "Health & Safety",
                "Attend scheduled prenatal check-ups and take medicines or supplements only as advised.\n\n"
                        + "Seek urgent medical care for heavy bleeding, severe abdominal pain, difficulty breathing, fainting, severe headache or vision changes, fluid leaking, or reduced fetal movement when movement is normally expected.\n\n"
                        + "For an emergency, contact local emergency services or go to the nearest hospital."
        ));
    }

    private void setupAppointmentButtons() {
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnSaveAppointment.setOnClickListener(v -> saveAppointment());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (selectedAppointmentTime > 0) {
            calendar.setTimeInMillis(selectedAppointmentTime);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (picker, year, month, day) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.setTimeInMillis(
                            selectedAppointmentTime > 0
                                    ? selectedAppointmentTime
                                    : System.currentTimeMillis()
                    );

                    chosen.set(Calendar.YEAR, year);
                    chosen.set(Calendar.MONTH, month);
                    chosen.set(Calendar.DAY_OF_MONTH, day);

                    selectedAppointmentTime = chosen.getTimeInMillis();

                    btnPickDate.setText(new SimpleDateFormat(
                            "dd MMM yyyy", Locale.getDefault()
                    ).format(chosen.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();

        if (selectedAppointmentTime > 0) {
            calendar.setTimeInMillis(selectedAppointmentTime);
        }

        new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    Calendar chosen = Calendar.getInstance();

                    if (selectedAppointmentTime > 0) {
                        chosen.setTimeInMillis(selectedAppointmentTime);
                    }

                    chosen.set(Calendar.HOUR_OF_DAY, hour);
                    chosen.set(Calendar.MINUTE, minute);
                    chosen.set(Calendar.SECOND, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    selectedAppointmentTime = chosen.getTimeInMillis();

                    btnPickTime.setText(new SimpleDateFormat(
                            "hh:mm a", Locale.getDefault()
                    ).format(chosen.getTime()));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void saveAppointment() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_LONG).show();
            return;
        }

        String doctor = edtDoctorName.getText().toString().trim();
        String clinic = edtClinic.getText().toString().trim();
        String notes = edtAppointmentNotes.getText().toString().trim();

        if (doctor.isEmpty()) {
            edtDoctorName.setError("Enter doctor name");
            return;
        }

        if (clinic.isEmpty()) {
            edtClinic.setError("Enter hospital or clinic");
            return;
        }

        if (selectedAppointmentTime <= System.currentTimeMillis()) {
            Toast.makeText(
                    this,
                    "Choose a future appointment date and time.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("doctorName", doctor);
        appointment.put("clinic", clinic);
        appointment.put("notes", notes);
        appointment.put("appointmentTime", selectedAppointmentTime);
        appointment.put("createdAt", System.currentTimeMillis());

        String uid = auth.getCurrentUser().getUid();

        btnSaveAppointment.setEnabled(false);

        db.collection("users")
                .document(uid)
                .collection("motherAppointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    scheduleReminder(
                            documentReference.getId(),
                            doctor,
                            clinic,
                            selectedAppointmentTime
                    );

                    Toast.makeText(
                            this,
                            "Appointment saved successfully.",
                            Toast.LENGTH_LONG
                    ).show();

                    edtDoctorName.setText("");
                    edtClinic.setText("");
                    edtAppointmentNotes.setText("");
                    selectedAppointmentTime = 0L;

                    btnPickDate.setText("Choose appointment date");
                    btnPickTime.setText("Choose appointment time");
                    btnSaveAppointment.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    btnSaveAppointment.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Could not save appointment: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForAppointments();
    }

    private void listenForAppointments() {
        if (auth.getCurrentUser() == null) {
            txtAppointmentEmpty.setText("Please sign in to view appointments.");
            return;
        }

        if (appointmentListener != null) {
            appointmentListener.remove();
        }

        String uid = auth.getCurrentUser().getUid();

        appointmentListener = db.collection("users")
                .document(uid)
                .collection("motherAppointments")
                .orderBy("appointmentTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        txtAppointmentEmpty.setText(
                                "Unable to load appointments. Check your connection."
                        );
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    layoutAppointments.removeAllViews();

                    if (snapshot.isEmpty()) {
                        txtAppointmentEmpty.setVisibility(View.VISIBLE);
                        txtAppointmentEmpty.setText("No appointments saved yet.");
                        return;
                    }

                    txtAppointmentEmpty.setVisibility(View.GONE);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String doctor = doc.getString("doctorName");
                        String clinic = doc.getString("clinic");
                        String notes = doc.getString("notes");
                        Long time = doc.getLong("appointmentTime");

                        if (time == null) {
                            continue;
                        }

                        addAppointmentCard(
                                doc.getId(),
                                doctor == null ? "Doctor" : doctor,
                                clinic == null ? "" : clinic,
                                notes == null ? "" : notes,
                                time
                        );
                    }
                });
    }

    private void addAppointmentCard(
            String id,
            String doctor,
            String clinic,
            String notes,
            long time
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(10));
        card.setLayoutParams(params);
        card.setElevation(dp(2));

        TextView details = new TextView(this);
        details.setText(
                "Doctor: " + doctor
                        + "\nHospital: " + clinic
                        + "\nDate: " + new SimpleDateFormat(
                        "dd MMM yyyy", Locale.getDefault()
                ).format(new Date(time))
                        + "\nTime: " + new SimpleDateFormat(
                        "hh:mm a", Locale.getDefault()
                ).format(new Date(time))
                        + (notes.isEmpty() ? "" : "\nNotes: " + notes)
        );
        details.setTextColor(0xFF263653);
        details.setTextSize(15);
        details.setLineSpacing(dp(4), 1f);

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete Appointment");
        deleteButton.setAllCaps(false);
        deleteButton.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Delete appointment?")
                        .setMessage("This will remove the saved appointment.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) ->
                                deleteAppointment(id, time))
                        .show()
        );

        card.addView(details);
        card.addView(deleteButton);
        layoutAppointments.addView(card);
    }

    private void deleteAppointment(String id, long appointmentTime) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("motherAppointments")
                .document(id)
                .delete()
                .addOnSuccessListener(unused -> {
                    cancelReminder(id, appointmentTime);
                    Toast.makeText(
                            this,
                            "Appointment deleted.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Delete failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void scheduleReminder(
            String appointmentId,
            String doctor,
            String clinic,
            long appointmentTime
    ) {
        // Reminder is scheduled for 30 minutes before the appointment.
        long reminderTime = appointmentTime - (30 * 60 * 1000L);

        // If less than 30 minutes remain, remind at the appointment time.
        if (reminderTime <= System.currentTimeMillis()) {
            reminderTime = appointmentTime;
        }

        Intent intent = new Intent(this, AppointmentReminderReceiver.class);
        intent.setAction("com.mediguide.ai.APPOINTMENT_REMINDER");
        intent.putExtra("appointmentId", appointmentId);
        intent.putExtra("doctorName", doctor);
        intent.putExtra("clinic", clinic);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                appointmentId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        // Inexact alarm avoids requiring special exact-alarm access.
        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
        );
    }

    private void cancelReminder(String appointmentId, long appointmentTime) {
        Intent intent = new Intent(this, AppointmentReminderReceiver.class);
        intent.setAction("com.mediguide.ai.APPOINTMENT_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                appointmentId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) getSystemService(ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "mother_appointment_reminders",
                    "Mother Care Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for upcoming doctor appointments");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (appointmentListener != null) {
            appointmentListener.remove();
            appointmentListener = null;
        }
    }
}

package com.mediguide.ai;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.UUID;

public class AddMedicineActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtDosage, edtFrequency;
    private TimePicker timePicker;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        edtName = findViewById(R.id.edtMedicineName);
        edtDosage = findViewById(R.id.edtDosage);
        edtFrequency = findViewById(R.id.edtFrequency);
        timePicker = findViewById(R.id.timePicker);
        Button save = findViewById(R.id.btnSaveMedicine);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        save.setOnClickListener(v -> saveMedicine());
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "No active Firebase session. Please login again.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Signed in: " + auth.getCurrentUser().getUid(),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void saveMedicine() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = value(edtName);
        String dosage = value(edtDosage);
        String frequency = value(edtFrequency);

        if (name.isEmpty() || dosage.isEmpty() || frequency.isEmpty()) {
            Toast.makeText(this, "Fill all medicine fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        String uid = auth.getCurrentUser().getUid();
        String medicineId = UUID.randomUUID().toString();

        MedicineModel medicine = new MedicineModel(
                medicineId, uid, name, dosage, hour, minute, frequency,
                System.currentTimeMillis());

        db.collection("users").document(uid)
                .collection("medicines").document(medicineId)
                .set(medicine)
                .addOnSuccessListener(unused -> {
                    scheduleAlarm(medicine);
                    Toast.makeText(this, "Medicine saved and reminder scheduled.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String value(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void scheduleAlarm(MedicineModel medicine) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "Allow exact alarms, then save the medicine again.", Toast.LENGTH_LONG).show();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, medicine.getHour());
        calendar.set(Calendar.MINUTE, medicine.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicineId", medicine.getId());
        intent.putExtra("medicineName", medicine.getMedicineName());
        intent.putExtra("dosage", medicine.getDosage());
        intent.putExtra("uid", medicine.getUserId());
        intent.putExtra("hour", medicine.getHour());
        intent.putExtra("minute", medicine.getMinute());
        intent.putExtra("frequency", medicine.getFrequency());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                medicine.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent);
    }
}

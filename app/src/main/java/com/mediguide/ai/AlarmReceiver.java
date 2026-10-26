package com.mediguide.ai;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_alerts";
    private static final int SNOOZE_MINUTES = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("TAKEN".equals(action) || "MISSED".equals(action)) {
            saveLog(intent, action);
            cancelNotification(context, intent);
            return;
        }

        if ("SNOOZE".equals(action)) {
            saveLog(intent, "SNOOZE");
            cancelNotification(context, intent);
            scheduleSnooze(context, intent);
            return;
        }

        showMedicineNotification(context, intent);
    }

    private void showMedicineNotification(
            Context context, Intent intent) {

        createChannel(context);

        String id = intent.getStringExtra("medicineId");
        String name = intent.getStringExtra("medicineName");
        String dosage = intent.getStringExtra("dosage");
        String uid = intent.getStringExtra("uid");

        if (id == null || name == null || uid == null) {
            return;
        }

        PendingIntent taken =
                actionPendingIntent(context, "TAKEN", intent);

        PendingIntent snooze =
                actionPendingIntent(context, "SNOOZE", intent);

        PendingIntent missed =
                actionPendingIntent(context, "MISSED", intent);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("MediGuide-AI Medicine Reminder")
                        .setContentText(name + " • "
                                + (dosage == null ? "" : dosage))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .addAction(0, "Taken", taken)
                        .addAction(0, "Snooze 10m", snooze)
                        .addAction(0, "Missed", missed);

        if (Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

            NotificationManagerCompat.from(context)
                    .notify(id.hashCode(), builder.build());
        }

        scheduleNextDaily(context, intent);
    }

    private PendingIntent actionPendingIntent(
            Context context, String action, Intent source) {

        Intent actionIntent =
                new Intent(context, AlarmReceiver.class);

        actionIntent.setAction(action);
        copyExtras(source, actionIntent);

        String id = source.getStringExtra("medicineId");
        int requestCode = (id + "_" + action).hashCode();

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void scheduleSnooze(
            Context context, Intent source) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager == null || !canScheduleExact(context, alarmManager)) {
            return;
        }

        Intent snoozeIntent =
                new Intent(context, AlarmReceiver.class);

        copyExtras(source, snoozeIntent);
        snoozeIntent.setAction(null);

        String id = source.getStringExtra("medicineId");

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        (id + "_snooze").hashCode(),
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis()
                        + SNOOZE_MINUTES * 60_000L,
                pendingIntent
        );
    }

    private void scheduleNextDaily(
            Context context, Intent source) {

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager == null || !canScheduleExact(context, alarmManager)) {
            return;
        }

        int hour = source.getIntExtra("hour", -1);
        int minute = source.getIntExtra("minute", -1);

        // Do not invent a reminder time if it was not supplied.
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
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

        Intent nextIntent =
                new Intent(context, AlarmReceiver.class);

        copyExtras(source, nextIntent);
        nextIntent.setAction(null);

        String id = source.getStringExtra("medicineId");

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        id.hashCode(),
                        nextIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.getTimeInMillis(),
                pendingIntent
        );
    }

    private boolean canScheduleExact(
            Context context, AlarmManager alarmManager) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
            return false;
        }

        return true;
    }

    private void copyExtras(Intent source, Intent destination) {
        String[] stringKeys = {
                "medicineId", "medicineName", "dosage",
                "uid", "frequency"
        };

        for (String key : stringKeys) {
            destination.putExtra(
                    key, source.getStringExtra(key));
        }

        destination.putExtra("hour", source.getIntExtra("hour", -1));
        destination.putExtra("minute", source.getIntExtra("minute", -1));
    }

    private void saveLog(Intent intent, String status) {
        String uid = intent.getStringExtra("uid");
        String medicineId = intent.getStringExtra("medicineId");

        if (uid == null || medicineId == null) {
            return;
        }

        Map<String, Object> log = new HashMap<>();
        log.put("medicineId", medicineId);
        log.put("medicineName",
                intent.getStringExtra("medicineName"));
        log.put("dosage", intent.getStringExtra("dosage"));
        log.put("frequency", intent.getStringExtra("frequency"));
        log.put("status", status);
        log.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("medicineLogs")
                .add(log)
                .addOnFailureListener(error ->
                        android.util.Log.e(
                                "AlarmReceiver",
                                "Could not save medicine log",
                                error
                        ));
    }

    private void cancelNotification(
            Context context, Intent intent) {

        String id = intent.getStringExtra("medicineId");

        if (id != null) {
            NotificationManagerCompat.from(context)
                    .cancel(id.hashCode());
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Medicine Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "MediGuide-AI personalized medicine reminders");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
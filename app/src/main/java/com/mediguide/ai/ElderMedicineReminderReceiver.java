
package com.mediguide.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ElderMedicineReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "elder_medicine_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String medicineName =
                intent.getStringExtra("medicineName");
        String dose =
                intent.getStringExtra("medicineDose");
        String instructions =
                intent.getStringExtra("medicineSchedule");

        if (medicineName == null || medicineName.trim().isEmpty()) {
            medicineName = "Prescribed medicine";
        }

        if (dose == null || dose.trim().isEmpty()) {
            dose = "Follow the prescription";
        }

        if (instructions == null) {
            instructions = "";
        }

        createNotificationChannel(context);

        Notification notification = new NotificationCompat.Builder(
                context, CHANNEL_ID
        )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Elder Care — Medicine Reminder")
                .setContentText("Reminder for " + medicineName)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Medicine: " + medicineName
                                + "\nDose: " + dose
                                + "\nInstructions: " + instructions
                                + "\n\nPlease follow the clinician's prescribed schedule."
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        long medicineId = intent.getLongExtra(
                "medicineId",
                System.currentTimeMillis()
        );

        int notificationId = (int) (medicineId & 0x7fffffff);

        try {
            NotificationManagerCompat.from(context)
                    .notify(notificationId, notification);
        } catch (SecurityException ignored) {
            // Android notification permission may be disabled.
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Elder Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "Scheduled notifications for elder medicines"
            );

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

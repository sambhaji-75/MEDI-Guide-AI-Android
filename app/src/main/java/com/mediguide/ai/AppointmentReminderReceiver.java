
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

public class AppointmentReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "mother_appointment_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String doctor = intent.getStringExtra("doctorName");
        String clinic = intent.getStringExtra("clinic");
        String appointmentId = intent.getStringExtra("appointmentId");

        if (doctor == null || doctor.trim().isEmpty()) {
            doctor = "your doctor";
        }

        if (clinic == null || clinic.trim().isEmpty()) {
            clinic = "your clinic";
        }

        createChannel(context);

        Notification notification = new NotificationCompat.Builder(
                context, CHANNEL_ID
        )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Mother Care: Appointment Reminder")
                .setContentText("Your appointment with " + doctor + " at " + clinic)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Reminder: Your doctor appointment is coming up. "
                                + "Doctor: " + doctor
                                + ". Hospital/Clinic: " + clinic
                                + ". Please prepare for your visit."
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        int notificationId = appointmentId == null
                ? (int) System.currentTimeMillis()
                : appointmentId.hashCode();

        NotificationManagerCompat manager =
                NotificationManagerCompat.from(context);

        try {
            manager.notify(notificationId, notification);
        } catch (SecurityException e) {
            // Notification permission is disabled on the device.
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mother Care Appointment Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "Notifications for upcoming doctor appointments"
            );

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

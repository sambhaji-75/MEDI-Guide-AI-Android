
package com.mediguide.ai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BabyVaccinationReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "baby_vaccination_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String vaccineName = intent.getStringExtra("vaccinationName");
        long vaccinationId = intent.getLongExtra("vaccinationId", 1L);

        if (vaccineName == null || vaccineName.trim().isEmpty()) {
            vaccineName = "Scheduled vaccination";
        }

        createNotificationChannel(context);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Baby Care — Vaccination Reminder")
                        .setContentText("Upcoming vaccination: " + vaccineName)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Vaccination: " + vaccineName
                                        + "\nPlease check the baby's vaccination record "
                                        + "and follow the date advised by your pediatrician."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        int notificationId = (int) (vaccinationId ^ (vaccinationId >>> 32));

        try {
            NotificationManagerCompat.from(context)
                    .notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Notifications may be blocked by Android settings.
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Baby Vaccination Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(
                    "Notifications for scheduled baby vaccination reminders");

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}

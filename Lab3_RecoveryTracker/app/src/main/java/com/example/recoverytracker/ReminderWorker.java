package com.example.recoverytracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String type = getInputData().getString("type");
        sendNotification(type);
        return Result.success();
    }

    private void sendNotification(String type) {
        // Создаём канал для Android 8+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "recovery_channel",
                    "Recovery Tracker",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        String title, text;
        if ("medication".equals(type)) {
            title = "💊 Напоминание о лекарствах";
            text = "Не забудьте принять лекарства!";
        } else {
            title = "📝 Напоминание о записи";
            text = "Как вы себя чувствуете? Добавьте запись о самочувствии.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "recovery_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        manager.notify(1, builder.build());
    }
}
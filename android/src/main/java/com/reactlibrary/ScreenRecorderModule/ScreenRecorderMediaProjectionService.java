package com.reactlibrary.ScreenRecorderModule;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ScreenRecorderMediaProjectionService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel("screen_recorder_service", "Screen recorder service");

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
            Notification notification = builder.setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(101, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) {
            service.createNotificationChannel(channel);
        }
        return channelId;
    }

}
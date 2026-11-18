package com.pystarter.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

/**
 * App 启动时做三件事：
 * 1. 启动 App 里内嵌的 Python 解释器（Chaquopy），之后各页面都能调用 Python 代码；
 * 2. 建立通知渠道（Android 8.0 以上发通知必须先建渠道）；
 * 3. 安排每天的学习提醒。
 */
public class PyStarterApp extends Application {

    public static final String CHANNEL_ID = "study_reminder";

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        createNotificationChannel();
        ReminderWorker.schedule(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.channel_desc));
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}

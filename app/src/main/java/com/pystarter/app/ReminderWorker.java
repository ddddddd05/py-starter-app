package com.pystarter.app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pystarter.app.data.Store;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * 每日学习提醒（WorkManager）。
 *
 * WorkManager 是安卓官方的后台任务组件：App 关掉、手机重启后，任务依然会按计划执行。
 * 每天晚上 20 点左右检查一次：今天还没学过，才发通知，学过就不打扰。
 */
public class ReminderWorker extends Worker {

    private static final String WORK_NAME = "daily_study_reminder";
    private static final int REMIND_HOUR = 20;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** 安排每天一次的任务；首次执行时间对齐到下一个 20 点。KEEP 表示已安排过就不重复安排。 */
    public static void schedule(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SettingsActivity.KEY_REMINDER, true)) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            return;
        }
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, REMIND_HOUR);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        long delay = next.getTimeInMillis() - now.getTimeInMillis();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        if (Store.studiedToday(ctx)) {
            return Result.success();
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
                && android.os.Build.VERSION.SDK_INT >= 33) {
            return Result.success();           // 用户没给通知权限，就不发
        }
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, PyStarterApp.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(ctx.getString(R.string.remind_title))
                .setContentText(ctx.getString(R.string.remind_text))
                .setContentIntent(pi)
                .setAutoCancel(true);
        NotificationManagerCompat.from(ctx).notify(1, builder.build());
        return Result.success();
    }
}

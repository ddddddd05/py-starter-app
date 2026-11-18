package com.pystarter.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** 本地数据库（Room，底层是手机自带的 SQLite）。做题记录只存在手机上，不上传。 */
@Database(entities = {QuestionRecord.class, StudyDay.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract RecordDao dao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "pystarter.db").build();
                }
            }
        }
        return instance;
    }
}

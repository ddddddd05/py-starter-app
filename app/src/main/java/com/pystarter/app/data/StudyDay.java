package com.pystarter.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** 学习打卡：哪一天学过（日期格式 yyyy-MM-dd）。用来算连续学习天数、判断今天要不要提醒。 */
@Entity(tableName = "study_day")
public class StudyDay {
    @PrimaryKey
    @NonNull
    public String date = "";
    public int submissions;
}

package com.pystarter.app.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * 每道题的做题记录（Room 数据库里的一张表）。
 * 主键是“第几关 + 第几题”，同一道题反复做只更新这一行。
 */
@Entity(tableName = "question_record", primaryKeys = {"lessonId", "questionIndex"})
public class QuestionRecord {
    public int lessonId;
    public int questionIndex;
    public boolean correct;        // 最后一次是否答对
    public boolean everWrong;      // 是否答错过（进错题本）
    public int attempts;           // 提交次数
    public int hintsUsed;          // 用了几次提示
    @NonNull
    public String lastAnswer = "";
    public long updatedAt;
}

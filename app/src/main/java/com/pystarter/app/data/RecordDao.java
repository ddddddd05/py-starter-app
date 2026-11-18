package com.pystarter.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** 数据库的读写操作（DAO：Data Access Object）。Room 会在编译时根据这些注解生成真正的 SQL 代码。 */
@Dao
public interface RecordDao {

    @Query("SELECT * FROM question_record WHERE lessonId = :lessonId AND questionIndex = :index")
    QuestionRecord find(int lessonId, int index);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(QuestionRecord record);

    @Query("SELECT COUNT(*) FROM question_record WHERE lessonId = :lessonId AND correct = 1")
    int correctCount(int lessonId);

    @Query("SELECT COUNT(*) FROM question_record WHERE correct = 1")
    int totalCorrect();

    /** 错题本：答错过、而且现在还没答对的题，最近的排前面 */
    @Query("SELECT * FROM question_record WHERE everWrong = 1 AND correct = 0 ORDER BY updatedAt DESC")
    List<QuestionRecord> wrongQuestions();

    @Query("SELECT * FROM study_day WHERE date = :date")
    StudyDay findDay(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveDay(StudyDay day);

    @Query("SELECT date FROM study_day ORDER BY date DESC")
    List<String> studyDates();
}

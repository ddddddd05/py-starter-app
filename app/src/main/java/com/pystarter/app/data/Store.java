package com.pystarter.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 数据库操作的统一入口：Room 不允许在主线程读写数据库（会卡界面），
 * 所以这里所有操作都放到后台线程，结果再回到主线程。
 */
public final class Store {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Store() {}

    private static <T> void async(Supplier<T> work, Consumer<T> done) {
        IO.execute(() -> {
            T value = work.get();
            if (done != null) {
                MAIN.post(() -> done.accept(value));
            }
        });
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    /** 记录一次提交：更新这道题的记录，并给今天打卡。 */
    public static void recordSubmit(Context ctx, int lessonId, int index, String answer, boolean correct,
                                    Runnable done) {
        async(() -> {
            RecordDao dao = AppDatabase.get(ctx).dao();
            QuestionRecord r = dao.find(lessonId, index);
            if (r == null) {
                r = new QuestionRecord();
                r.lessonId = lessonId;
                r.questionIndex = index;
            }
            r.attempts += 1;
            r.correct = correct;
            if (!correct) {
                r.everWrong = true;
            }
            r.lastAnswer = answer == null ? "" : answer;
            r.updatedAt = System.currentTimeMillis();
            dao.save(r);

            StudyDay day = dao.findDay(today());
            if (day == null) {
                day = new StudyDay();
                day.date = today();
            }
            day.submissions += 1;
            dao.saveDay(day);
            return null;
        }, v -> {
            if (done != null) {
                done.run();
            }
        });
    }

    public static void recordHint(Context ctx, int lessonId, int index) {
        async(() -> {
            RecordDao dao = AppDatabase.get(ctx).dao();
            QuestionRecord r = dao.find(lessonId, index);
            if (r == null) {
                r = new QuestionRecord();
                r.lessonId = lessonId;
                r.questionIndex = index;
            }
            r.hintsUsed += 1;
            r.updatedAt = System.currentTimeMillis();
            dao.save(r);
            return null;
        }, null);
    }

    public static void correctCount(Context ctx, int lessonId, Consumer<Integer> done) {
        async(() -> AppDatabase.get(ctx).dao().correctCount(lessonId), done);
    }

    public static void lastAnswer(Context ctx, int lessonId, int index, Consumer<QuestionRecord> done) {
        async(() -> AppDatabase.get(ctx).dao().find(lessonId, index), done);
    }

    public static void wrongQuestions(Context ctx, Consumer<List<QuestionRecord>> done) {
        async(() -> AppDatabase.get(ctx).dao().wrongQuestions(), done);
    }

    public static void totalCorrect(Context ctx, Consumer<Integer> done) {
        async(() -> AppDatabase.get(ctx).dao().totalCorrect(), done);
    }

    /** 连续学习天数：从今天（或昨天）往前数，连续有打卡记录的天数。 */
    public static void streak(Context ctx, Consumer<Integer> done) {
        async(() -> countStreak(AppDatabase.get(ctx).dao().studyDates(), today()), done);
    }

    public static boolean studiedToday(Context ctx) {
        return AppDatabase.get(ctx).dao().findDay(today()) != null;     // 只在后台线程（提醒任务）里调用
    }

    /** 纯计算，方便单独测试。dates 按日期倒序。今天还没学时，从昨天开始算，不算断签。 */
    static int countStreak(List<String> dates, String todayStr) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(f.parse(todayStr));
        } catch (Exception e) {
            return 0;
        }
        if (!dates.contains(todayStr)) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        int count = 0;
        while (dates.contains(f.format(cal.getTime()))) {
            count++;
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        return count;
    }
}

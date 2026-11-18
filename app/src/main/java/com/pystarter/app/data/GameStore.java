package com.pystarter.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 闯关进度存档：学完了哪些关、每关最好几颗星、经验值、徽章、复仇怪。
 *
 * 数据量很小，用 SharedPreferences 存，不用改 Room 数据库结构（改结构要写迁移，容易丢用户的做题记录）。
 * 做题明细、错题本、打卡天数仍然在 Room 里（Store 类），这里只放游戏层的东西。
 */
public final class GameStore {

    public static final int LESSON_COUNT = 12;
    public static final int XP_PER_LEVEL = 100;

    private static final String PREFS = "game";
    private static final String KEY_LEARNED = "learned";
    private static final String KEY_STARS = "stars";          // JSON：{"1": 3, "2": 2}
    private static final String KEY_XP = "xp";
    private static final String KEY_BADGES = "badges";
    private static final String KEY_NO_HINT_RUN = "no_hint_run";
    private static final String KEY_REVENGE = "revenge";      // JSON：{"3_2": {"tag": "colon", "done": false}}

    /** 徽章：id、名字、说明。顺序就是在徽章页的显示顺序。 */
    public static final String[][] BADGES = {
            {"first_clear", "初次通关", "通过任意一关的挑战"},
            {"first_three", "首个三星", "在一关里拿到 3 颗星"},
            {"chapter_1", "新手村毕业", "通过第 4 关"},
            {"chapter_2", "森林毕业", "通过第 8 关"},
            {"all_clear", "通关全部关卡", "通过全部 12 关"},
            {"all_three", "全部三星", "12 关全部拿到 3 颗星"},
            {"no_hint_3", "独立思考", "连续 3 关不看提示通过"},
            {"streak_7", "七日之约", "连续打卡 7 天"},
    };

    private GameStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- 学习 / 解锁 ----------

    public static boolean isLearned(Context ctx, int lessonId) {
        return prefs(ctx).getStringSet(KEY_LEARNED, new HashSet<>()).contains(String.valueOf(lessonId));
    }

    public static void markLearned(Context ctx, int lessonId) {
        Set<String> set = new HashSet<>(prefs(ctx).getStringSet(KEY_LEARNED, new HashSet<>()));
        set.add(String.valueOf(lessonId));
        prefs(ctx).edit().putStringSet(KEY_LEARNED, set).apply();
    }

    /** 学习解锁：第 1 关一开始就开放，之后要先通过上一关的挑战。 */
    public static boolean isLearnUnlocked(Context ctx, int lessonId) {
        return lessonId == 1 || stars(ctx, lessonId - 1) > 0;
    }

    /** 挑战解锁：这一关学完才能挑战。 */
    public static boolean isChallengeUnlocked(Context ctx, int lessonId) {
        return isLearnUnlocked(ctx, lessonId) && isLearned(ctx, lessonId);
    }

    /** 当前该打哪一关：第一个还没通过的关。全部通过返回 LESSON_COUNT。 */
    public static int currentLesson(Context ctx) {
        for (int i = 1; i <= LESSON_COUNT; i++) {
            if (stars(ctx, i) == 0) {
                return i;
            }
        }
        return LESSON_COUNT;
    }

    // ---------- 星级 ----------

    private static JSONObject starsJson(Context ctx) {
        try {
            return new JSONObject(prefs(ctx).getString(KEY_STARS, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static int stars(Context ctx, int lessonId) {
        return starsJson(ctx).optInt(String.valueOf(lessonId), 0);
    }

    public static int totalStars(Context ctx) {
        int total = 0;
        for (int i = 1; i <= LESSON_COUNT; i++) {
            total += stars(ctx, i);
        }
        return total;
    }

    /**
     * 根据这次挑战的表现算星级（纯计算，方便测试）：
     * 没答错、没看提示 3 星；最多错 1 次 2 星；其他情况 1 星。翻过笔记最多 2 星。
     */
    public static int computeStars(int mistakes, int hintsUsed, boolean usedNotes) {
        int s;
        if (mistakes == 0 && hintsUsed == 0) {
            s = 3;
        } else if (mistakes <= 1) {
            s = 2;
        } else {
            s = 1;
        }
        return usedNotes ? Math.min(s, 2) : s;
    }

    // ---------- 经验 / 等级 ----------

    public static int xp(Context ctx) {
        return prefs(ctx).getInt(KEY_XP, 0);
    }

    public static int levelOf(int xp) {
        return xp / XP_PER_LEVEL + 1;
    }

    /** 等级称号：每升一级换一个，最高“Python 大师”。 */
    public static final String[] LEVEL_TITLES = {
            "见习码农", "初级码农", "代码学徒", "循环骑士", "函数法师", "数据猎人", "代码勇者", "Python 大师"};

    public static String titleOf(int level) {
        return LEVEL_TITLES[Math.max(1, Math.min(level, LEVEL_TITLES.length)) - 1];
    }

    public static boolean allCleared(Context ctx) {
        for (int i = 1; i <= LESSON_COUNT; i++) {
            if (stars(ctx, i) == 0) {
                return false;
            }
        }
        return true;
    }

    // ---------- 一次挑战结束 ----------

    /** 挑战结算的结果，交给结算页显示。 */
    public static final class Clear {
        public int stars;
        public int oldStars;
        public int battleXp;
        public int clearXp;          // 首次通关奖励
        public int starXp;           // 星级提升奖励（只奖励比以前多出来的星）
        public int oldLevel;
        public int newLevel;
        public final List<String> newBadges = new ArrayList<>();

        public int totalXp() {
            return battleXp + clearXp + starXp;
        }
    }

    /**
     * 挑战成功时调用：保存星级、加经验、检查徽章。
     * 重复挑战时，过关奖励只给第一次，星级奖励只给新增的星，避免反复刷同一关。
     */
    public static Clear recordClear(Context ctx, int lessonId, int stars, int battlesWon, int hintsUsed,
                                    int streakDays) {
        Clear c = new Clear();
        c.stars = stars;
        c.oldStars = stars(ctx, lessonId);
        c.battleXp = battlesWon * 10;
        c.clearXp = c.oldStars == 0 ? 20 : 0;
        c.starXp = Math.max(0, stars - c.oldStars) * 10;

        int oldXp = xp(ctx);
        c.oldLevel = levelOf(oldXp);
        int newXp = oldXp + c.totalXp();
        c.newLevel = levelOf(newXp);

        JSONObject s = starsJson(ctx);
        try {
            s.put(String.valueOf(lessonId), Math.max(stars, c.oldStars));
        } catch (JSONException ignored) {
        }
        int noHintRun = hintsUsed == 0 ? prefs(ctx).getInt(KEY_NO_HINT_RUN, 0) + 1 : 0;
        prefs(ctx).edit()
                .putString(KEY_STARS, s.toString())
                .putInt(KEY_XP, newXp)
                .putInt(KEY_NO_HINT_RUN, noHintRun)
                .apply();

        c.newBadges.addAll(checkBadges(ctx, streakDays));
        return c;
    }

    /** 复仇怪打赢的额外经验。 */
    public static void addXp(Context ctx, int amount) {
        prefs(ctx).edit().putInt(KEY_XP, xp(ctx) + amount).apply();
    }

    // ---------- 徽章 ----------

    public static Set<String> badges(Context ctx) {
        return prefs(ctx).getStringSet(KEY_BADGES, new HashSet<>());
    }

    /** 检查有没有新达成的徽章，有就保存，并返回新徽章的名字。 */
    public static List<String> checkBadges(Context ctx, int streakDays) {
        Set<String> owned = new HashSet<>(badges(ctx));
        List<String> fresh = new ArrayList<>();
        int cleared = 0;
        int three = 0;
        for (int i = 1; i <= LESSON_COUNT; i++) {
            int st = stars(ctx, i);
            if (st > 0) {
                cleared++;
            }
            if (st == 3) {
                three++;
            }
        }
        boolean[] reached = {
                cleared >= 1,
                three >= 1,
                stars(ctx, 4) > 0,
                stars(ctx, 8) > 0,
                cleared == LESSON_COUNT,
                three == LESSON_COUNT,
                prefs(ctx).getInt(KEY_NO_HINT_RUN, 0) >= 3,
                streakDays >= 7,
        };
        for (int i = 0; i < BADGES.length; i++) {
            if (reached[i] && owned.add(BADGES[i][0])) {
                fresh.add(BADGES[i][1]);
            }
        }
        prefs(ctx).edit().putStringSet(KEY_BADGES, owned).apply();
        return fresh;
    }

    // ---------- 复仇怪：答错过的题，在后面的关卡里带着错因标签重新出现 ----------

    private static JSONObject revengeJson(Context ctx) {
        try {
            return new JSONObject(prefs(ctx).getString(KEY_REVENGE, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /** 挑战里答错时记下来；已经征服过的同一道题再错，会重新变回“待复仇”。 */
    public static void addRevenge(Context ctx, int lessonId, int index, String tag, String tagName) {
        JSONObject all = revengeJson(ctx);
        try {
            JSONObject r = new JSONObject();
            r.put("lesson", lessonId);
            r.put("index", index);
            r.put("tag", tag == null ? "other" : tag);
            r.put("name", tagName == null ? "迷糊怪" : tagName);
            r.put("done", false);
            all.put(lessonId + "_" + index, r);
        } catch (JSONException ignored) {
        }
        prefs(ctx).edit().putString(KEY_REVENGE, all.toString()).apply();
    }

    /** 找一只来自更早关卡、还没被征服的复仇怪；没有返回 null。 */
    public static JSONObject pickRevenge(Context ctx, int currentLesson) {
        JSONObject all = revengeJson(ctx);
        JSONObject pick = null;
        for (java.util.Iterator<String> it = all.keys(); it.hasNext(); ) {
            JSONObject r = all.optJSONObject(it.next());
            if (r != null && !r.optBoolean("done") && r.optInt("lesson") < currentLesson) {
                if (pick == null || r.optInt("lesson") < pick.optInt("lesson")) {
                    pick = r;                       // 优先最早的错题
                }
            }
        }
        return pick;
    }

    public static void conquerRevenge(Context ctx, int lessonId, int index) {
        JSONObject all = revengeJson(ctx);
        JSONObject r = all.optJSONObject(lessonId + "_" + index);
        if (r != null) {
            try {
                r.put("done", true);
            } catch (JSONException ignored) {
            }
            prefs(ctx).edit().putString(KEY_REVENGE, all.toString()).apply();
        }
    }

    /** 复仇怪图鉴：[已征服数, 总数]。 */
    public static int[] revengeCount(Context ctx) {
        JSONObject all = revengeJson(ctx);
        int done = 0;
        for (java.util.Iterator<String> it = all.keys(); it.hasNext(); ) {
            JSONObject r = all.optJSONObject(it.next());
            if (r != null && r.optBoolean("done")) {
                done++;
            }
        }
        return new int[]{done, all.length()};
    }
}

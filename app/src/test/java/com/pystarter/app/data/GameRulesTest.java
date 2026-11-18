package com.pystarter.app.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 闯关规则的单元测试：星级、结算防刷、解锁、徽章、等级。都是纯计算，在电脑上跑，不需要手机。 */
public class GameRulesTest {

    // ---------- 星级 ----------

    @Test
    public void perfectRunGetsThreeStars() {
        assertEquals(3, GameStore.computeStars(0, 0, false));
    }

    @Test
    public void oneMistakeOrAnyHintGetsTwoStars() {
        assertEquals(2, GameStore.computeStars(1, 0, false));
        assertEquals(2, GameStore.computeStars(0, 1, false));
        assertEquals(2, GameStore.computeStars(1, 3, false));
    }

    @Test
    public void twoOrMoreMistakesGetOneStar() {
        assertEquals(1, GameStore.computeStars(2, 0, false));
        assertEquals(1, GameStore.computeStars(5, 2, false));
    }

    @Test
    public void notesCapStarsAtTwoButNeverRaiseThem() {
        assertEquals(2, GameStore.computeStars(0, 0, true));
        assertEquals(1, GameStore.computeStars(3, 0, true));
    }

    // ---------- 结算：不能靠重复刷同一关拿奖励 ----------

    @Test
    public void firstClearGetsClearBonusAndAllStarXp() {
        GameStore.Clear c = GameStore.computeClear(0, 2, 3, 0);
        assertEquals(30, c.battleXp);
        assertEquals(20, c.clearXp);
        assertEquals(20, c.starXp);
        assertEquals(70, c.totalXp());
    }

    @Test
    public void replayOnlyRewardsNewStars() {
        GameStore.Clear c = GameStore.computeClear(2, 3, 3, 100);
        assertEquals(0, c.clearXp);
        assertEquals(10, c.starXp);
    }

    @Test
    public void replayWithSameOrFewerStarsGetsOnlyBattleXp() {
        assertEquals(30, GameStore.computeClear(3, 1, 3, 0).totalXp());
        assertEquals(30, GameStore.computeClear(2, 2, 3, 0).totalXp());
    }

    @Test
    public void levelUpIsDetectedAcrossHundredXp() {
        GameStore.Clear c = GameStore.computeClear(0, 3, 3, 95);   // 95 + 30 + 20 + 30 = 175
        assertEquals(1, c.oldLevel);
        assertEquals(2, c.newLevel);
    }

    // ---------- 解锁 ----------

    @Test
    public void firstLessonIsAlwaysOpen() {
        assertTrue(GameStore.learnUnlocked(1, 0));
    }

    @Test
    public void laterLessonsNeedPreviousCleared() {
        assertFalse(GameStore.learnUnlocked(2, 0));
        assertTrue(GameStore.learnUnlocked(2, 1));
    }

    // ---------- 等级 ----------

    @Test
    public void levelAndTitleFromXp() {
        assertEquals(1, GameStore.levelOf(0));
        assertEquals(1, GameStore.levelOf(99));
        assertEquals(2, GameStore.levelOf(100));
        assertEquals("见习码农", GameStore.titleOf(1));
        assertEquals("Python 大师", GameStore.titleOf(99));     // 超过最高级时停在最后一个称号
    }

    // ---------- 徽章 ----------

    @Test
    public void noProgressNoBadges() {
        boolean[] r = GameStore.reachedBadges(new int[12], 0, 0);
        assertArrayEquals(new boolean[8], r);
    }

    @Test
    public void chapterBadgesNeedTheBossLesson() {
        int[] stars = {1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};
        boolean[] r = GameStore.reachedBadges(stars, 0, 0);
        assertTrue(r[0]);       // 初次通关
        assertFalse(r[1]);      // 首个三星
        assertTrue(r[2]);       // 新手村毕业（第 4 关）
        assertFalse(r[3]);      // 森林毕业（第 8 关）
    }

    @Test
    public void allClearAndAllThreeStars() {
        int[] stars = new int[12];
        java.util.Arrays.fill(stars, 3);
        boolean[] r = GameStore.reachedBadges(stars, 3, 7);
        for (boolean b : r) {
            assertTrue(b);
        }
        stars[11] = 2;
        assertFalse(GameStore.reachedBadges(stars, 3, 7)[5]);   // 有一关不是三星
    }

    @Test
    public void badgeRuleListMatchesBadgeDefinitions() {
        assertEquals(GameStore.BADGES.length, GameStore.reachedBadges(new int[12], 0, 0).length);
    }
}

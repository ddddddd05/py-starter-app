package com.pystarter.app.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** 连续打卡天数的计算。 */
public class StreakTest {

    @Test
    public void noStudyIsZero() {
        assertEquals(0, Store.countStreak(Collections.emptyList(), "2026-11-18"));
    }

    @Test
    public void consecutiveDaysIncludingToday() {
        assertEquals(3, Store.countStreak(Arrays.asList("2026-11-18", "2026-11-17", "2026-11-16"), "2026-11-18"));
    }

    @Test
    public void notStudiedYetTodayDoesNotBreakStreak() {
        // 今天还没学：从昨天开始数，不算断签
        assertEquals(2, Store.countStreak(Arrays.asList("2026-11-17", "2026-11-16"), "2026-11-18"));
    }

    @Test
    public void gapBreaksStreak() {
        assertEquals(1, Store.countStreak(Arrays.asList("2026-11-18", "2026-11-16"), "2026-11-18"));
    }

    @Test
    public void crossesMonthBoundary() {
        assertEquals(3, Store.countStreak(Arrays.asList("2026-12-01", "2026-11-30", "2026-11-29"), "2026-12-01"));
    }
}

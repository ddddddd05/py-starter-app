package com.pystarter.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 主流程的 UI 测试：在模拟器上真的打开 App、点按钮，检查界面。
 * 地图 → 第 1 关学习 → 小练习 → 进入挑战 → 答错扣心 → 答对打倒小怪。
 *
 * 注意：每个测试开始前会清空闯关进度，保证结果可重复。
 * 判题在 Python 后台线程里跑，Espresso 不知道什么时候结束，所以用 waitFor 轮询等界面出现。
 */
@RunWith(AndroidJUnit4.class)
public class MainFlowTest {

    @Rule
    public GrantPermissionRule notifications = Build.VERSION.SDK_INT >= 33
            ? GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
            : GrantPermissionRule.grant();

    @Before
    public void resetProgress() {
        Context ctx = ApplicationProvider.getApplicationContext();
        ctx.getSharedPreferences("game", Context.MODE_PRIVATE).edit().clear().commit();
    }

    /** 等某个控件出现（Python 初始化和判题都需要时间），最多等 timeoutMs。 */
    private static void waitFor(Matcher<View> matcher, long timeoutMs) {
        long end = SystemClock.uptimeMillis() + timeoutMs;
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionError e) {
                if (SystemClock.uptimeMillis() > end) {
                    throw e;
                }
                SystemClock.sleep(200);
            }
        }
    }

    @Test
    public void mapShowsFirstChapterAndLocksLaterLessons() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            waitFor(withText("第 1 章 · 新手村"), 20_000);
            onView(withId(R.id.levelTitle)).check(matches(withText("Lv.1 见习码农")));
            onView(withText("点我开始学习")).check(matches(isDisplayed()));     // 只有第 1 关可以学
        }
    }

    @Test
    public void learnThenChallengeThenLoseAndWinABattle() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            waitFor(withText("点我开始学习"), 20_000);
            onView(withText("点我开始学习")).perform(click());

            // 学习页：一路点“下一步”直到小练习
            waitFor(withText("小法师"), 10_000);
            for (int i = 0; i < 10; i++) {
                try {
                    onView(withText("下一步 >")).check(matches(isDisplayed())).perform(click());
                } catch (NoMatchingViewException | AssertionError e) {
                    break;                                   // 到最后一步了，按钮变成“我学会了”
                }
            }
            onView(allOf(withText("print(\"你好\")"), isClickable())).perform(scrollTo(), click());
            waitFor(withText(containsString("答对了")), 10_000);
            onView(withText("我学会了，去挑战！")).perform(click());

            // 挑战第 1 战：先答错，扣 1 颗心
            waitFor(withText("第 1 关 · 第 1 战 · 小怪"), 10_000);
            onView(withId(R.id.hearts)).check(matches(withText("♥♥♥")));
            onView(allOf(withText("print(\"我爱学习\")"), isClickable())).perform(scrollTo(), click());   // 题目代码块里也有这句话，只点选项按钮
            waitFor(withText("♥♥♡"), 10_000);
            onView(withId(R.id.speaker)).check(matches(withText(containsString("偏差怪"))));

            // 再答对：打倒小怪，出现“继续前进”
            onView(allOf(withText("我爱学习"), isClickable())).perform(scrollTo(), click());
            waitFor(withText("继续前进 >"), 10_000);
            onView(withId(R.id.hearts)).check(matches(withText("♥♥♡")));
        }
    }
}

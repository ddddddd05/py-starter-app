package com.pystarter.app.game;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** 小动画：受击抖动、闪烁、上下浮动、飘字。都是一两行就能调用的效果。 */
public final class Fx {

    private Fx() {}

    /** 左右抖动（答错时卡片抖、受伤时人物抖）。 */
    public static void shake(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, -14, 14, -10, 10, -6, 6, 0);
        a.setDuration(380);
        a.start();
    }

    /** 被打中：闪几下 + 抖动。 */
    public static void hit(View v) {
        ObjectAnimator blink = ObjectAnimator.ofFloat(v, View.ALPHA, 1f, 0.2f, 1f, 0.2f, 1f);
        blink.setDuration(420);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(blink, ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, 18, -12, 8, 0).setDuration(420));
        set.start();
    }

    /** 冲上去攻击再退回来。dx 为正往右冲。 */
    public static void lunge(View v, float dx) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0, dx, 0);
        a.setDuration(320);
        a.start();
    }

    /** 待机时轻轻上下浮动（像素游戏里角色的“呼吸”）。 */
    public static void idle(View v) {
        if ("idle".equals(v.getTag())) {
            return;                                    // 已经在浮动了，不重复叠加动画
        }
        v.setTag("idle");
        ObjectAnimator a = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0, -6, 0);
        a.setDuration(900);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.start();
    }

    /** 被打倒：缩小淡出。 */
    public static void defeat(View v) {
        v.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).setDuration(450).start();
    }

    public static void reset(View v) {
        v.animate().cancel();
        v.setAlpha(1f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setTranslationX(0);
    }

    /** 弹出：从小变大（星星、徽章出现时）。 */
    public static void pop(View v, long delay) {
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.animate().scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(260).start();
    }

    /** 在 parent 里 anchor 上方飘出一行字（例如“+10 XP”），然后消失。parent 必须是 FrameLayout。 */
    public static void floatText(FrameLayout parent, View anchor, String text, int color) {
        TextView t = new TextView(parent.getContext());
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(20);
        t.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        t.setShadowLayer(1, 3, 3, 0xFF000000);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parent.addView(t, lp);
        t.setX(anchor.getX() + anchor.getWidth() / 2f - 40);
        t.setY(anchor.getY());
        t.animate().translationYBy(-120).alpha(0f).setDuration(1000)
                .withEndAction(() -> parent.removeView(t)).start();
    }
}

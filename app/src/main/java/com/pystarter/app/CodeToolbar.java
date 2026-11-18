package com.pystarter.app;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pystarter.app.game.SoundFx;

import java.util.WeakHashMap;

/**
 * 手机端代码快捷符号栏：解决手机输入法切英文、打缩进/冒号/引号/括号麻烦的问题。
 */
public final class CodeToolbar {

    private static final WeakHashMap<EditText, View> TOOLBARS = new WeakHashMap<>();

    private static final String[][] KEYS = {
            {"缩进", "    ", "4"},
            {"()", "()", "1"},
            {"\"\"", "\"\"", "1"},
            {":", ":", "1"},
            {"=", "=", "1"},
            {"for", "for i in range():", "15"},
            {"if", "if :", "3"},
            {"print", "print()", "6"},
    };

    private CodeToolbar() {}

    /** 在 codeInput 上方自动插入一排快捷符号按钮。 */
    public static void attach(EditText codeInput) {
        if (codeInput == null || !(codeInput.getParent() instanceof ViewGroup)) {
            return;
        }
        ViewGroup parent = (ViewGroup) codeInput.getParent();
        int idx = parent.indexOfChild(codeInput);
        if (idx < 0) {
            return;
        }
        Context ctx = codeInput.getContext();
        HorizontalScrollView scroll = new HorizontalScrollView(ctx);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.bottomMargin = dp(ctx, 4);
        scroll.setLayoutParams(sp);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (String[] item : KEYS) {
            TextView btn = new TextView(ctx);
            btn.setText(item[0]);
            btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setTextColor(0xFFE2E8F0);
            btn.setBackgroundResource(R.drawable.px_btn_gray);
            btn.setGravity(Gravity.CENTER);
            int padH = dp(ctx, 10);
            int padV = dp(ctx, 5);
            btn.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(ctx, 5);
            btn.setLayoutParams(lp);
            String insert = item[1];
            int cursorOffset = Integer.parseInt(item[2]);
            btn.setOnClickListener(v -> {
                SoundFx.click(ctx);
                insertAtCursor(codeInput, insert, cursorOffset);
            });
            row.addView(btn);
        }
        scroll.addView(row);
        parent.addView(scroll, idx);
        TOOLBARS.put(codeInput, scroll);
    }

    /** 控制快捷符号栏随代码编辑框一起显示或隐藏（避免在选择题/填空题里露出）。 */
    public static void setVisible(EditText codeInput, boolean visible) {
        View bar = TOOLBARS.get(codeInput);
        if (bar != null) {
            bar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private static void insertAtCursor(EditText et, String text, int cursorOffset) {
        int start = Math.max(et.getSelectionStart(), 0);
        int end = Math.max(et.getSelectionEnd(), 0);
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        et.getText().replace(start, end, text);
        int pos = Math.min(start + cursorOffset, et.getText().length());
        et.setSelection(pos);
        et.requestFocus();
    }

    private static int dp(Context ctx, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.getResources().getDisplayMetrics());
    }
}

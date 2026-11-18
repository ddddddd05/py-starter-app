package com.pystarter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.databinding.ActivityLessonBinding;
import com.pystarter.app.debug.StepDebugActivity;
import com.pystarter.app.game.Fx;
import com.pystarter.app.game.Sprites;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习页：先学后闯关。小法师一步一步讲，全部不扣血、不计时：
 *   概念卡片（一张只讲一个点）→ 比喻 → 动手试一试（示例代码可以改着玩）→ 小练习（答错只给解释）。
 * 学完点“我学会了，去挑战”，这一关的挑战就解锁了。
 */
public class LessonActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "lesson_id";

    private ActivityLessonBinding binding;
    private int lessonId;
    private JSONObject info;
    private final List<String[]> steps = new ArrayList<>();     // 每一步：{类型, 内容}
    private int step = 0;
    private boolean practiceAnswered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CodeToolbar.attach(binding.exampleEditor);
        lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, 1);

        binding.npc.setSprite(Sprites.wizard());
        Fx.idle(binding.npc);
        binding.back.setOnClickListener(v -> finish());
        binding.prev.setOnClickListener(v -> show(step - 1));
        binding.next.setOnClickListener(v -> onNext());
        binding.runExample.setOnClickListener(v -> runExample());
        binding.slowExample.setOnClickListener(v -> {
            String code = binding.exampleEditor.getText().toString();
            if (code.contains("input(") && binding.exampleInput.getText().toString().trim().isEmpty()) {
                Fx.shake(binding.inputBox);
                binding.speech.setText(R.string.example_input_needed);
                return;
            }
            StepDebugActivity.start(this, code, exampleInputs());
        });
        binding.resetExample.setOnClickListener(v -> {
            binding.exampleEditor.setText(info.optString("example"));
            binding.exampleOutput.setVisibility(View.GONE);
        });
        binding.exampleEditor.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                refreshInputBox();
            }
        });

        PyBridge.call(level -> PyBridge.call(lesson -> {
            if (level.has("error") || lesson.has("error")) {
                binding.speech.setText(level.optString("error", lesson.optString("error")));
                return;
            }
            info = level;
            binding.title.setText(getString(R.string.lesson_title, lessonId, lesson.optString("title")));
            buildSteps();
            show(0);
        }, "get_lesson", lessonId), "level_info", lessonId);
    }

    private void buildSteps() {
        JSONArray cards = info.optJSONArray("cards");
        for (int i = 0; cards != null && i < cards.length(); i++) {
            steps.add(new String[]{"card", cards.optString(i)});
        }
        steps.add(new String[]{"analogy", info.optString("analogy")});
        steps.add(new String[]{"example", getString(R.string.learn_try)});
        steps.add(new String[]{"practice", info.optJSONObject("practice").optString("prompt")});
        binding.stepBar.setMax(steps.size());
        binding.exampleEditor.setText(info.optString("example"));
    }

    private void show(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }
        step = index;
        String type = steps.get(index)[0];
        binding.stepText.setText(getString(R.string.step_of, index + 1, steps.size()));
        binding.stepBar.setProgress(index + 1);
        binding.speech.setText(steps.get(index)[1]);
        binding.speaker.setText(type.equals("analogy") ? R.string.npc_analogy : R.string.npc_name);
        binding.exampleBox.setVisibility(type.equals("example") ? View.VISIBLE : View.GONE);
        binding.practiceBox.setVisibility(type.equals("practice") ? View.VISIBLE : View.GONE);
        binding.prev.setVisibility(index == 0 ? View.INVISIBLE : View.VISIBLE);
        Fx.pop(binding.bubble, 0);

        if (type.equals("practice")) {
            buildPractice();
        }
        boolean last = index == steps.size() - 1;
        if (last) {
            binding.next.setText(R.string.learned_go);
            binding.next.setBackgroundResource(practiceAnswered ? R.drawable.px_btn_green : R.drawable.px_btn_gray);
        } else {
            binding.next.setText(R.string.next_step);
            binding.next.setBackgroundResource(R.drawable.px_btn_blue);
        }
    }

    private void onNext() {
        if (step < steps.size() - 1) {
            show(step + 1);
            return;
        }
        if (!practiceAnswered) {
            Fx.shake(binding.practiceBox);
            binding.speech.setText(R.string.practice_first);
            return;
        }
        GameStore.markLearned(this, lessonId);
        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra(BattleActivity.EXTRA_LESSON_ID, lessonId);
        startActivity(intent);
        finish();
    }

    private void runExample() {
        String code = binding.exampleEditor.getText().toString();
        if (code.contains("input(") && binding.exampleInput.getText().toString().trim().isEmpty()) {
            // 代码要输入内容，但输入框是空的：先提醒，而不是直接报“程序在等你输入”
            Fx.shake(binding.inputBox);
            binding.exampleInput.requestFocus();
            binding.speech.setText(R.string.example_input_needed);
            return;
        }
        binding.exampleOutput.setVisibility(View.VISIBLE);
        binding.exampleOutput.setText(R.string.running);
        PyBridge.call(r -> {
            JSONObject error = r.optJSONObject("error");
            String text = r.optString("output");
            if (error != null) {
                text = text + (text.isEmpty() ? "" : "\n") + "⚠ " + error.optString("message");
            }
            binding.exampleOutput.setText(text.isEmpty() ? getString(R.string.no_output) : text);
        }, "run_code", code, exampleInputs());
    }

    /** 输入框里每一行，对应程序里的一次 input()。 */
    private String exampleInputs() {
        JSONArray inputs = new JSONArray();
        for (String line : binding.exampleInput.getText().toString().split("\n", -1)) {
            if (!line.isEmpty()) {
                inputs.put(line);
            }
        }
        return inputs.toString();
    }

    /** 代码里出现 input() 时才显示输入框；用户把 input() 删掉或加上，输入框跟着出现或消失。 */
    private void refreshInputBox() {
        boolean needs = binding.exampleEditor.getText().toString().contains("input(");
        binding.inputBox.setVisibility(needs ? View.VISIBLE : View.GONE);
    }

    private void buildPractice() {
        JSONObject p = info.optJSONObject("practice");
        String code = p.optString("code");
        binding.practiceCode.setVisibility(code.isEmpty() ? View.GONE : View.VISIBLE);
        binding.practiceCode.setText(code);
        binding.practiceOptions.removeAllViews();
        JSONArray options = p.optJSONArray("options");
        for (int i = 0; options != null && i < options.length(); i++) {
            int choice = i;
            TextView b = new TextView(this, null, 0, R.style.PxButton);
            b.setText(options.optString(i));
            b.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            b.setOnClickListener(v -> answerPractice(choice, b));
            binding.practiceOptions.addView(b, lp);
        }
    }

    private void answerPractice(int choice, TextView button) {
        PyBridge.call(r -> {
            boolean correct = r.optBoolean("correct");
            practiceAnswered = true;                       // 答错也可以继续：小练习只是确认，不卡人
            button.setBackgroundResource(correct ? R.drawable.px_btn_green : R.drawable.px_btn_red);
            if (!correct) {
                Fx.shake(button);
                View right = binding.practiceOptions.getChildAt(r.optInt("answer"));
                if (right != null) {
                    right.setBackgroundResource(R.drawable.px_btn_green);
                }
            }
            binding.practiceExplain.setVisibility(View.VISIBLE);
            binding.practiceExplain.setText(getString(correct ? R.string.practice_right : R.string.practice_wrong,
                    r.optString("explain")));
            binding.next.setBackgroundResource(R.drawable.px_btn_green);
            binding.speech.setText(correct ? R.string.practice_npc_right : R.string.practice_npc_wrong);
        }, "check_practice", lessonId, choice);
    }
}

package com.pystarter.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityQuestionBinding;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 做题页。根据题型显示不同的输入方式：
 *   选择题 → 单选按钮；填空题 → 一个输入框；写代码 → 多行代码编辑框 + “运行”按钮。
 * 点“提交”交给 Python 判题（真的运行代码、比对输出）；点“问老师”拿提示，每多问一次提示更具体一级。
 */
public class QuestionActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "lesson_id";
    public static final String EXTRA_INDEX = "index";

    private ActivityQuestionBinding binding;
    private int lessonId;
    private int index;
    private int questionCount;
    private String type = "";
    private JSONObject question;
    private int hintLevel = 0;            // 已经给过几级提示

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, 1);
        index = getIntent().getIntExtra(EXTRA_INDEX, 0);

        PyBridge.call(this::showQuestion, "get_lesson", lessonId);
        binding.submit.setOnClickListener(v -> submit());
        binding.run.setOnClickListener(v -> runOnly());
        binding.askTeacher.setOnClickListener(v -> askTeacher());
        binding.next.setOnClickListener(v -> goNext());
    }

    private void showQuestion(JSONObject lesson) {
        if (lesson.has("error")) {
            binding.prompt.setText(lesson.optString("error"));
            return;
        }
        JSONArray questions = lesson.optJSONArray("questions");
        questionCount = questions == null ? 0 : questions.length();
        question = questions == null ? new JSONObject() : questions.optJSONObject(index);
        type = question.optString("type");
        setTitle(getString(R.string.question_title, lessonId, index + 1, questionCount));
        binding.prompt.setText(question.optString("prompt"));

        String code = question.optString("code");
        binding.codeBlock.setVisibility(code.isEmpty() ? View.GONE : View.VISIBLE);
        binding.codeBlock.setText(code);

        binding.choiceGroup.setVisibility(View.GONE);
        binding.fillBox.setVisibility(View.GONE);
        binding.codeEditor.setVisibility(View.GONE);
        binding.run.setVisibility(View.GONE);

        switch (type) {
            case "choice":
                binding.choiceGroup.setVisibility(View.VISIBLE);
                JSONArray options = question.optJSONArray("options");
                for (int i = 0; options != null && i < options.length(); i++) {
                    RadioButton rb = new RadioButton(this);
                    rb.setId(View.generateViewId());
                    rb.setTag(i);
                    rb.setText(options.optString(i));
                    binding.choiceGroup.addView(rb);
                }
                break;
            case "fill":
                binding.fillBox.setVisibility(View.VISIBLE);
                binding.fillTemplate.setText(question.optString("template"));
                break;
            default:
                binding.codeEditor.setVisibility(View.VISIBLE);
                binding.run.setVisibility(View.VISIBLE);
                binding.codeEditor.setText(question.optString("starter"));
                break;
        }
        // 上次写到一半的答案恢复回来（错题本点进来时尤其有用）
        Store.lastAnswer(this, lessonId, index, record -> {
            if (record == null || record.lastAnswer.isEmpty()) {
                return;
            }
            if ("code".equals(type)) {
                binding.codeEditor.setText(record.lastAnswer);
            } else if ("fill".equals(type)) {
                binding.fillInput.setText(record.lastAnswer);
            }
        });
    }

    /** 读取学生当前的回答；选择题返回选中的选项下标。 */
    private String currentAnswer() {
        switch (type) {
            case "choice":
                int checked = binding.choiceGroup.getCheckedRadioButtonId();
                if (checked == View.NO_ID) {
                    return "";
                }
                return String.valueOf(findViewById(checked).getTag());
            case "fill":
                return binding.fillInput.getText().toString();
            default:
                return binding.codeEditor.getText().toString();
        }
    }

    private void runOnly() {
        binding.output.setVisibility(View.VISIBLE);
        binding.output.setText(R.string.running);
        JSONArray inputs = question.optJSONArray("inputs");
        PyBridge.call(r -> {
            JSONObject error = r.optJSONObject("error");
            String text = r.optString("output");
            if (error != null) {
                text = text + (text.isEmpty() ? "" : "\n") + "⚠ " + error.optString("message");
            }
            binding.output.setText(text.isEmpty() ? getString(R.string.no_output) : text);
        }, "run_code", currentAnswer(), inputs == null ? "[]" : inputs.toString());
    }

    private void submit() {
        String answer = currentAnswer();
        if (answer.trim().isEmpty()) {
            showFeedback(false, getString(R.string.answer_first));
            return;
        }
        binding.submit.setEnabled(false);
        PyBridge.call(r -> {
            binding.submit.setEnabled(true);
            boolean correct = r.optBoolean("correct");
            String feedback = r.optString("feedback");
            if (r.has("expected")) {
                feedback += "\n" + getString(R.string.expected_output, r.optString("expected"));
            }
            showFeedback(correct, feedback);
            if ("code".equals(type)) {
                binding.output.setVisibility(View.VISIBLE);
                String out = r.optString("output");
                binding.output.setText(out.isEmpty() ? getString(R.string.no_output) : out);
            }
            binding.next.setVisibility(correct ? View.VISIBLE : View.GONE);
            Store.recordSubmit(this, lessonId, index, answer, correct, null);
        }, "check", lessonId, index, answer);
    }

    private void askTeacher() {
        hintLevel = Math.min(3, hintLevel + 1);
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String key = prefs.getString(SettingsActivity.KEY_API_KEY, "");
        binding.hintCard.setVisibility(View.VISIBLE);
        binding.hintText.setText(R.string.teacher_thinking);
        binding.hintNote.setVisibility(View.GONE);
        binding.askTeacher.setEnabled(false);
        PyBridge.call(r -> {
            binding.askTeacher.setEnabled(true);
            binding.hintLevel.setText(getString(R.string.hint_level, r.optInt("level", hintLevel)));
            binding.hintText.setText(r.optString("text", r.optString("error")));
            String note = r.optString("note");
            binding.hintNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
            binding.hintNote.setText(note);
            binding.askTeacher.setText(hintLevel >= 3 ? R.string.ask_again : R.string.ask_more);
            Store.recordHint(this, lessonId, index);
        }, "hint", lessonId, index, currentAnswer(), hintLevel, key,
                prefs.getString(SettingsActivity.KEY_BASE_URL, SettingsActivity.DEFAULT_BASE_URL),
                prefs.getString(SettingsActivity.KEY_MODEL, SettingsActivity.DEFAULT_MODEL));
    }

    private void showFeedback(boolean correct, String text) {
        binding.feedback.setVisibility(View.VISIBLE);
        binding.feedback.setText(text);
        binding.feedback.setTextColor(ContextCompat.getColor(this, correct ? R.color.good : R.color.bad));
    }

    /** 下一题；这一关做完就回到关卡页。 */
    private void goNext() {
        if (index + 1 < questionCount) {
            Intent intent = new Intent(this, QuestionActivity.class);
            intent.putExtra(EXTRA_LESSON_ID, lessonId);
            intent.putExtra(EXTRA_INDEX, index + 1);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

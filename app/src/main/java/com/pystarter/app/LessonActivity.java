package com.pystarter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityLessonBinding;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 关卡页：先用大白话讲概念 + 一个比喻 + 一段示例代码（可以点“运行看看”），
 * 下面是这一关的 3 道题，点进去做题。
 */
public class LessonActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "lesson_id";

    private ActivityLessonBinding binding;
    private int lessonId;
    private JSONObject lesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLessonBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, 1);

        PyBridge.call(result -> {
            lesson = result;
            if (result.has("error")) {
                binding.concept.setText(result.optString("error"));
                return;
            }
            setTitle(getString(R.string.lesson_title, lessonId, result.optString("title")));
            binding.concept.setText(result.optString("concept"));
            binding.analogy.setText(result.optString("analogy"));
            binding.example.setText(result.optString("example"));
            buildQuestionButtons(result.optJSONArray("questions"));
        }, "get_lesson", lessonId);

        binding.runExample.setOnClickListener(v -> {
            binding.exampleOutput.setVisibility(View.VISIBLE);
            binding.exampleOutput.setText(R.string.running);
            PyBridge.call(r -> binding.exampleOutput.setText(r.optString("output")),
                    "run_code", binding.example.getText().toString(), "[]");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lesson != null) {
            buildQuestionButtons(lesson.optJSONArray("questions"));
        }
    }

    /** 每道题一个按钮；答对过的题按钮上打勾。 */
    private void buildQuestionButtons(JSONArray questions) {
        binding.questionButtons.removeAllViews();
        if (questions == null) {
            return;
        }
        for (int i = 0; i < questions.length(); i++) {
            int index = i;
            JSONObject q = questions.optJSONObject(i);
            MaterialButton button = (MaterialButton) getLayoutInflater()
                    .inflate(R.layout.item_question_button, binding.questionButtons, false);
            String label = getString(R.string.question_label, i + 1, typeName(q.optString("type")));
            button.setText(label);
            Store.lastAnswer(this, lessonId, index, record -> {
                if (record != null && record.correct) {
                    button.setText(label + "  ✓");
                }
            });
            button.setOnClickListener(v -> {
                Intent intent = new Intent(this, QuestionActivity.class);
                intent.putExtra(QuestionActivity.EXTRA_LESSON_ID, lessonId);
                intent.putExtra(QuestionActivity.EXTRA_INDEX, index);
                startActivity(intent);
            });
            binding.questionButtons.addView(button);
        }
    }

    private String typeName(String type) {
        switch (type) {
            case "choice":
                return getString(R.string.type_choice);
            case "fill":
                return getString(R.string.type_fill);
            default:
                return getString(R.string.type_code);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

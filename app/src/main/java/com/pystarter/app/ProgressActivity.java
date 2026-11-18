package com.pystarter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.QuestionRecord;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityProgressBinding;

/** 我的进度：答对了多少题、连续学习几天，以及错题本（点一下重新做）。 */
public class ProgressActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 36;
    private ActivityProgressBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Store.totalCorrect(this, n -> {
            binding.correctCount.setText(getString(R.string.correct_count, n, TOTAL_QUESTIONS));
            binding.totalProgress.setMax(TOTAL_QUESTIONS);
            binding.totalProgress.setProgress(n);
        });
        Store.streak(this, d -> binding.streakCount.setText(getString(R.string.streak_days, d)));
        Store.wrongQuestions(this, list -> {
            binding.wrongList.removeAllViews();
            binding.wrongEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            for (QuestionRecord r : list) {
                TextView row = (TextView) getLayoutInflater()
                        .inflate(R.layout.item_wrong, binding.wrongList, false);
                row.setText(getString(R.string.wrong_item, r.lessonId, r.questionIndex + 1, r.attempts));
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, QuestionActivity.class);
                    intent.putExtra(QuestionActivity.EXTRA_LESSON_ID, r.lessonId);
                    intent.putExtra(QuestionActivity.EXTRA_INDEX, r.questionIndex);
                    startActivity(intent);
                });
                binding.wrongList.addView(row);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

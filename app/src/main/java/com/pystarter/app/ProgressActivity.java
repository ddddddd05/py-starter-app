package com.pystarter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.data.QuestionRecord;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityProgressBinding;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

        int xp = GameStore.xp(this);
        int level = GameStore.levelOf(xp);
        int[] revenge = GameStore.revengeCount(this);
        binding.playerStats.setText(getString(R.string.player_stats, level, GameStore.titleOf(level), xp,
                GameStore.totalStars(this), GameStore.LESSON_COUNT * 3, revenge[0], revenge[1]));

        Set<String> skills = GameStore.skills(this);
        String skillLine = skills.stream()
                .sorted()
                .collect(Collectors.joining("、"));
        binding.playerStats.append("\n" + (skills.isEmpty() ? getString(R.string.skill_book_empty)
                : getString(R.string.skill_book, skillLine)));

        // 徽章：使用 Java 8 Stream 统计与渲染，拿到的亮色，没拿到的灰色
        binding.badgeList.removeAllViews();
        Set<String> owned = GameStore.badges(this);
        long unlockedBadges = Arrays.stream(GameStore.BADGES)
                .filter(b -> owned.contains(b[0]))
                .count();
        binding.playerStats.append("  |  徽章：" + unlockedBadges + "/" + GameStore.BADGES.length);

        Arrays.stream(GameStore.BADGES).forEach(b -> {
            boolean has = owned.contains(b[0]);
            TextView row = new TextView(this, null, 0, R.style.PxText);
            row.setText(getString(has ? R.string.badge_owned : R.string.badge_locked, b[1], b[2]));
            row.setTextColor(has ? 0xFFFFEC27 : 0xFF5F574F);
            row.setPadding(0, 10, 0, 10);
            binding.badgeList.addView(row);
        });
        Store.wrongQuestions(this, list -> {
            binding.wrongList.removeAllViews();
            binding.wrongEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            list.stream()
                    .filter(r -> r.attempts > 0)
                    .forEach(r -> {
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
                    });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

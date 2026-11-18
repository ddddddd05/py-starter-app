package com.pystarter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.databinding.ActivityResultBinding;
import com.pystarter.app.game.Fx;
import com.pystarter.app.game.Sprites;

import java.util.ArrayList;

/**
 * 结算页。
 * 过关：星星一颗颗弹出来，列出经验来源，显示升级和新徽章；按钮是“下一关 / 再挑战（拿满星）”。
 * 失败：告诉玩家倒在第几战，建议“回去复习”或“再挑战”。
 */
public class ResultActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "lesson_id";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_STARS = "stars";
    public static final String EXTRA_OLD_STARS = "old_stars";
    public static final String EXTRA_BATTLE_XP = "battle_xp";
    public static final String EXTRA_CLEAR_XP = "clear_xp";
    public static final String EXTRA_STAR_XP = "star_xp";
    public static final String EXTRA_OLD_LEVEL = "old_level";
    public static final String EXTRA_NEW_LEVEL = "new_level";
    public static final String EXTRA_MISTAKES = "mistakes";
    public static final String EXTRA_HINTS = "hints";
    public static final String EXTRA_NOTES = "notes";
    public static final String EXTRA_BADGES = "badges";
    public static final String EXTRA_REVENGE = "revenge";
    public static final String EXTRA_FAILED_STAGE = "failed_stage";

    private ActivityResultBinding binding;
    private int lessonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent in = getIntent();
        lessonId = in.getIntExtra(EXTRA_LESSON_ID, 1);
        binding.lessonName.setText(getString(R.string.result_lesson, lessonId));
        binding.toMap.setOnClickListener(v -> finish());

        if (in.getBooleanExtra(EXTRA_SUCCESS, false)) {
            showWin(in);
        } else {
            showFail(in.getIntExtra(EXTRA_FAILED_STAGE, 1));
        }
    }

    private void showWin(Intent in) {
        int stars = in.getIntExtra(EXTRA_STARS, 1);
        binding.resultTitle.setText(R.string.result_win);
        binding.sprite.setSprite(Sprites.hero());
        Fx.idle(binding.sprite);

        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 3; i++) {
            TextView star = new TextView(this, null, 0, R.style.PxText_Title);
            star.setText(i < stars ? "★" : "☆");
            star.setTextSize(44);
            star.setPadding((int) (6 * density), 0, (int) (6 * density), 0);
            binding.starRow.addView(star);
            Fx.pop(star, 250L * (i + 1));
        }
        binding.subText.setText(starReason(stars, in.getIntExtra(EXTRA_MISTAKES, 0),
                in.getIntExtra(EXTRA_HINTS, 0), in.getBooleanExtra(EXTRA_NOTES, false)));

        addReward(getString(R.string.reward_battle), in.getIntExtra(EXTRA_BATTLE_XP, 0));
        int clearXp = in.getIntExtra(EXTRA_CLEAR_XP, 0);
        if (clearXp > 0) {
            addReward(getString(R.string.reward_clear), clearXp);
        }
        int starXp = in.getIntExtra(EXTRA_STAR_XP, 0);
        if (starXp > 0) {
            addReward(getString(R.string.reward_star), starXp);
        }
        String revenge = in.getStringExtra(EXTRA_REVENGE);
        if (revenge != null) {
            addReward(getString(R.string.reward_revenge, revenge), 15);
        }

        StringBuilder extra = new StringBuilder();
        int oldLevel = in.getIntExtra(EXTRA_OLD_LEVEL, 1);
        int newLevel = in.getIntExtra(EXTRA_NEW_LEVEL, 1);
        if (newLevel > oldLevel) {
            extra.append(getString(R.string.level_up, newLevel, GameStore.titleOf(newLevel)));
        }
        ArrayList<String> badges = in.getStringArrayListExtra(EXTRA_BADGES);
        if (badges != null) {
            for (String b : badges) {
                if (extra.length() > 0) {
                    extra.append("\n");
                }
                extra.append(getString(R.string.new_badge, b));
            }
        }
        if (extra.length() > 0) {
            binding.extraText.setVisibility(View.VISIBLE);
            binding.extraText.setText(extra.toString());
            Fx.pop(binding.extraText, 1000);
        }

        boolean hasNext = lessonId < GameStore.LESSON_COUNT;
        if (hasNext) {
            binding.primary.setText(R.string.next_lesson);
            binding.primary.setOnClickListener(v -> {
                Intent intent = new Intent(this, LessonActivity.class);
                intent.putExtra(LessonActivity.EXTRA_LESSON_ID, lessonId + 1);
                startActivity(intent);
                finish();
            });
        } else {
            binding.primary.setText(R.string.all_done);
            binding.primary.setOnClickListener(v -> finish());
        }
        if (stars < 3) {
            binding.secondary.setText(R.string.retry_for_stars);
            binding.secondary.setOnClickListener(v -> retry());
        } else {
            binding.secondary.setVisibility(View.GONE);
        }
    }

    private String starReason(int stars, int mistakes, int hints, boolean notes) {
        if (stars == 3) {
            return getString(R.string.reason_perfect);
        }
        StringBuilder sb = new StringBuilder();
        if (mistakes > 0) {
            sb.append(getString(R.string.reason_mistakes, mistakes));
        }
        if (hints > 0) {
            sb.append(sb.length() > 0 ? "，" : "").append(getString(R.string.reason_hints, hints));
        }
        if (notes) {
            sb.append(sb.length() > 0 ? "，" : "").append(getString(R.string.reason_notes));
        }
        return sb + "\n" + getString(R.string.reason_retry);
    }

    private void addReward(String label, int xp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = new TextView(this, null, 0, R.style.PxText);
        l.setText(label);
        TextView r = new TextView(this, null, 0, R.style.PxText);
        r.setText(getString(R.string.xp_plus, xp));
        r.setTextColor(0xFF00E436);
        row.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(r);
        binding.rewardBox.addView(row);
    }

    private void showFail(int failedStage) {
        binding.resultTitle.setText(R.string.result_fail);
        binding.resultTitle.setTextColor(0xFFFF004D);
        binding.sprite.setSprite(Sprites.enemy("village", 0));
        binding.subText.setText(getString(R.string.fail_text, failedStage));
        binding.rewardBox.setVisibility(View.GONE);
        binding.primary.setText(R.string.go_review);
        binding.primary.setOnClickListener(v -> {
            Intent intent = new Intent(this, LessonActivity.class);
            intent.putExtra(LessonActivity.EXTRA_LESSON_ID, lessonId);
            startActivity(intent);
            finish();
        });
        binding.secondary.setText(R.string.retry);
        binding.secondary.setOnClickListener(v -> retry());
    }

    private void retry() {
        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra(BattleActivity.EXTRA_LESSON_ID, lessonId);
        startActivity(intent);
        finish();
    }
}

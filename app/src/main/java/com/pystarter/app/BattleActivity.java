package com.pystarter.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityBattleBinding;
import com.pystarter.app.game.Fx;
import com.pystarter.app.game.Sprites;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 挑战页：一关 3 场战斗——小怪（选择题）→ 精英（填空题）→ Boss（写代码题）。
 *
 * 规则：
 * - 每关 3 颗心，答错扣 1 颗，扣完挑战失败，从第 1 战重来；
 * - 写代码题可以随便“试运行”，只有点“出招”提交错了才扣血；
 * - 问小精灵（AI 老师）和翻笔记都不扣血，但会影响星级；
 * - 打完 3 场后，如果以前有答错过的题，会有一只“复仇怪”带着当时的错因出现（打赢加经验，打输不扣血）。
 *
 * 判题仍然由 Python 完成（真的运行代码、比对输出）；判题结果里的 events 字段驱动动画。
 */
public class BattleActivity extends AppCompatActivity {

    public static final String EXTRA_LESSON_ID = "lesson_id";
    private static final int MAX_HEARTS = 3;
    private static final int REVENGE_STAGE = 3;

    private ActivityBattleBinding binding;
    private int lessonId;
    private JSONObject info;          // level_info：章节、敌人、学习卡片
    private JSONArray questions;      // 这一关的 3 道题（不含答案）
    private String theme = "village";

    private int stage = 0;
    private int hearts = MAX_HEARTS;
    private int mistakes = 0;
    private int hintsUsed = 0;
    private int battlesWon = 0;
    private boolean usedNotes = false;
    private boolean busy = false;
    private boolean stageWon = false;

    // 当前这场战斗的题目来源（复仇怪的题来自更早的关卡）
    private int qLesson;
    private int qIndex;
    private JSONObject question;
    private String type = "";
    private int hintLevel = 0;
    private int lastChoice = -1;
    private JSONObject revenge;
    private String revengeName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, 1);

        binding.hero.setSprite(Sprites.hero());
        Fx.idle(binding.hero);
        binding.back.setOnClickListener(v -> confirmQuit());
        binding.attack.setOnClickListener(v -> submit(currentAnswer()));
        binding.run.setOnClickListener(v -> tryRun());
        binding.hint.setOnClickListener(v -> askFairy());
        binding.notes.setOnClickListener(v -> openNotes());
        binding.continueBtn.setOnClickListener(v -> nextStage());
        // 战斗中按返回键先确认，免得误触丢掉进度
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });

        PyBridge.call(level -> PyBridge.call(lesson -> {
            if (level.has("error") || lesson.has("error")) {
                binding.npcLine.setText(level.optString("error", lesson.optString("error")));
                return;
            }
            info = level;
            questions = lesson.optJSONArray("questions");
            theme = level.optJSONObject("chapter").optString("theme", "village");
            startStage(0);
        }, "get_lesson", lessonId), "level_info", lessonId);
    }

    private void confirmQuit() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.quit_confirm)
                .setPositiveButton(R.string.quit_yes, (d, w) -> finish())
                .setNegativeButton(R.string.quit_no, null)
                .show();
    }

    // ---------- 战斗流程 ----------

    private void startStage(int s) {
        stage = s;
        stageWon = false;
        hintLevel = 0;
        if (s == REVENGE_STAGE) {
            qLesson = revenge.optInt("lesson");
            qIndex = revenge.optInt("index");
            PyBridge.call(old -> {
                JSONArray qs = old.optJSONArray("questions");
                question = qs == null ? new JSONObject() : qs.optJSONObject(qIndex);
                setupEnemy(Sprites.enemy(theme, 3), getString(R.string.revenge_enemy, revenge.optString("name")));
                binding.stageText.setText(getString(R.string.stage_revenge, qLesson));
                say(getString(R.string.npc_fairy), getString(R.string.revenge_intro, revenge.optString("name")), null);
                showQuestion();
            }, "get_lesson", qLesson);
            return;
        }
        qLesson = lessonId;
        qIndex = s;
        question = questions.optJSONObject(s);
        JSONObject battle = info.optJSONArray("battles").optJSONObject(s);
        setupEnemy(Sprites.enemy(theme, s), battle.optString("enemy"));
        binding.stageText.setText(getString(R.string.stage_title, lessonId, s + 1, battle.optString("stage")));
        say(getString(R.string.npc_fairy), getString(s == 0 ? R.string.battle_intro_0
                : s == 1 ? R.string.battle_intro_1 : R.string.battle_intro_2, battle.optString("enemy")), null);
        showQuestion();
    }

    private void setupEnemy(Sprites.Sprite sprite, String name) {
        Fx.reset(binding.enemy);
        binding.enemy.setSprite(sprite);
        binding.enemy.setFlipped(true);
        Fx.idle(binding.enemy);
        binding.enemyName.setText(name);
        binding.enemyHp.setProgress(100);
        refreshHearts();
    }

    private void refreshHearts() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_HEARTS; i++) {
            sb.append(i < hearts ? "♥" : "♡");
        }
        binding.hearts.setText(sb.toString());
    }

    private void showQuestion() {
        type = question.optString("type");
        binding.prompt.setText(question.optString("prompt"));
        String code = question.optString("code");
        binding.codeBlock.setVisibility(code.isEmpty() ? View.GONE : View.VISIBLE);
        binding.codeBlock.setText(code);
        binding.options.removeAllViews();
        binding.fillTemplate.setVisibility(View.GONE);
        binding.fillInput.setVisibility(View.GONE);
        binding.codeEditor.setVisibility(View.GONE);
        binding.output.setVisibility(View.GONE);
        binding.run.setVisibility(View.GONE);
        binding.attack.setVisibility(View.VISIBLE);
        binding.helpRow.setVisibility(View.VISIBLE);
        binding.continueBtn.setVisibility(View.GONE);
        binding.fillInput.setText("");
        binding.codeEditor.setText(question.optString("starter"));
        binding.hint.setText(R.string.ask_fairy);          // 每场战斗提示重新从第 1 级开始

        switch (type) {
            case "choice":
                // 选择题：点选项就是出招
                binding.attack.setVisibility(View.GONE);
                JSONArray options = question.optJSONArray("options");
                for (int i = 0; options != null && i < options.length(); i++) {
                    int choice = i;
                    TextView b = new TextView(this, null, 0, R.style.PxButton);
                    b.setText(options.optString(i));
                    b.setAllCaps(false);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
                    b.setOnClickListener(v -> {
                        lastChoice = choice;
                        submit(String.valueOf(choice));
                    });
                    binding.options.addView(b, lp);
                }
                break;
            case "fill":
                binding.fillTemplate.setVisibility(View.VISIBLE);
                binding.fillTemplate.setText(question.optString("template"));
                binding.fillInput.setVisibility(View.VISIBLE);
                break;
            default:
                binding.codeEditor.setVisibility(View.VISIBLE);
                binding.run.setVisibility(View.VISIBLE);
                break;
        }
        binding.scroll.scrollTo(0, 0);
    }

    private String currentAnswer() {
        if ("fill".equals(type)) {
            return binding.fillInput.getText().toString();
        }
        return binding.codeEditor.getText().toString();
    }

    private String inputsJson() {
        JSONArray inputs = question.optJSONArray("inputs");
        return inputs == null ? "[]" : inputs.toString();
    }

    /** 试运行：只看结果，不判题、不扣血。 */
    private void tryRun() {
        binding.output.setVisibility(View.VISIBLE);
        binding.output.setText(R.string.running);
        PyBridge.call(r -> {
            JSONObject error = r.optJSONObject("error");
            String text = r.optString("output");
            if (error != null) {
                text = text + (text.isEmpty() ? "" : "\n") + "⚠ " + error.optString("message");
            }
            binding.output.setText(text.isEmpty() ? getString(R.string.no_output) : text);
        }, "run_code", currentAnswer(), inputsJson());
    }

    /** 出招：交给 Python 判题。 */
    private void submit(String answer) {
        if (busy || stageWon) {
            return;
        }
        if (answer.trim().isEmpty()) {
            Fx.shake(binding.card);
            say(getString(R.string.npc_fairy), getString(R.string.answer_first), null);
            return;
        }
        busy = true;
        PyBridge.call(r -> {
            busy = false;
            boolean correct = r.optBoolean("correct");
            Store.recordSubmit(this, qLesson, qIndex, answer, correct, null);   // 错题本和打卡照常记录
            if ("code".equals(type)) {
                binding.output.setVisibility(View.VISIBLE);
                String out = r.optString("output");
                binding.output.setText(out.isEmpty() ? getString(R.string.no_output) : out);
            }
            if (correct) {
                onWin(r);
            } else {
                onMiss(r, answer);
            }
        }, "check", qLesson, qIndex, answer);
    }

    private void onWin(JSONObject r) {
        stageWon = true;
        if ("choice".equals(type) && lastChoice >= 0) {     // 选择题：把选对的那一项标成绿色
            View chosen = binding.options.getChildAt(lastChoice);
            if (chosen != null) {
                chosen.setBackgroundResource(R.drawable.px_btn_green);
            }
        }
        battlesWon++;
        playEvents(r.optJSONArray("events"));
        binding.enemyHp.setProgress(0);
        binding.enemy.postDelayed(() -> Fx.defeat(binding.enemy), 350);
        int xp = stage == REVENGE_STAGE ? 15 : 10;
        Fx.floatText(binding.root, binding.arena, "+" + xp + " XP", 0xFFFFEC27);
        if (stage == REVENGE_STAGE) {
            GameStore.conquerRevenge(this, qLesson, qIndex);
            GameStore.addXp(this, xp);
            revengeName = revenge.optString("name");
            say(getString(R.string.npc_fairy), getString(R.string.revenge_win, revengeName), r.optString("feedback"));
        } else {
            say(getString(R.string.npc_fairy), r.optString("npc"), r.optString("feedback"));
        }
        binding.helpRow.setVisibility(View.GONE);
        binding.attack.setVisibility(View.GONE);
        binding.run.setVisibility(View.GONE);
        binding.continueBtn.setVisibility(View.VISIBLE);
        binding.continueBtn.setText(isLastStage() ? R.string.finish_battle : R.string.continue_battle);
    }

    private void onMiss(JSONObject r, String answer) {
        playEvents(r.optJSONArray("events"));
        Fx.shake(binding.card);
        String detail = r.optString("feedback");
        if (r.has("expected")) {
            detail += "\n" + getString(R.string.expected_output, r.optString("expected"));
        }
        say(getString(R.string.npc_fairy_tag, r.optString("tag_name")), r.optString("npc"), detail);
        if ("choice".equals(type)) {
            View chosen = binding.options.getChildAt(Integer.parseInt(answer));
            if (chosen != null) {
                chosen.setBackgroundResource(R.drawable.px_btn_gray);
                chosen.setEnabled(false);
            }
        }
        if (stage == REVENGE_STAGE) {
            return;                                     // 复仇怪打输不扣血
        }
        mistakes++;
        hearts--;
        GameStore.addRevenge(this, qLesson, qIndex, r.optString("tag"), r.optString("tag_name"));
        refreshHearts();
        Fx.shake(binding.hearts);
        if (hearts <= 0) {
            busy = true;                                // 失败动画期间不再接受出招
            binding.root.postDelayed(this::fail, 900);
        }
    }

    /** 根据判题结果里的 events 播放动画（第 2 步做融入式关卡时，在这里加专属动画）。 */
    private void playEvents(JSONArray events) {
        for (int i = 0; events != null && i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            switch (e.optString("action")) {
                case "attack":
                    Fx.lunge(binding.hero, 60);
                    binding.enemy.postDelayed(() -> Fx.hit(binding.enemy), 200);
                    break;
                case "hurt":
                    Fx.lunge(binding.enemy, -60);
                    binding.hero.postDelayed(() -> Fx.hit(binding.hero), 200);
                    break;
                default:
                    break;
            }
        }
    }

    private boolean isLastStage() {
        if (stage == REVENGE_STAGE) {
            return true;
        }
        return stage == 2 && GameStore.pickRevenge(this, lessonId) == null;
    }

    private void nextStage() {
        if (stage < 2) {
            startStage(stage + 1);
            return;
        }
        if (stage == 2) {
            revenge = GameStore.pickRevenge(this, lessonId);
            if (revenge != null) {
                startStage(REVENGE_STAGE);
                return;
            }
        }
        win();
    }

    // ---------- 结算 ----------

    private void win() {
        int stars = GameStore.computeStars(mistakes, hintsUsed, usedNotes);
        int won = Math.min(battlesWon, 3);
        Store.streak(this, days -> {
            GameStore.Clear c = GameStore.recordClear(this, lessonId, stars, won, hintsUsed, days);
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra(ResultActivity.EXTRA_LESSON_ID, lessonId);
            intent.putExtra(ResultActivity.EXTRA_SUCCESS, true);
            intent.putExtra(ResultActivity.EXTRA_STARS, c.stars);
            intent.putExtra(ResultActivity.EXTRA_OLD_STARS, c.oldStars);
            intent.putExtra(ResultActivity.EXTRA_BATTLE_XP, c.battleXp);
            intent.putExtra(ResultActivity.EXTRA_CLEAR_XP, c.clearXp);
            intent.putExtra(ResultActivity.EXTRA_STAR_XP, c.starXp);
            intent.putExtra(ResultActivity.EXTRA_OLD_LEVEL, c.oldLevel);
            intent.putExtra(ResultActivity.EXTRA_NEW_LEVEL, c.newLevel);
            intent.putExtra(ResultActivity.EXTRA_MISTAKES, mistakes);
            intent.putExtra(ResultActivity.EXTRA_HINTS, hintsUsed);
            intent.putExtra(ResultActivity.EXTRA_NOTES, usedNotes);
            intent.putStringArrayListExtra(ResultActivity.EXTRA_BADGES, new ArrayList<>(c.newBadges));
            intent.putExtra(ResultActivity.EXTRA_REVENGE, revengeName);
            startActivity(intent);
            finish();
        });
    }

    private void fail() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_LESSON_ID, lessonId);
        intent.putExtra(ResultActivity.EXTRA_SUCCESS, false);
        intent.putExtra(ResultActivity.EXTRA_FAILED_STAGE, stage + 1);
        startActivity(intent);
        finish();
    }

    // ---------- 帮助：不扣血，但影响星级 ----------

    private void askFairy() {
        hintLevel = Math.min(3, hintLevel + 1);
        hintsUsed++;
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        say(getString(R.string.npc_fairy), getString(R.string.teacher_thinking), null);
        binding.hint.setEnabled(false);
        PyBridge.call(r -> {
            binding.hint.setEnabled(true);
            String note = r.optString("note");
            say(getString(R.string.hint_level_fairy, r.optInt("level", hintLevel)),
                    r.optString("text", r.optString("error")), note.isEmpty() ? null : note);
            binding.hint.setText(hintLevel >= 3 ? R.string.ask_fairy_again : R.string.ask_fairy_more);
            Store.recordHint(this, qLesson, qIndex);
        }, "hint", qLesson, qIndex, "choice".equals(type) ? "" : currentAnswer(), hintLevel,
                prefs.getString(SettingsActivity.KEY_API_KEY, ""),
                prefs.getString(SettingsActivity.KEY_BASE_URL, SettingsActivity.DEFAULT_BASE_URL),
                prefs.getString(SettingsActivity.KEY_MODEL, SettingsActivity.DEFAULT_MODEL));
    }

    /** 翻笔记：回看这一关学过的卡片和示例。不扣血，但这一关最多 2 星。 */
    private void openNotes() {
        StringBuilder sb = new StringBuilder();
        JSONArray cards = info.optJSONArray("cards");
        for (int i = 0; cards != null && i < cards.length(); i++) {
            sb.append("▸ ").append(cards.optString(i)).append("\n\n");
        }
        sb.append(info.optString("analogy")).append("\n\n").append(getString(R.string.example)).append("：\n")
                .append(info.optString("example"));
        new AlertDialog.Builder(this)
                .setTitle(usedNotes ? R.string.notes_title : R.string.notes_title_first)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.notes_close, null)
                .show();
        usedNotes = true;
    }

    private void say(String speaker, String line, String detail) {
        binding.speaker.setText(speaker);
        binding.npcLine.setText(line);
        binding.feedback.setVisibility(detail == null || detail.isEmpty() ? View.GONE : View.VISIBLE);
        binding.feedback.setText(detail == null ? "" : detail);
        Fx.pop(binding.dialogBox, 0);
    }
}

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
import androidx.lifecycle.ViewModelProvider;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityBattleBinding;
import com.pystarter.app.debug.StepDebugActivity;
import com.pystarter.app.game.Fx;
import com.pystarter.app.game.SoundFx;
import com.pystarter.app.game.Sprites;
import com.pystarter.app.strategy.QuestionStrategyFactory;

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
    private static final int MAX_HEARTS = BattleViewModel.MAX_HEARTS;
    private static final int REVENGE_STAGE = 3;

    private ActivityBattleBinding binding;
    private int lessonId;
    private BattleViewModel st;       // 战斗状态放在 ViewModel 里，旋转屏幕不丢

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBattleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        CodeToolbar.attach(binding.codeEditor);
        lessonId = getIntent().getIntExtra(EXTRA_LESSON_ID, 1);

        binding.hero.setSprite(Sprites.hero());
        Fx.idle(binding.hero);
        binding.back.setOnClickListener(v -> confirmQuit());
        binding.attack.setOnClickListener(v -> submit(currentAnswer()));
        binding.run.setOnClickListener(v -> tryRun());
        // 慢放：一行一行看代码怎么跑，不判题、不扣血
        binding.slow.setOnClickListener(v -> StepDebugActivity.start(this, currentAnswer(), inputsJson()));
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

        st = new ViewModelProvider(this).get(BattleViewModel.class);
        if (st.getLoaded()) {
            restoreUi();                 // 旋转屏幕等原因页面重建：状态还在，直接按状态把界面画回来
            return;
        }
        PyBridge.call(level -> PyBridge.call(lesson -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (level.has("error") || lesson.has("error")) {
                binding.npcLine.setText(level.optString("error", lesson.optString("error")));
                return;
            }
            st.info = level;
            st.questions = lesson.optJSONArray("questions");
            JSONObject chapter = level.optJSONObject("chapter");
            st.theme = chapter != null ? chapter.optString("theme", "village") : "village";
            startStage(0);
        }, "get_lesson", lessonId), "level_info", lessonId);
    }

    /** 页面重建后恢复界面：同一场战斗、同样的心数；这一战已经打赢的话直接显示“继续”。 */
    private void restoreUi() {
        st.busy = false;                 // 重建前如果正在判题，那次结果回不到新页面，允许重新出招
        if (st.hearts <= 0) {
            fail();
            return;
        }
        setupEnemy(Sprites.enemy(st.theme, Math.min(st.stage, REVENGE_STAGE)), st.enemyName);
        binding.stageText.setText(st.stageTitle);
        say(getString(R.string.npc_fairy), getString(R.string.battle_resume), null);
        showQuestion();                  // 输入框里写到一半的内容由安卓自动恢复
        if (st.stageWon) {
            binding.enemyHp.setProgress(0);
            binding.enemy.setAlpha(0f);
            binding.helpRow.setVisibility(View.GONE);
            binding.attack.setVisibility(View.GONE);
            binding.run.setVisibility(View.GONE);
            binding.slow.setVisibility(View.GONE);
            binding.options.removeAllViews();
            binding.continueBtn.setVisibility(View.VISIBLE);
            binding.continueBtn.setText(isLastStage() ? R.string.finish_battle : R.string.continue_battle);
        }
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
        st.stage = s;
        st.stageWon = false;
        st.hintLevel = 0;
        if (s == REVENGE_STAGE) {
            st.qLesson = st.revenge.optInt("lesson");
            st.qIndex = st.revenge.optInt("index");
            PyBridge.call(old -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                JSONArray qs = old.optJSONArray("questions");
                st.question = qs == null ? new JSONObject() : qs.optJSONObject(st.qIndex);
                setupEnemy(Sprites.enemy(st.theme, 3), getString(R.string.revenge_enemy, st.revenge.optString("name")));
                st.stageTitle = getString(R.string.stage_revenge, st.qLesson);
                binding.stageText.setText(st.stageTitle);
                say(getString(R.string.npc_fairy), getString(R.string.revenge_intro, st.revenge.optString("name")), null);
                showQuestion();
            }, "get_lesson", st.qLesson);
            return;
        }
        st.qLesson = lessonId;
        st.qIndex = s;
        st.question = st.questions.optJSONObject(s);
        JSONObject battle = st.info.optJSONArray("battles").optJSONObject(s);
        setupEnemy(Sprites.enemy(st.theme, s), battle.optString("enemy"));
        st.stageTitle = getString(R.string.stage_title, lessonId, s + 1, battle.optString("stage"));
        binding.stageText.setText(st.stageTitle);
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
        st.enemyName = name;
        binding.enemyHp.setProgress(100);
        refreshHearts();
    }

    private void refreshHearts() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_HEARTS; i++) {
            sb.append(i < st.hearts ? "♥" : "♡");
        }
        binding.hearts.setText(sb.toString());
    }

    private void showQuestion() {
        st.type = st.question.optString("type");
        binding.prompt.setText(st.question.optString("prompt"));
        String code = st.question.optString("code");
        binding.codeBlock.setVisibility(code.isEmpty() ? View.GONE : View.VISIBLE);
        binding.codeBlock.setText(code);
        binding.options.removeAllViews();
        binding.fillTemplate.setVisibility(View.GONE);
        binding.fillInput.setVisibility(View.GONE);
        binding.codeEditor.setVisibility(View.GONE);
        CodeToolbar.setVisible(binding.codeEditor, false);
        binding.output.setVisibility(View.GONE);
        binding.run.setVisibility(View.GONE);
        binding.slow.setVisibility(View.GONE);
        binding.attack.setVisibility(View.VISIBLE);
        binding.helpRow.setVisibility(View.VISIBLE);
        binding.continueBtn.setVisibility(View.GONE);
        binding.fillInput.setText("");
        binding.codeEditor.setText(st.question.optString("starter"));
        binding.hint.setText(R.string.ask_fairy);          // 每场战斗提示重新从第 1 级开始

        QuestionStrategyFactory.get(st.type).render(binding, st.question, (choice, ans) -> {
            st.lastChoice = choice;
            submit(ans);
        });
        binding.scroll.scrollTo(0, 0);
    }

    private String currentAnswer() {
        return QuestionStrategyFactory.get(st.type).extractAnswer(binding);
    }

    private String inputsJson() {
        JSONArray inputs = st.question.optJSONArray("inputs");
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
        if (st.busy || st.stageWon) {
            return;
        }
        if (answer.trim().isEmpty()) {
            Fx.shake(binding.card);
            say(getString(R.string.npc_fairy), getString(R.string.answer_first), null);
            return;
        }
        st.busy = true;
        PyBridge.call(r -> {
            st.busy = false;
            boolean correct = r.optBoolean("correct");
            Store.recordSubmit(this, st.qLesson, st.qIndex, answer, correct, null);   // 错题本和打卡照常记录
            if ("code".equals(st.type)) {
                binding.output.setVisibility(View.VISIBLE);
                String out = r.optString("output");
                binding.output.setText(out.isEmpty() ? getString(R.string.no_output) : out);
            }
            if (correct) {
                onWin(r);
            } else {
                onMiss(r, answer);
            }
        }, "check", st.qLesson, st.qIndex, answer);
    }

    private void onWin(JSONObject r) {
        st.stageWon = true;
        if ("choice".equals(st.type) && st.lastChoice >= 0) {     // 选择题：把选对的那一项标成绿色
            View chosen = binding.options.getChildAt(st.lastChoice);
            if (chosen != null) {
                chosen.setBackgroundResource(R.drawable.px_btn_green);
            }
        }
        st.battlesWon++;
        long animMs = playEvents(r.optJSONArray("events"));     // 连击等融入式动画播完再倒下
        binding.enemy.postDelayed(() -> {
            binding.enemyHp.setProgress(0);
            Fx.defeat(binding.enemy);
        }, Math.max(350, animMs));
        int xp = st.stage == REVENGE_STAGE ? 15 : 10;
        Fx.floatText(binding.root, binding.arena, "+" + xp + " XP", 0xFFFFEC27);
        String mpText = applyMp(r.optJSONObject("mp"));
        if (st.stage == REVENGE_STAGE) {
            GameStore.conquerRevenge(this, st.qLesson, st.qIndex);
            GameStore.addXp(this, xp);
            st.revengeName = st.revenge.optString("name");
            say(getString(R.string.npc_fairy), getString(R.string.revenge_win, st.revengeName),
                    join(r.optString("feedback"), mpText));
        } else {
            say(getString(R.string.npc_fairy), r.optString("npc"), join(r.optString("feedback"), mpText));
        }
        binding.helpRow.setVisibility(View.GONE);
        binding.attack.setVisibility(View.GONE);
        binding.run.setVisibility(View.GONE);
        binding.slow.setVisibility(View.GONE);
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
        if ("choice".equals(st.type)) {
            View chosen = binding.options.getChildAt(Integer.parseInt(answer));
            if (chosen != null) {
                chosen.setBackgroundResource(R.drawable.px_btn_gray);
                chosen.setEnabled(false);
            }
        }
        if (st.stage == REVENGE_STAGE) {
            return;                                     // 复仇怪打输不扣血
        }
        st.mistakes++;
        st.hearts--;
        GameStore.addRevenge(this, st.qLesson, st.qIndex, r.optString("tag"), r.optString("tag_name"));
        refreshHearts();
        Fx.shake(binding.hearts);
        if (st.hearts <= 0) {
            st.busy = true;                                // 失败动画期间不再接受出招
            binding.root.postDelayed(this::fail, 900);
        }
    }

    /**
     * 根据判题结果里的 events 播放动画，返回动画总时长（毫秒）。
     * events 由 Python 按代码的真实执行过程生成：
     *   attack 普通一击；hurt 主角受伤；
     *   combo 循环跑了几轮就连击几刀（融入式：for / while）；
     *   branch 实际走了 if 还是 else（融入式：判断）；
     *   skill 自己定义的函数变成技能（融入式：def）。
     */
    private long playEvents(JSONArray events) {
        long t = 0;
        for (int i = 0; events != null && i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            switch (e.optString("action")) {
                case "attack":
                    SoundFx.attack(this);
                    Fx.lunge(binding.hero, 60);
                    binding.enemy.postDelayed(() -> Fx.hit(binding.enemy), 200);
                    t = Math.max(t, 420);
                    break;
                case "hurt":
                    SoundFx.hit(this);
                    Fx.lunge(binding.enemy, -60);
                    binding.hero.postDelayed(() -> Fx.hit(binding.hero), 200);
                    break;
                case "combo": {
                    int times = e.optInt("times", 1);
                    long start = 450;
                    for (int k = 0; k < times; k++) {
                        int hit = k + 1;
                        binding.hero.postDelayed(() -> {
                            Fx.lunge(binding.hero, 60);
                            Fx.hit(binding.enemy);
                            binding.enemyHp.setProgress(Math.max(0, 100 - hit * 100 / times));
                            Fx.floatText(binding.root, binding.enemy, hit + " 连击", 0xFFFF77A8);
                        }, start + k * 330L);
                    }
                    t = Math.max(t, start + times * 330L + 200);
                    break;
                }
                case "branch": {
                    String path = e.optString("path");
                    binding.hero.postDelayed(() -> {
                        Fx.floatText(binding.root, binding.hero,
                                getString(R.string.branch_float, path), 0xFF29ADFF);
                        binding.hero.animate().translationYBy(-40).setDuration(150)
                                .withEndAction(() -> binding.hero.animate().translationYBy(40).setDuration(150).start())
                                .start();
                    }, 450);
                    t = Math.max(t, 900);
                    break;
                }
                case "skill": {
                    String name = e.optString("name");
                    GameStore.addSkill(this, name);
                    binding.hero.postDelayed(() -> {
                        Fx.floatText(binding.root, binding.arena, getString(R.string.skill_float, name), 0xFF00E436);
                        Fx.hit(binding.enemy);
                        Fx.hit(binding.enemy);
                    }, 500);
                    t = Math.max(t, 1000);
                    break;
                }
                default:
                    break;
            }
        }
        return t;
    }

    /** MP（简洁度）：写得和参考写法一样简洁就加 5 经验；太啰嗦就给一句提示。返回要显示的说明。 */
    private String applyMp(JSONObject mp) {
        if (mp == null) {
            return null;
        }
        int used = mp.optInt("used");
        int par = mp.optInt("par");
        if (mp.optBoolean("bonus")) {
            GameStore.addXp(this, 5);
            binding.root.postDelayed(() -> Fx.floatText(binding.root, binding.hero, "+5 MP", 0xFF29ADFF), 600);
            return getString(R.string.mp_bonus, used, par);
        }
        String tip = mp.optString("tip");
        return getString(R.string.mp_long, used, par) + (tip.isEmpty() ? "" : "\n" + tip);
    }

    private static String join(String a, String b) {
        if (b == null || b.isEmpty()) {
            return a;
        }
        return a == null || a.isEmpty() ? b : a + "\n" + b;
    }

    private boolean isLastStage() {
        if (st.stage == REVENGE_STAGE) {
            return true;
        }
        return st.stage == 2 && GameStore.pickRevenge(this, lessonId) == null;
    }

    private void nextStage() {
        if (st.stage < 2) {
            startStage(st.stage + 1);
            return;
        }
        if (st.stage == 2) {
            st.revenge = GameStore.pickRevenge(this, lessonId);
            if (st.revenge != null) {
                startStage(REVENGE_STAGE);
                return;
            }
        }
        win();
    }

    // ---------- 结算 ----------

    private void win() {
        SoundFx.win(this);
        int stars = GameStore.computeStars(st.mistakes, st.hintsUsed, st.usedNotes);
        int won = Math.min(st.battlesWon, 3);
        Store.streak(this, days -> {
            GameStore.Clear c = GameStore.recordClear(this, lessonId, stars, won, st.hintsUsed, days);
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
            intent.putExtra(ResultActivity.EXTRA_MISTAKES, st.mistakes);
            intent.putExtra(ResultActivity.EXTRA_HINTS, st.hintsUsed);
            intent.putExtra(ResultActivity.EXTRA_NOTES, st.usedNotes);
            intent.putStringArrayListExtra(ResultActivity.EXTRA_BADGES, new ArrayList<>(c.newBadges));
            intent.putExtra(ResultActivity.EXTRA_REVENGE, st.revengeName);
            startActivity(intent);
            finish();
        });
    }

    private void fail() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(ResultActivity.EXTRA_LESSON_ID, lessonId);
        intent.putExtra(ResultActivity.EXTRA_SUCCESS, false);
        intent.putExtra(ResultActivity.EXTRA_FAILED_STAGE, st.stage + 1);
        startActivity(intent);
        finish();
    }

    // ---------- 帮助：不扣血，但影响星级 ----------

    private void askFairy() {
        st.hintLevel = Math.min(3, st.hintLevel + 1);
        st.hintsUsed++;
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        say(getString(R.string.npc_fairy), getString(R.string.teacher_thinking), null);
        binding.hint.setEnabled(false);
        PyBridge.call(r -> {
            binding.hint.setEnabled(true);
            String note = r.optString("note");
            say(getString(R.string.hint_level_fairy, r.optInt("level", st.hintLevel)),
                    r.optString("text", r.optString("error")), note.isEmpty() ? null : note);
            binding.hint.setText(st.hintLevel >= 3 ? R.string.ask_fairy_again : R.string.ask_fairy_more);
            Store.recordHint(this, st.qLesson, st.qIndex);
        }, "hint", st.qLesson, st.qIndex, "choice".equals(st.type) ? "" : currentAnswer(), st.hintLevel,
                prefs.getString(SettingsActivity.KEY_API_KEY, ""),
                prefs.getString(SettingsActivity.KEY_BASE_URL, SettingsActivity.DEFAULT_BASE_URL),
                prefs.getString(SettingsActivity.KEY_MODEL, SettingsActivity.DEFAULT_MODEL));
    }

    /** 翻笔记：回看这一关学过的卡片和示例。不扣血，但这一关最多 2 星。 */
    private void openNotes() {
        StringBuilder sb = new StringBuilder();
        JSONArray cards = st.info.optJSONArray("cards");
        for (int i = 0; cards != null && i < cards.length(); i++) {
            sb.append("▸ ").append(cards.optString(i)).append("\n\n");
        }
        sb.append(st.info.optString("analogy")).append("\n\n").append(getString(R.string.example)).append("：\n")
                .append(st.info.optString("example"));
        new AlertDialog.Builder(this)
                .setTitle(st.usedNotes ? R.string.notes_title : R.string.notes_title_first)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.notes_close, null)
                .show();
        st.usedNotes = true;
    }

    private void say(String speaker, String line, String detail) {
        binding.speaker.setText(speaker);
        binding.npcLine.setText(line);
        binding.feedback.setVisibility(detail == null || detail.isEmpty() ? View.GONE : View.VISIBLE);
        binding.feedback.setText(detail == null ? "" : detail);
        Fx.pop(binding.dialogBox, 0);
    }
}

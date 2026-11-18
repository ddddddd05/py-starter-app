package com.pystarter.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityMainBinding;
import com.pystarter.app.game.Fx;
import com.pystarter.app.game.PixelSpriteView;
import com.pystarter.app.game.SoundFx;
import com.pystarter.app.game.Sprites;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 首页：闯关地图。
 * 12 关分 3 章，排成左右弯曲的路线；每关显示是否学过、挑战拿了几颗星，没解锁的显示锁。
 * 解锁规则：上一关挑战通过 → 这一关可以学习；这一关学完 → 可以挑战。
 */
public class MainActivity extends AppCompatActivity {

    /** 路线左右摆动：每关的横向位置（占可用宽度的比例）。 */
    private static final float[] ZIGZAG = {0.08f, 0.40f, 0.62f, 0.32f};

    private ActivityMainBinding binding;
    private JSONObject mapInfo;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.navProgress.setOnClickListener(v -> {
            SoundFx.click(this);
            startActivity(new Intent(this, ProgressActivity.class));
        });
        binding.navMaze.setOnClickListener(v -> {
            SoundFx.click(this);
            startActivity(new Intent(this, MazeActivity.class));
        });
        binding.navSettings.setOnClickListener(v -> {
            SoundFx.click(this);
            startActivity(new Intent(this, SettingsActivity.class));
        });

        binding.loading.setVisibility(View.VISIBLE);
        PyBridge.call(info -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.loading.setVisibility(View.GONE);
            mapInfo = info;
            buildMap();
            maybeShowIntro();
        }, "map_info");
        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHud();
        if (mapInfo != null) {
            buildMap();               // 从学习 / 挑战回来时刷新解锁和星级
        }
    }

    private void refreshHud() {
        int xp = GameStore.xp(this);
        int level = GameStore.levelOf(xp);
        binding.levelTitle.setText(getString(R.string.hud_level, level, GameStore.titleOf(level)));
        binding.starTotal.setText(getString(R.string.hud_stars, GameStore.totalStars(this),
                GameStore.LESSON_COUNT * 3));
        binding.xpBar.setMax(GameStore.XP_PER_LEVEL);
        binding.xpBar.setProgress(xp % GameStore.XP_PER_LEVEL);
        Store.streak(this, days -> binding.xpText.setText(getString(R.string.hud_xp,
                xp % GameStore.XP_PER_LEVEL, GameStore.XP_PER_LEVEL, days)));
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void buildMap() {
        LinearLayout box = binding.mapContainer;
        box.removeAllViews();
        JSONArray chapters = mapInfo.optJSONArray("chapters");
        JSONArray lessons = mapInfo.optJSONArray("lessons");
        if (chapters == null || lessons == null) {
            TextView err = new TextView(this);
            err.setText(mapInfo.optString("error"));
            box.addView(err);
            return;
        }
        int current = GameStore.currentLesson(this);
        int count = 0;

        // 顶部快捷条：试炼迷宫卡片 + 演示模式开关
        LinearLayout topCard = new LinearLayout(this);
        topCard.setOrientation(LinearLayout.HORIZONTAL);
        topCard.setGravity(Gravity.CENTER_VERTICAL);
        topCard.setBackgroundResource(R.drawable.px_panel);
        topCard.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tcp.topMargin = dp(8);
        tcp.bottomMargin = dp(6);

        TextView mazeCard = new TextView(this, null, 0, R.style.PxText);
        mazeCard.setText("⚔ 试炼迷宫");
        mazeCard.setTextColor(0xFFFFEC27);
        LinearLayout.LayoutParams mcp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        mazeCard.setOnClickListener(v -> {
            SoundFx.click(this);
            startActivity(new Intent(this, MazeActivity.class));
        });
        topCard.addView(mazeCard, mcp);

        TextView rankPill = new TextView(this, null, 0, R.style.PxText_Small);
        rankPill.setText("🏆 排行榜");
        rankPill.setTextColor(0xFF000000);
        rankPill.setBackgroundResource(R.drawable.px_btn_yellow);
        rankPill.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.rightMargin = dp(6);
        rankPill.setOnClickListener(v -> {
            SoundFx.click(this);
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
        topCard.addView(rankPill, rlp);

        TextView demoPill = new TextView(this, null, 0, R.style.PxText_Small);
        boolean demo = GameStore.isDemoMode(this);
        demoPill.setText(demo ? "演示:全开" : "演示解锁");
        demoPill.setTextColor(demo ? 0xFF000000 : 0xFFFFF1E8);
        demoPill.setBackgroundResource(demo ? R.drawable.px_btn_green : R.drawable.px_btn_gray);
        demoPill.setPadding(dp(8), dp(4), dp(8), dp(4));
        demoPill.setOnClickListener(v -> {
            SoundFx.click(this);
            boolean next = !GameStore.isDemoMode(this);
            GameStore.setDemoMode(this, next);
            Toast.makeText(this, next ? "演示模式已开启：全部关卡已解锁！" : "演示模式已关闭", Toast.LENGTH_SHORT).show();
            buildMap();
        });
        topCard.addView(demoPill);
        box.addView(topCard, tcp);

        int[] chapterBg = {0xFF152E24, 0xFF132336, 0xFF261A36};
        for (int c = 0; c < chapters.length(); c++) {
            JSONObject ch = chapters.optJSONObject(c);
            TextView banner = new TextView(this, null, 0, R.style.PxText_Title);
            banner.setText(getString(R.string.chapter_banner, ch.optInt("id"), ch.optString("title")));
            banner.setBackgroundResource(R.drawable.bg_banner);
            banner.setGravity(Gravity.CENTER);
            banner.setPadding(dp(8), dp(8), dp(8), dp(8));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.topMargin = dp(12);
            bp.bottomMargin = dp(4);
            box.addView(banner, bp);

            LinearLayout ground = new LinearLayout(this);
            ground.setOrientation(LinearLayout.VERTICAL);
            ground.setBackgroundColor(chapterBg[c % chapterBg.length]);
            ground.setPadding(dp(4), dp(6), dp(4), dp(8));

            JSONArray ids = ch.optJSONArray("lessons");
            for (int i = 0; ids != null && i < ids.length(); i++) {
                int id = ids.optInt(i);
                JSONObject lesson = lessons.optJSONObject(id - 1);
                ground.addView(buildNode(id, lesson, i == ids.length() - 1, id == current, count++));
                if (i < ids.length() - 1) {
                    TextView pathDots = new TextView(this, null, 0, R.style.PxText_Small);
                    pathDots.setText("⋮");
                    pathDots.setTextColor(0xFF83769C);
                    pathDots.setGravity(Gravity.CENTER);
                    ground.addView(pathDots);
                }
            }
            box.addView(ground);
        }
        if (GameStore.allCleared(this)) {                // 全部通关：终点放一个宝箱
            PixelSpriteView chest = new PixelSpriteView(this);
            chest.setSprite(Sprites.chest());
            box.addView(chest, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)));
        }
    }

    /** 一个关卡节点：方块按钮 + 右边的标题、星星、状态。 */
    private View buildNode(int id, JSONObject lesson, boolean boss, boolean isCurrent, int position) {
        boolean learnOpen = GameStore.isLearnUnlocked(this, id);
        boolean learned = GameStore.isLearned(this, id);
        int stars = GameStore.stars(this, id);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int width = getResources().getDisplayMetrics().widthPixels - dp(20);
        row.setPadding((int) (width * ZIGZAG[position % ZIGZAG.length]), dp(10), 0, dp(10));

        TextView node = new TextView(this, null, 0, R.style.PxText_Title);
        node.setGravity(Gravity.CENTER);
        node.setTextColor(0xFF000000);
        int size = boss ? dp(76) : dp(64);
        if (!learnOpen) {
            node.setBackgroundResource(R.drawable.px_node_locked);
            node.setText(R.string.node_locked);
            node.setTextColor(0xFFC2C3C7);
        } else if (stars > 0) {
            node.setBackgroundResource(R.drawable.px_node_done);
            node.setText(String.valueOf(id));
        } else if (learned) {
            node.setBackgroundResource(R.drawable.px_node_current);
            node.setText(String.valueOf(id));
        } else {
            node.setBackgroundResource(R.drawable.px_node_learn);
            node.setText(String.valueOf(id));
        }
        row.addView(node, new LinearLayout.LayoutParams(size, size));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        TextView title = new TextView(this, null, 0, R.style.PxText);
        title.setText(boss ? getString(R.string.node_boss, lesson.optString("keyword"))
                : lesson.optString("keyword"));
        title.setTextColor(learnOpen ? 0xFFFFF1E8 : 0xFF5F574F);
        TextView starText = new TextView(this, null, 0, R.style.PxText);
        starText.setText(starString(stars));
        starText.setTextColor(learnOpen ? 0xFFFFEC27 : 0xFF5F574F);     // 没解锁的关星星是灰色
        TextView status = new TextView(this, null, 0, R.style.PxText_Small);
        status.setText(!learnOpen ? getString(R.string.node_status_locked)
                : stars > 0 ? getString(R.string.node_status_cleared)
                : learned ? getString(R.string.node_status_challenge)
                : getString(R.string.node_status_learn));
        info.addView(title);
        info.addView(starText);
        info.addView(status);
        row.addView(info);

        if (isCurrent && learnOpen) {
            PixelSpriteView hero = new PixelSpriteView(this);
            hero.setSprite(Sprites.hero());
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(40), dp(40));
            hp.leftMargin = dp(8);
            row.addView(hero, hp);
            Fx.idle(hero);
        }

        View.OnClickListener open = v -> onNodeClick(id, lesson, learnOpen, learned, stars);
        node.setOnClickListener(open);
        info.setOnClickListener(open);
        return row;
    }

    static String starString(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(i < stars ? "★" : "☆");
        }
        return sb.toString();
    }

    private void onNodeClick(int id, JSONObject lesson, boolean learnOpen, boolean learned, int stars) {
        if (!learnOpen) {
            SoundFx.hit(this);
            Fx.shake(binding.mapContainer);
            Toast.makeText(this, getString(R.string.locked_toast, id - 1), Toast.LENGTH_SHORT).show();
            return;
        }
        SoundFx.click(this);
        if (!learned) {
            openLearn(id);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.lesson_title, id, lesson.optString("title")))
                .setMessage(stars > 0 ? getString(R.string.dialog_best, starString(stars))
                        : getString(R.string.dialog_ready))
                .setPositiveButton(R.string.go_challenge, (d, w) -> openBattle(id))
                .setNegativeButton(R.string.go_review, (d, w) -> openLearn(id))
                .show();
    }

    private void openLearn(int id) {
        Intent intent = new Intent(this, LessonActivity.class);
        intent.putExtra(LessonActivity.EXTRA_LESSON_ID, id);
        startActivity(intent);
    }

    private void openBattle(int id) {
        Intent intent = new Intent(this, BattleActivity.class);
        intent.putExtra(BattleActivity.EXTRA_LESSON_ID, id);
        startActivity(intent);
    }

    /** 首次打开时的像素剧情开场：交代世界观并可一键跳过。 */
    private void maybeShowIntro() {
        if (GameStore.isIntroSeen(this)) {
            return;
        }
        GameStore.markIntroSeen(this);
        new AlertDialog.Builder(this)
                .setTitle("✨ 欢迎来到代码王国")
                .setMessage("小法师：不好了！代码王国被 Bug 怪占领了！\n\n"
                        + "• 先学习概念、动手改代码，再去挑战 Bug 怪；\n"
                        + "• 还可以随时点击「试炼迷宫」，写代码亲自指挥小骑士走格子！\n\n"
                        + "（提示：右上角“演示解锁”可一键体验全部关卡）")
                .setPositiveButton("开始冒险！", (d, w) -> SoundFx.star(this))
                .setNegativeButton("跳过", null)
                .show();
    }

    /** Android 13 起发通知要用户同意；只在第一次打开时问一次。 */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}

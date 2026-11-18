package com.pystarter.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.databinding.ActivityLeaderboardBinding;
import com.pystarter.app.game.SoundFx;
import com.pystarter.app.net.LeaderboardClient;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 在线排行榜页：通过 OkHttp 调用 Spring Boot + MySQL 后端，
 * 支持上传本地学习星数/经验与查看全服 Top 50。
 */
public class LeaderboardActivity extends AppCompatActivity {

    private ActivityLeaderboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int stars = GameStore.totalStars(this);
        int xp = GameStore.xp(this);
        int level = GameStore.levelOf(xp);
        binding.myStats.setText("我的进度：Lv." + level + " · " + stars + " ★ · " + xp + " XP");
        binding.nicknameInput.setText(LeaderboardClient.nickname(this));
        binding.serverUrlInput.setText(LeaderboardClient.serverUrl(this));

        binding.back.setOnClickListener(v -> finish());
        binding.refreshBtn.setOnClickListener(v -> {
            SoundFx.click(this);
            saveInputs();
            loadRank();
        });
        binding.uploadBtn.setOnClickListener(v -> {
            SoundFx.click(this);
            saveInputs();
            String nick = LeaderboardClient.nickname(this);
            binding.statusMsg.setText("正在上传成绩...");
            LeaderboardClient.submitScore(this, nick, stars, xp, level, (ok, data, err) -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (ok) {
                    SoundFx.star(this);
                    binding.statusMsg.setText("上传成功！已同步到全服排行榜");
                    loadRank();
                } else {
                    SoundFx.hit(this);
                    binding.statusMsg.setText(err);
                }
            });
        });

        loadRank();
    }

    private void saveInputs() {
        LeaderboardClient.saveProfile(
                this,
                binding.nicknameInput.getText().toString(),
                binding.serverUrlInput.getText().toString()
        );
    }

    private void loadRank() {
        binding.statusMsg.setText("正在加载排行榜...");
        LeaderboardClient.fetchTop50(this, (ok, data, err) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            binding.rankList.removeAllViews();
            if (!ok || data == null) {
                binding.statusMsg.setText(err);
                return;
            }
            JSONArray items = data.optJSONArray("items");
            if (items == null || items.length() == 0) {
                binding.statusMsg.setText("排行榜暂无记录，快来做第一个上榜的骑士！");
                return;
            }
            binding.statusMsg.setText("共 " + items.length() + " 位上榜骑士（按星数、经验排序）");
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.optJSONObject(i);
                TextView row = new TextView(this, null, 0, R.style.PxText);
                int rank = it.optInt("rank");
                String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
                row.setText(medal + "  " + it.optString("nickname")
                        + "   Lv." + it.optInt("level")
                        + "   " + it.optInt("stars") + "★   " + it.optInt("xp") + " XP");
                row.setTextColor(rank <= 3 ? 0xFFFFEC27 : 0xFFFFF1E8);
                row.setBackgroundResource(R.drawable.px_panel);
                row.setPadding(24, 18, 24, 18);
                binding.rankList.addView(row);
            }
        });
    }
}

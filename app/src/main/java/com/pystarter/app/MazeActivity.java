package com.pystarter.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.databinding.ActivityMazeBinding;
import com.pystarter.app.game.SoundFx;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 试炼迷宫：写 Python 代码（move/turn_left/turn_right/collect）直接指挥像素小骑士走迷宫。
 */
public class MazeActivity extends AppCompatActivity {

    public static final String EXTRA_MAZE_ID = "maze_id";

    private ActivityMazeBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private JSONArray mazes = new JSONArray();
    private int currentMazeId = 1;
    private Runnable animTask;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMazeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentMazeId = getIntent().getIntExtra(EXTRA_MAZE_ID, 1);
        CodeToolbar.attach(binding.codeInput);

        binding.back.setOnClickListener(v -> finish());
        binding.resetBtn.setOnClickListener(v -> {
            SoundFx.click(this);
            stopAnim();
            loadCurrentMaze(true);
        });
        binding.hintBtn.setOnClickListener(v -> {
            SoundFx.click(this);
            JSONObject m = findMaze(currentMazeId);
            if (m != null) {
                binding.statusText.setText("💡 提示：" + m.optString("hint"));
            }
        });
        binding.nextMaze.setOnClickListener(v -> {
            SoundFx.click(this);
            if (mazes.length() > 0) {
                currentMazeId = (currentMazeId % mazes.length()) + 1;
                loadCurrentMaze(true);
            }
        });
        binding.runBtn.setOnClickListener(v -> runCode());

        PyBridge.listMazes(arr -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            mazes = arr;
            loadCurrentMaze(true);
        });
    }

    @Override
    protected void onDestroy() {
        stopAnim();
        super.onDestroy();
    }

    private JSONObject findMaze(int id) {
        for (int i = 0; i < mazes.length(); i++) {
            JSONObject m = mazes.optJSONObject(i);
            if (m != null && m.optInt("id") == id) {
                return m;
            }
        }
        return mazes.optJSONObject(0);
    }

    private void loadCurrentMaze(boolean resetCode) {
        stopAnim();
        JSONObject m = findMaze(currentMazeId);
        if (m == null) {
            return;
        }
        currentMazeId = m.optInt("id");
        binding.title.setText(m.optString("title"));
        binding.statusText.setText(m.optString("desc"));
        if (resetCode) {
            binding.codeInput.setText(m.optString("starter"));
        }

        JSONArray gridArr = m.optJSONArray("grid");
        List<String> rows = new ArrayList<>();
        Set<String> gems = new HashSet<>();
        int sx = 0, sy = 0;
        if (gridArr != null) {
            for (int y = 0; y < gridArr.length(); y++) {
                String row = gridArr.optString(y);
                rows.add(row);
                for (int x = 0; x < row.length(); x++) {
                    char ch = row.charAt(x);
                    if (ch == 'S') {
                        sx = x;
                        sy = y;
                    } else if (ch == 'G') {
                        gems.add(x + "," + y);
                    }
                }
            }
        }
        binding.mazeView.setMaze(rows, sx, sy, m.optInt("start_dir", 0), gems);
    }

    private void runCode() {
        if (busy) {
            return;
        }
        SoundFx.click(this);
        stopAnim();
        busy = true;
        binding.runBtn.setText("运行中…");
        String code = binding.codeInput.getText().toString();
        PyBridge.call(res -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            busy = false;
            binding.runBtn.setText("▶ 运行指挥骑士");
            JSONArray frames = res.optJSONArray("frames");
            boolean won = res.optBoolean("won", false);
            String feedback = res.optString("feedback", res.optString("error", ""));
            if (frames == null || frames.length() == 0) {
                SoundFx.hit(this);
                binding.statusText.setText(feedback);
                return;
            }
            playFrames(frames, 0, won, feedback);
        }, "run_maze", currentMazeId, code);
    }

    private void playFrames(JSONArray frames, int idx, boolean won, String finalFeedback) {
        if (idx >= frames.length() || isFinishing() || isDestroyed()) {
            binding.statusText.setText(finalFeedback);
            if (won) {
                SoundFx.win(this);
                GameStore.markMazeCleared(this, currentMazeId);
            } else {
                SoundFx.hit(this);
            }
            return;
        }
        JSONObject f = frames.optJSONObject(idx);
        if (f != null) {
            Set<String> gems = new HashSet<>();
            JSONArray gArr = f.optJSONArray("gems");
            if (gArr != null) {
                for (int i = 0; i < gArr.length(); i++) {
                    JSONArray pt = gArr.optJSONArray(i);
                    if (pt != null) {
                        gems.add(pt.optInt(0) + "," + pt.optInt(1));
                    }
                }
            }
            String action = f.optString("action");
            boolean isHit = "hit".equals(action);
            binding.mazeView.updateFrame(f.optInt("x"), f.optInt("y"), f.optInt("dir"), gems, isHit);
            String msg = f.optString("msg");
            if (!msg.isEmpty()) {
                binding.statusText.setText(msg);
            }
            if ("move".equals(action) || "turn".equals(action)) {
                SoundFx.step(this);
            } else if ("collect".equals(action)) {
                SoundFx.star(this);
            } else if (isHit) {
                SoundFx.hit(this);
            }
        }
        animTask = () -> playFrames(frames, idx + 1, won, finalFeedback);
        handler.postDelayed(animTask, 240);
    }

    private void stopAnim() {
        if (animTask != null) {
            handler.removeCallbacks(animTask);
            animTask = null;
        }
    }
}

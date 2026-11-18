package com.pystarter.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pystarter.app.data.GameStore;
import com.pystarter.app.databinding.ActivitySettingsBinding;
import com.pystarter.app.game.SoundFx;

/**
 * 设置：AI 老师用的 API Key（只保存在这台手机上，不写进代码、不上传），以及每日提醒开关。
 * 不填 Key 也能用：AI 老师会改用每道题自带的三级提示。
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "settings";
    public static final String KEY_API_KEY = "api_key";
    public static final String KEY_BASE_URL = "base_url";
    public static final String KEY_MODEL = "model";
    public static final String KEY_REMINDER = "reminder";
    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL = "deepseek-chat";

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        binding.apiKey.setText(prefs.getString(KEY_API_KEY, ""));
        binding.baseUrl.setText(prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL));
        binding.model.setText(prefs.getString(KEY_MODEL, DEFAULT_MODEL));
        binding.reminder.setChecked(prefs.getBoolean(KEY_REMINDER, true));
        binding.soundFx.setChecked(prefs.getBoolean(SoundFx.KEY_SOUND, true));
        binding.vibrateFx.setChecked(prefs.getBoolean(SoundFx.KEY_VIBRATE, true));
        binding.demoMode.setChecked(GameStore.isDemoMode(this));

        binding.save.setOnClickListener(v -> {
            String baseUrl = binding.baseUrl.getText().toString().trim();
            String model = binding.model.getText().toString().trim();
            prefs.edit()
                    .putString(KEY_API_KEY, binding.apiKey.getText().toString().trim())
                    .putString(KEY_BASE_URL, baseUrl.isEmpty() ? DEFAULT_BASE_URL : baseUrl)
                    .putString(KEY_MODEL, model.isEmpty() ? DEFAULT_MODEL : model)
                    .putBoolean(KEY_REMINDER, binding.reminder.isChecked())
                    .putBoolean(SoundFx.KEY_SOUND, binding.soundFx.isChecked())
                    .putBoolean(SoundFx.KEY_VIBRATE, binding.vibrateFx.isChecked())
                    .apply();
            GameStore.setDemoMode(this, binding.demoMode.isChecked());
            ReminderWorker.schedule(this);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

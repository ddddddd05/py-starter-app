package com.pystarter.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ActivityMainBinding;

/**
 * 首页：学习路线。12 个关卡按顺序排成一列，每关显示完成了几道题。
 * 顶部显示连续学习天数；右上角菜单进入“我的进度”和“设置”。
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private LessonAdapter adapter;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        adapter = new LessonAdapter(this, lesson -> {
            Intent intent = new Intent(this, LessonActivity.class);
            intent.putExtra(LessonActivity.EXTRA_LESSON_ID, lesson.optInt("id"));
            startActivity(intent);
        });
        binding.lessonList.setLayoutManager(new LinearLayoutManager(this));
        binding.lessonList.setAdapter(adapter);

        binding.loading.setVisibility(View.VISIBLE);
        PyBridge.listLessons(lessons -> {
            binding.loading.setVisibility(View.GONE);
            adapter.setLessons(lessons);
        });
        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.refreshProgress();          // 从做题页回来时刷新每关进度
        Store.streak(this, days -> binding.streak.setText(
                days > 0 ? getString(R.string.streak_days, days) : getString(R.string.streak_zero)));
    }

    /** Android 13 起发通知要用户同意；只在第一次打开时问一次。 */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_progress) {
            startActivity(new Intent(this, ProgressActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

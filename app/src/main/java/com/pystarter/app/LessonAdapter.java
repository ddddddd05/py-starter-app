package com.pystarter.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pystarter.app.data.Store;
import com.pystarter.app.databinding.ItemLessonBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 关卡列表的适配器（RecyclerView 的标准写法）：
 * RecyclerView 只创建屏幕上看得见的几行，滚动时复用，列表再长也不卡。
 */
public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.Holder> {

    public interface OnLessonClick {
        void onClick(JSONObject lesson);
    }

    private final Context context;
    private final OnLessonClick listener;
    private JSONArray lessons = new JSONArray();
    private final Map<Integer, Integer> correct = new HashMap<>();

    public LessonAdapter(Context context, OnLessonClick listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setLessons(JSONArray lessons) {
        this.lessons = lessons;
        notifyDataSetChanged();
        refreshProgress();
    }

    /** 从数据库读每关答对了几题，读到一关就刷新一行。 */
    public void refreshProgress() {
        for (int i = 0; i < lessons.length(); i++) {
            int position = i;
            int id = lessons.optJSONObject(i).optInt("id");
            Store.correctCount(context, id, n -> {
                correct.put(id, n);
                notifyItemChanged(position);
            });
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemLessonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        JSONObject l = lessons.optJSONObject(position);
        int id = l.optInt("id");
        int total = l.optInt("questions");
        int done = correct.getOrDefault(id, 0);
        h.b.number.setText(String.valueOf(id));
        h.b.title.setText(l.optString("title"));
        h.b.keyword.setText(l.optString("keyword"));
        h.b.progress.setMax(total);
        h.b.progress.setProgress(done);
        h.b.status.setText(done >= total ? context.getString(R.string.lesson_done)
                : context.getString(R.string.lesson_progress, done, total));
        h.itemView.setOnClickListener(v -> listener.onClick(l));
    }

    @Override
    public int getItemCount() {
        return lessons.length();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemLessonBinding b;

        Holder(ItemLessonBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}

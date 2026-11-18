package com.pystarter.app.strategy;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pystarter.app.R;
import com.pystarter.app.databinding.ActivityBattleBinding;

import org.json.JSONArray;
import org.json.JSONObject;

/** 选择题策略：动态生成像素选项按钮，点击即出招。 */
public class ChoiceQuestionStrategy implements QuestionStrategy {

    @Override
    public void render(ActivityBattleBinding binding, JSONObject question, SubmitCallback callback) {
        binding.attack.setVisibility(View.GONE);
        JSONArray options = question.optJSONArray("options");
        if (options == null) {
            return;
        }
        float density = binding.getRoot().getResources().getDisplayMetrics().density;
        for (int i = 0; i < options.length(); i++) {
            int choice = i;
            TextView b = new TextView(binding.getRoot().getContext(), null, 0, R.style.PxButton);
            b.setText(options.optString(i));
            b.setAllCaps(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * density);
            b.setOnClickListener(v -> callback.onChoiceSelected(choice, String.valueOf(choice)));
            binding.options.addView(b, lp);
        }
    }

    @Override
    public String extractAnswer(ActivityBattleBinding binding) {
        return "";
    }
}

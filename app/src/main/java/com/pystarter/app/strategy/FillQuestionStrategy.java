package com.pystarter.app.strategy;

import android.view.View;

import com.pystarter.app.databinding.ActivityBattleBinding;

import org.json.JSONObject;

/** 填空题策略：展示代码模板与单行输入框。 */
public class FillQuestionStrategy implements QuestionStrategy {

    @Override
    public void render(ActivityBattleBinding binding, JSONObject question, SubmitCallback callback) {
        binding.fillTemplate.setVisibility(View.VISIBLE);
        binding.fillTemplate.setText(question.optString("template"));
        binding.fillInput.setVisibility(View.VISIBLE);
    }

    @Override
    public String extractAnswer(ActivityBattleBinding binding) {
        return binding.fillInput.getText().toString();
    }
}

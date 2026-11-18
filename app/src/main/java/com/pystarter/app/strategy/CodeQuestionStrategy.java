package com.pystarter.app.strategy;

import android.view.View;

import com.pystarter.app.CodeToolbar;
import com.pystarter.app.databinding.ActivityBattleBinding;

import org.json.JSONObject;

/** 编程题策略：展示多行代码编辑器、快捷符号栏、试运行和单步慢放按钮。 */
public class CodeQuestionStrategy implements QuestionStrategy {

    @Override
    public void render(ActivityBattleBinding binding, JSONObject question, SubmitCallback callback) {
        binding.codeEditor.setVisibility(View.VISIBLE);
        CodeToolbar.setVisible(binding.codeEditor, true);
        binding.run.setVisibility(View.VISIBLE);
        binding.slow.setVisibility(View.VISIBLE);
    }

    @Override
    public String extractAnswer(ActivityBattleBinding binding) {
        return binding.codeEditor.getText().toString();
    }
}

package com.pystarter.app.strategy;

import com.pystarter.app.databinding.ActivityBattleBinding;

import org.json.JSONObject;

/**
 * 策略模式接口：把不同题型（选择 / 填空 / 写代码）的界面渲染与答案提取解耦，
 * 避免在 BattleActivity 里堆砌 switch-case。
 */
public interface QuestionStrategy {

    interface SubmitCallback {
        void onChoiceSelected(int choiceIndex, String answer);
    }

    /** 根据题目数据渲染对应题型的输入控件。 */
    void render(ActivityBattleBinding binding, JSONObject question, SubmitCallback callback);

    /** 从当前界面提取玩家输入的答案（选择题通过回调直接提交）。 */
    String extractAnswer(ActivityBattleBinding binding);
}

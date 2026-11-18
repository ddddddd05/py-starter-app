package com.pystarter.app.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 策略工厂：通过 Map 注册题型与策略实例，新增题型只需扩展新类并注册，符合开闭原则。
 */
public final class QuestionStrategyFactory {

    private static final QuestionStrategy DEFAULT_STRATEGY = new CodeQuestionStrategy();
    private static final Map<String, QuestionStrategy> STRATEGIES;

    static {
        Map<String, QuestionStrategy> map = new HashMap<>();
        map.put("choice", new ChoiceQuestionStrategy());
        map.put("fill", new FillQuestionStrategy());
        map.put("code", DEFAULT_STRATEGY);
        STRATEGIES = Collections.unmodifiableMap(map);
    }

    private QuestionStrategyFactory() {}

    public static QuestionStrategy get(String type) {
        if (type == null) {
            return DEFAULT_STRATEGY;
        }
        return STRATEGIES.getOrDefault(type, DEFAULT_STRATEGY);
    }
}

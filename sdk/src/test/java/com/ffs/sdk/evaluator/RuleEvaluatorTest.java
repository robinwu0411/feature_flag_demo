package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {
    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() { evaluator = new RuleEvaluator(); }

    @Test
    void shouldReturnDefaultWhenDisabled() {
        FlagConfig f = flag("test", "false", false);
        assertThat(evaluator.evaluate(f, user("u1")).getValue()).isEqualTo("false");
        assertThat(evaluator.evaluate(f, user("u1")).getReason()).isEqualTo("disabled");
    }

    @Test
    void shouldMatchEqualsRule() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getValue()).isEqualTo("true");
        assertThat(r.getReason()).isEqualTo("rule_match");
    }

    @Test
    void shouldRespectPriority() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(
                new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true"),
                new TargetingRule(2, "region", "EQUALS", "\"eu-west\"", "false")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getValue()).isEqualTo("true");
    }

    @Test
    void shouldHandle100PercentRollout() {
        FlagConfig f = flag("test", "false", true);
        FlagConfig.Rollout ro = new FlagConfig.Rollout(); ro.setPercentage(100); ro.setServeValue("true");
        f.setRollout(ro);
        assertThat(evaluator.evaluate(f, user("u1")).getReason()).isEqualTo("rollout");
    }

    @Test
    void shouldMatchCustomAttribute() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "membership_level", "EQUALS", "\"vip\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").custom("membership_level", "vip").build());
        assertThat(r.getValue()).isEqualTo("true");
    }

    @Test
    void shouldProduceTrace() {
        FlagConfig f = flag("test", "false", true);
        f.setRules(List.of(new TargetingRule(1, "region", "EQUALS", "\"eu-west\"", "true")));
        EvalResult r = evaluator.evaluate(f, FFUser.builder().id("u1").region("eu-west").build());
        assertThat(r.getTrace()).isNotEmpty();
        assertThat(r.getTrace().get(0).isMatched()).isTrue();
    }

    private FlagConfig flag(String key, String defVal, boolean enabled) {
        FlagConfig f = new FlagConfig();
        f.setKey(key); f.setType("BOOLEAN"); f.setDefaultValue(defVal); f.setEnabled(enabled);
        return f;
    }

    private FFUser user(String id) { return FFUser.builder().id(id).build(); }
}

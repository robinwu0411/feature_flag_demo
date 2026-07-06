package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import com.ffs.sdk.model.FlagConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {
    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() { evaluator = new RuleEvaluator(); }

    @Test
    void shouldReturnDefaultWhenDisabled() {
        FlagConfig f = flag("test", "BOOLEAN", "false", false);
        EvalResult r = evaluator.evaluate(f, user("u1"));
        assertThat(r.getValue()).isEqualTo("false");
        assertThat(r.getReason()).isEqualTo("disabled");
    }

    @Test
    void shouldReturnTrueWhenBooleanEnabled() {
        FlagConfig f = flag("test", "BOOLEAN", "false", true);
        EvalResult r = evaluator.evaluate(f, user("u1"));
        assertThat(r.getValue()).isEqualTo("true");
        assertThat(r.getReason()).isEqualTo("enabled");
    }

    @Test
    void shouldReturnDefaultValueWhenNonBooleanEnabled() {
        FlagConfig f = flag("max_results", "NUMBER", "20", true);
        EvalResult r = evaluator.evaluate(f, user("u1"));
        assertThat(r.getValue()).isEqualTo("20");
        assertThat(r.getReason()).isEqualTo("enabled");
    }

    @Test
    void shouldIncludeReleaseVersion() {
        FlagConfig f = flag("test", "BOOLEAN", "false", true);
        f.setReleaseVersion("v2.0.0");
        EvalResult r = evaluator.evaluate(f, user("u1"));
        assertThat(r.getReleaseVersion()).isEqualTo("v2.0.0");
    }

    @Test
    void shouldProduceTrace() {
        FlagConfig f = flag("test", "BOOLEAN", "false", true);
        EvalResult r = evaluator.evaluate(f, user("u1"));
        assertThat(r.getTrace()).isNotEmpty();
        assertThat(r.getTrace().get(0).getRuleId()).isEqualTo("enabled_check");
    }

    private FlagConfig flag(String key, String type, String defVal, boolean enabled) {
        FlagConfig f = new FlagConfig();
        f.setKey(key);
        f.setType(type);
        f.setDefaultValue(defVal);
        f.setEnabled(enabled);
        return f;
    }

    private FFUser user(String id) { return FFUser.builder().id(id).build(); }
}

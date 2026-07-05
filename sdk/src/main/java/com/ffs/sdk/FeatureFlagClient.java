package com.ffs.sdk;

import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.config.ClientConfig;
import com.ffs.sdk.evaluator.RuleEvaluator;
import com.ffs.sdk.model.EvalResult;
import com.ffs.sdk.model.FFUser;
import com.ffs.sdk.model.FlagConfig;
import com.ffs.sdk.sync.ConfigPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureFlagClient {
    private static final Logger log = LoggerFactory.getLogger(FeatureFlagClient.class);
    private final FlagCache cache;
    private final ConfigPoller poller;
    private final RuleEvaluator evaluator = new RuleEvaluator();

    FeatureFlagClient(ClientConfig config, FlagCache cache, ConfigPoller poller) {
        this.cache = cache; this.poller = poller;
    }

    public static FeatureFlagClientBuilder builder() { return new FeatureFlagClientBuilder(); }
    public void start() { poller.start(); }
    public void close() { poller.stop(); }

    public boolean isEnabled(String flagKey, FFUser user) {
        return "true".equalsIgnoreCase(evaluate(flagKey, user, "false"));
    }

    public String stringValue(String flagKey, FFUser user, String defaultValue) {
        return evaluate(flagKey, user, defaultValue);
    }

    public int intValue(String flagKey, FFUser user, int defaultValue) {
        try { return Integer.parseInt(evaluate(flagKey, user, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public Map<String, EvalResult> evaluateAll(FFUser user, String... flagKeys) {
        Map<String, EvalResult> results = new LinkedHashMap<>();
        for (String key : flagKeys) {
            FlagConfig flag = cache.get(key);
            results.put(key, flag != null ? evaluator.evaluate(flag, user) : new EvalResult(key, "false", "not_found"));
        }
        return results;
    }

    private String evaluate(String flagKey, FFUser user, String defaultValue) {
        FlagConfig flag = cache.get(flagKey);
        if (flag == null) return defaultValue;
        return evaluator.evaluate(flag, user).getValue();
    }
}

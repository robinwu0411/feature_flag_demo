package com.ffs.sdk.evaluator;

import com.ffs.sdk.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class RuleEvaluator {

    public EvalResult evaluate(FlagConfig flag, FFUser user) {
        if (!flag.isEnabled()) {
            return buildResult(flag, flag.getDefaultValue(), "disabled", null);
        }
        if (flag.getRules() != null) {
            for (TargetingRule rule : flag.getRules()) {
                if (evaluateRule(rule, user)) {
                    return buildResult(flag, rule.getServeValue(), "rule_match",
                            String.valueOf(rule.getPriority()));
                }
            }
        }
        if (flag.getRollout() != null && flag.getRollout().getPercentage() > 0) {
            if (isInRollout(user.getId(), flag.getKey(), flag.getRollout().getPercentage())) {
                return buildResult(flag, flag.getRollout().getServeValue(), "rollout", null);
            }
        }
        return buildResult(flag, flag.getDefaultValue(), "default", null);
    }

    private boolean evaluateRule(TargetingRule rule, FFUser user) {
        String userValue = user.getAttribute(rule.getAttribute());
        String ruleValue = stripQuotes(rule.getValue());
        switch (rule.getOperator().toUpperCase()) {
            case "EQUALS": return ruleValue.equals(userValue);
            case "IN":
                List<String> values = parseList(rule.getValue());
                return userValue != null && values.contains(userValue);
            case "CONTAINS":
                return userValue != null && Arrays.asList(userValue.split(",")).contains(ruleValue);
            case "GREATER_THAN":
                return userValue != null && safeCompare(userValue, ruleValue) > 0;
            case "LESS_THAN":
                return userValue != null && safeCompare(userValue, ruleValue) < 0;
            default: return false;
        }
    }

    private int safeCompare(String a, String b) {
        try { return Double.compare(Double.parseDouble(a), Double.parseDouble(b)); }
        catch (NumberFormatException e) { return 0; }
    }

    private boolean isInRollout(String userId, String flagKey, int percentage) {
        if (userId == null) return false;
        int hash = Math.abs(murmurHash((flagKey + ":" + userId).getBytes(StandardCharsets.UTF_8)));
        return (hash % 100) < percentage;
    }

    private int murmurHash(byte[] data) {
        int h = 0x9747b28c;
        for (byte b : data) { h ^= (b & 0xFF); h *= 0x5bd1e995; h ^= h >>> 15; }
        return h;
    }

    private String stripQuotes(String v) {
        if (v == null) return null;
        String t = v.trim();
        return (t.startsWith("\"") && t.endsWith("\"")) ? t.substring(1, t.length() - 1) : t;
    }

    private List<String> parseList(String v) {
        String t = v.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            return Arrays.stream(t.substring(1, t.length() - 1).split(","))
                    .map(String::trim).map(this::stripQuotes).toList();
        }
        return List.of(stripQuotes(v));
    }

    private EvalResult buildResult(FlagConfig flag, String value, String reason, String matchedRuleId) {
        EvalResult r = new EvalResult(flag.getKey(), value, reason);
        r.setMatchedRuleId(matchedRuleId);
        r.setReleaseVersion(flag.getReleaseVersion());
        r.setFlagUpdatedAt(flag.getUpdatedAt());
        if (flag.getRules() != null) {
            for (TargetingRule rule : flag.getRules()) {
                r.addTrace(new TraceEntry(String.valueOf(rule.getPriority()),
                        rule.getAttribute() + " " + rule.getOperator() + " " + rule.getValue(),
                        String.valueOf(rule.getPriority()).equals(matchedRuleId)));
            }
        }
        if (flag.getRollout() != null) {
            r.addTrace(new TraceEntry("rollout", "rollout " + flag.getRollout().getPercentage() + "%",
                    "rollout".equals(reason)));
        }
        r.addTrace(new TraceEntry("default", "default: " + flag.getDefaultValue(), "default".equals(reason)));
        return r;
    }
}

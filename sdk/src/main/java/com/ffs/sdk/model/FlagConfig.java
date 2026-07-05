package com.ffs.sdk.model;

import java.util.List;

public class FlagConfig {
    private String key;
    private String type;
    private String defaultValue;
    private boolean enabled;
    private String releaseVersion;
    private String updatedAt;
    private List<TargetingRule> rules;
    private Rollout rollout;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<TargetingRule> getRules() { return rules; }
    public void setRules(List<TargetingRule> rules) { this.rules = rules; }
    public Rollout getRollout() { return rollout; }
    public void setRollout(Rollout rollout) { this.rollout = rollout; }

    public static class Rollout {
        private int percentage;
        private String serveValue;
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        public String getServeValue() { return serveValue; }
        public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    }
}

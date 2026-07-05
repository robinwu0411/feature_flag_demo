package com.ffs.sdk.model;

public class TraceEntry {
    private String ruleId;
    private String condition;
    private boolean matched;

    public TraceEntry() {}
    public TraceEntry(String ruleId, String condition, boolean matched) {
        this.ruleId = ruleId; this.condition = condition; this.matched = matched;
    }
    public String getRuleId() { return ruleId; }
    public String getCondition() { return condition; }
    public boolean isMatched() { return matched; }
}

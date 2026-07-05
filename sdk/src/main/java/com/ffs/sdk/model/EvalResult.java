package com.ffs.sdk.model;

import java.util.ArrayList;
import java.util.List;

public class EvalResult {
    private String flagKey;
    private String value;
    private String reason;
    private String matchedRuleId;
    private List<TraceEntry> trace = new ArrayList<>();
    private String releaseVersion;
    private String flagUpdatedAt;

    public EvalResult() {}
    public EvalResult(String flagKey, String value, String reason) {
        this.flagKey = flagKey; this.value = value; this.reason = reason;
    }

    public String getFlagKey() { return flagKey; }
    public void setFlagKey(String flagKey) { this.flagKey = flagKey; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMatchedRuleId() { return matchedRuleId; }
    public void setMatchedRuleId(String matchedRuleId) { this.matchedRuleId = matchedRuleId; }
    public List<TraceEntry> getTrace() { return trace; }
    public void setTrace(List<TraceEntry> trace) { this.trace = trace; }
    public void addTrace(TraceEntry entry) { this.trace.add(entry); }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getFlagUpdatedAt() { return flagUpdatedAt; }
    public void setFlagUpdatedAt(String flagUpdatedAt) { this.flagUpdatedAt = flagUpdatedAt; }
}

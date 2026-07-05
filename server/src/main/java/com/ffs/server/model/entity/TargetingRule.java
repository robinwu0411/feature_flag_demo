package com.ffs.server.model.entity;

import java.time.Instant;

public class TargetingRule {
    private Long id;
    private Long flagId;
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;
    private Boolean enabled = true;
    private Instant createdAt;

    public TargetingRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

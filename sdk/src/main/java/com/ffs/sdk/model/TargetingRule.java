package com.ffs.sdk.model;

public class TargetingRule {
    private String id;
    private int priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;

    public TargetingRule() {}
    public TargetingRule(int priority, String attribute, String operator, String value, String serveValue) {
        this.priority = priority; this.attribute = attribute; this.operator = operator;
        this.value = value; this.serveValue = serveValue;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getAttribute() { return attribute; }
    public void setAttribute(String attribute) { this.attribute = attribute; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
}

package com.ffs.server.model.dto;

public class CreateRuleRequest {
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;
    private Boolean enabled = true;

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
}

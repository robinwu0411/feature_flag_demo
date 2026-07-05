package com.ffs.server.model.dto;

import com.ffs.server.model.entity.TargetingRule;

public class RuleDto {
    private Long id;
    private Integer priority;
    private String attribute;
    private String operator;
    private String value;
    private String serveValue;

    public static RuleDto from(TargetingRule r) {
        RuleDto d = new RuleDto();
        d.id = r.getId();
        d.priority = r.getPriority();
        d.attribute = r.getAttribute();
        d.operator = r.getOperator();
        d.value = r.getValue();
        d.serveValue = r.getServeValue();
        return d;
    }

    public Long getId() { return id; }
    public Integer getPriority() { return priority; }
    public String getAttribute() { return attribute; }
    public String getOperator() { return operator; }
    public String getValue() { return value; }
    public String getServeValue() { return serveValue; }
}

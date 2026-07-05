package com.ffs.server.model.entity;

import java.time.Instant;

public class Rollout {
    private Long id;
    private Long flagId;
    private Integer percentage;
    private String serveValue;
    private Boolean enabled = true;
    private Instant createdAt;

    public Rollout() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public Integer getPercentage() { return percentage; }
    public void setPercentage(Integer percentage) { this.percentage = percentage; }
    public String getServeValue() { return serveValue; }
    public void setServeValue(String serveValue) { this.serveValue = serveValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

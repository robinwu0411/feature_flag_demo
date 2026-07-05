package com.ffs.server.model.entity;

import java.time.Instant;

public class AuditLog {
    private Long id;
    private Long flagId;
    private String action;
    private String changedBy;
    private String detail;
    private Instant createdAt;

    public AuditLog() {}

    public AuditLog(Long flagId, String action, String changedBy, String detail) {
        this.flagId = flagId;
        this.action = action;
        this.changedBy = changedBy;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFlagId() { return flagId; }
    public void setFlagId(Long flagId) { this.flagId = flagId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

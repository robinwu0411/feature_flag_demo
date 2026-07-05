package com.ffs.server.model.entity;

import java.time.Instant;

public class Flag {
    private Long id;
    private String key;
    private String name;
    private String description;
    private String flagType = "BOOLEAN";
    private String defaultValue = "false";
    private Boolean enabled = false;
    private String releaseVersion;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public Flag() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFlagType() { return flagType; }
    public void setFlagType(String flagType) { this.flagType = flagType; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String releaseVersion) { this.releaseVersion = releaseVersion; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

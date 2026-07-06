package com.ffs.server.model.dto;

import com.ffs.server.model.entity.Flag;

import java.time.Instant;

public class FlagDto {
    private String key;
    private String type;
    private String defaultValue;
    private Boolean enabled;
    private String releaseVersion;
    private Instant updatedAt;

    public static FlagDto from(Flag flag) {
        FlagDto d = new FlagDto();
        d.key = flag.getKey();
        d.type = flag.getFlagType();
        d.defaultValue = flag.getDefaultValue();
        d.enabled = flag.getEnabled();
        d.releaseVersion = flag.getReleaseVersion();
        d.updatedAt = flag.getUpdatedAt();
        return d;
    }

    public String getKey() { return key; }
    public String getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
}

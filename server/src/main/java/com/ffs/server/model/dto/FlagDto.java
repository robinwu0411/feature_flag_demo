package com.ffs.server.model.dto;

import com.ffs.server.model.entity.Flag;
import com.ffs.server.model.entity.Rollout;
import com.ffs.server.model.entity.TargetingRule;

import java.time.Instant;
import java.util.List;

public class FlagDto {
    private String key;
    private String type;
    private String defaultValue;
    private Boolean enabled;
    private String releaseVersion;
    private Instant updatedAt;
    private List<RuleDto> rules;
    private RolloutDto rollout;

    public static FlagDto from(Flag flag, List<TargetingRule> rules, List<Rollout> rollouts) {
        FlagDto d = new FlagDto();
        d.key = flag.getKey();
        d.type = flag.getFlagType();
        d.defaultValue = flag.getDefaultValue();
        d.enabled = flag.getEnabled();
        d.releaseVersion = flag.getReleaseVersion();
        d.updatedAt = flag.getUpdatedAt();
        d.rules = rules.stream().map(RuleDto::from).toList();
        if (!rollouts.isEmpty()) {
            d.rollout = RolloutDto.from(rollouts.get(0));
        }
        return d;
    }

    public String getKey() { return key; }
    public String getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public Boolean getEnabled() { return enabled; }
    public String getReleaseVersion() { return releaseVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<RuleDto> getRules() { return rules; }
    public RolloutDto getRollout() { return rollout; }
}

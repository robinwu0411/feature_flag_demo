package com.ffs.server.service;

import com.ffs.server.mapper.FlagMapper;
import com.ffs.server.mapper.RolloutMapper;
import com.ffs.server.mapper.TargetingRuleMapper;
import com.ffs.server.model.dto.FlagDto;
import com.ffs.server.model.dto.SyncResponse;
import com.ffs.server.model.entity.Application;
import com.ffs.server.model.entity.Flag;
import com.ffs.server.model.entity.Rollout;
import com.ffs.server.model.entity.TargetingRule;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {
    private final FlagMapper flagMapper;
    private final TargetingRuleMapper ruleMapper;
    private final RolloutMapper rolloutMapper;
    private final ApplicationService applicationService;

    public SyncService(FlagMapper flagMapper,
                       TargetingRuleMapper ruleMapper,
                       RolloutMapper rolloutMapper,
                       ApplicationService applicationService) {
        this.flagMapper = flagMapper;
        this.ruleMapper = ruleMapper;
        this.rolloutMapper = rolloutMapper;
        this.applicationService = applicationService;
    }

    public SyncResponse sync(String appName, String sinceParam) {
        Application app = applicationService.getByName(appName);
        Long appId = app.getId();

        List<Flag> flags;
        if (sinceParam != null && !sinceParam.isEmpty()) {
            Instant since = Instant.parse(sinceParam);
            flags = flagMapper.findByApplicationIdAndUpdatedAfter(appId, since);
        } else {
            flags = flagMapper.findByApplicationId(appId);
        }

        List<FlagDto> dtos = flags.stream()
                .filter(Flag::getEnabled)
                .map(flag -> {
                    List<TargetingRule> rules = ruleMapper.findByFlagIdOrderByPriority(flag.getId());
                    List<Rollout> rollouts = rolloutMapper.findByFlagId(flag.getId());
                    return FlagDto.from(flag, rules, rollouts);
                })
                .toList();

        return new SyncResponse(dtos);
    }
}

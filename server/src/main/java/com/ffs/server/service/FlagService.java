package com.ffs.server.service;

import com.ffs.server.mapper.*;
import com.ffs.server.model.dto.*;
import com.ffs.server.model.entity.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlagService {
    private final FlagMapper flagMapper;
    private final TargetingRuleMapper ruleMapper;
    private final RolloutMapper rolloutMapper;
    private final ApplicationService applicationService;
    private final AuditService auditService;

    public FlagService(FlagMapper flagMapper,
                       TargetingRuleMapper ruleMapper,
                       RolloutMapper rolloutMapper,
                       ApplicationService applicationService,
                       AuditService auditService) {
        this.flagMapper = flagMapper;
        this.ruleMapper = ruleMapper;
        this.rolloutMapper = rolloutMapper;
        this.applicationService = applicationService;
        this.auditService = auditService;
    }

    @CacheEvict(value = "flags", key = "#request.key")
    public Flag create(CreateFlagRequest request) {
        Flag flag = new Flag();
        flag.setKey(request.getKey());
        flag.setName(request.getName());
        flag.setDescription(request.getDescription());
        flag.setFlagType(request.getFlagType());
        flag.setDefaultValue(request.getDefaultValue());
        flag.setEnabled(request.getEnabled() != null ? request.getEnabled() : false);
        flag.setReleaseVersion(request.getReleaseVersion());
        flag.setCreatedBy(request.getCreatedBy());
        flagMapper.insert(flag);
        auditService.log(flag.getId(), "CREATED", request.getCreatedBy(),
                "{\"key\":\"" + request.getKey() + "\"}");
        return flag;
    }

    public List<Flag> listAll() {
        return flagMapper.findAll();
    }

    public Flag getById(Long id) {
        Flag flag = flagMapper.findById(id);
        if (flag == null) throw new RuntimeException("Flag not found: " + id);
        return flag;
    }

    public Flag getByKey(String key) {
        Flag flag = flagMapper.findByKey(key);
        if (flag == null) throw new RuntimeException("Flag not found: " + key);
        return flag;
    }

    @CacheEvict(value = "flags", key = "#result.key")
    public Flag update(Long id, CreateFlagRequest request) {
        Flag flag = getById(id);
        if (request.getName() != null) flag.setName(request.getName());
        if (request.getDescription() != null) flag.setDescription(request.getDescription());
        if (request.getDefaultValue() != null) flag.setDefaultValue(request.getDefaultValue());
        if (request.getEnabled() != null) flag.setEnabled(request.getEnabled());
        if (request.getReleaseVersion() != null) flag.setReleaseVersion(request.getReleaseVersion());
        flagMapper.update(flag);
        auditService.log(flag.getId(), "UPDATED", request.getCreatedBy(), "{}");
        return getById(id);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void delete(Long id) {
        Flag flag = getById(id);
        flagMapper.deleteById(id);
        auditService.log(id, "DELETED", "system", "{\"key\":\"" + flag.getKey() + "\"}");
    }

    @CacheEvict(value = "flags", allEntries = true)
    public TargetingRule addRule(Long flagId, CreateRuleRequest request) {
        TargetingRule rule = new TargetingRule();
        rule.setFlagId(flagId);
        rule.setPriority(request.getPriority());
        rule.setAttribute(request.getAttribute());
        rule.setOperator(request.getOperator());
        rule.setValue(request.getValue());
        rule.setServeValue(request.getServeValue());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        ruleMapper.insert(rule);
        auditService.log(flagId, "RULE_ADDED", "admin",
                "{\"attribute\":\"" + request.getAttribute() + "\"}");
        return rule;
    }

    public List<TargetingRule> getRules(Long flagId) {
        return ruleMapper.findByFlagIdOrderByPriority(flagId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public TargetingRule updateRule(Long flagId, Long ruleId, CreateRuleRequest request) {
        TargetingRule rule = ruleMapper.findById(ruleId);
        if (rule == null) throw new RuntimeException("Rule not found: " + ruleId);
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getAttribute() != null) rule.setAttribute(request.getAttribute());
        if (request.getOperator() != null) rule.setOperator(request.getOperator());
        if (request.getValue() != null) rule.setValue(request.getValue());
        if (request.getServeValue() != null) rule.setServeValue(request.getServeValue());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
        ruleMapper.update(rule);
        return rule;
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void deleteRule(Long flagId, Long ruleId) {
        ruleMapper.deleteById(ruleId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public Rollout setRollout(Long flagId, CreateRolloutRequest request) {
        rolloutMapper.deactivateByFlagId(flagId);
        Rollout rollout = new Rollout();
        rollout.setFlagId(flagId);
        rollout.setPercentage(request.getPercentage());
        rollout.setServeValue(request.getServeValue());
        rollout.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        rolloutMapper.insert(rollout);
        auditService.log(flagId, "ROLLOUT_SET", "admin",
                "{\"percentage\":" + request.getPercentage() + "}");
        return rollout;
    }

    public List<Rollout> getRollouts(Long flagId) {
        return rolloutMapper.findByFlagId(flagId);
    }

    @CacheEvict(value = "flags", allEntries = true)
    public void bindApplication(Long flagId, Long applicationId) {
        flagMapper.insertFlagApplication(flagId, applicationId);
    }
}

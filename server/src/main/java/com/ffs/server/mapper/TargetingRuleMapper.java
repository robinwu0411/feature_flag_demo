package com.ffs.server.mapper;

import com.ffs.server.model.entity.TargetingRule;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TargetingRuleMapper {
    List<TargetingRule> findByFlagIdOrderByPriority(Long flagId);
    TargetingRule findById(Long id);
    int insert(TargetingRule rule);
    int update(TargetingRule rule);
    int deleteById(Long id);
    int deactivateByFlagId(Long flagId);
}

package com.ffs.server.mapper;

import com.ffs.server.model.entity.Rollout;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RolloutMapper {
    List<Rollout> findByFlagId(Long flagId);
    int insert(Rollout rollout);
    int deactivateByFlagId(Long flagId);
}

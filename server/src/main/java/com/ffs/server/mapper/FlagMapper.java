package com.ffs.server.mapper;

import com.ffs.server.model.entity.Flag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface FlagMapper {
    Flag findById(Long id);
    Flag findByKey(String key);
    List<Flag> findAll();
    List<Flag> findByApplicationId(@Param("appId") Long appId);
    List<Flag> findByApplicationIdAndUpdatedAfter(@Param("appId") Long appId, @Param("since") Instant since);
    int insert(Flag flag);
    int update(Flag flag);
    int deleteById(Long id);
    int insertFlagApplication(@Param("flagId") Long flagId, @Param("applicationId") Long appId);
    List<Long> findApplicationIdsByFlagId(@Param("flagId") Long flagId);
}

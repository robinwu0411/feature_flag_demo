package com.ffs.server.mapper;

import com.ffs.server.model.entity.Flag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlagMapper {
    Flag findById(Long id);
    Flag findByKey(String key);
    List<Flag> findAll();
    List<Flag> findAllEnabled();
    List<Flag> findEnabledByAppName(@Param("appName") String appName);
    int insert(Flag flag);
    int update(Flag flag);
    int deleteById(Long id);
}

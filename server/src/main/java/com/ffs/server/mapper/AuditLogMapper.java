package com.ffs.server.mapper;

import com.ffs.server.model.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AuditLogMapper {
    int insert(AuditLog log);
    List<AuditLog> findByFlagIdOrderByCreatedAtDesc(Long flagId);
}

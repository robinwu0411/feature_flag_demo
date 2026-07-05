package com.ffs.server.service;

import com.ffs.server.mapper.AuditLogMapper;
import com.ffs.server.model.entity.AuditLog;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {
    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void log(Long flagId, String action, String changedBy, String detail) {
        auditLogMapper.insert(new AuditLog(flagId, action, changedBy, detail));
    }

    public List<AuditLog> getAuditLogs(Long flagId) {
        return auditLogMapper.findByFlagIdOrderByCreatedAtDesc(flagId);
    }
}

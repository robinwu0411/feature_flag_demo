package com.ffs.server.service;

import com.ffs.server.mapper.FlagMapper;
import com.ffs.server.model.dto.CreateFlagRequest;
import com.ffs.server.model.entity.Flag;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlagService {
    private final FlagMapper flagMapper;
    private final FlagCacheService cacheService;

    public FlagService(FlagMapper flagMapper, FlagCacheService cacheService) {
        this.flagMapper = flagMapper;
        this.cacheService = cacheService;
    }

    public Flag create(CreateFlagRequest request) {
        Flag flag = new Flag();
        flag.setKey(request.getKey());
        flag.setName(request.getName());
        flag.setDescription(request.getDescription());
        flag.setFlagType(request.getFlagType());
        flag.setDefaultValue(request.getDefaultValue());
        flag.setEnabled(request.getEnabled() != null ? request.getEnabled() : false);
        flag.setReleaseVersion(request.getReleaseVersion());
        flag.setAppName(request.getAppName());
        flag.setCreatedBy(request.getCreatedBy());
        flagMapper.insert(flag);
        cacheService.evict(request.getAppName());
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

    public Flag update(Long id, CreateFlagRequest request) {
        Flag flag = getById(id);
        if (request.getName() != null) flag.setName(request.getName());
        if (request.getDescription() != null) flag.setDescription(request.getDescription());
        if (request.getDefaultValue() != null) flag.setDefaultValue(request.getDefaultValue());
        if (request.getEnabled() != null) flag.setEnabled(request.getEnabled());
        if (request.getReleaseVersion() != null) flag.setReleaseVersion(request.getReleaseVersion());
        flagMapper.update(flag);
        cacheService.evict(flag.getAppName());
        return getById(id);
    }

    public void delete(Long id) {
        Flag flag = getById(id);
        flagMapper.deleteById(id);
        cacheService.evict(flag.getAppName());
    }
}

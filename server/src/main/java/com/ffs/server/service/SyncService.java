package com.ffs.server.service;

import com.ffs.server.model.dto.FlagDto;
import com.ffs.server.model.dto.SyncResponse;
import com.ffs.server.model.entity.Flag;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SyncService {
    private final FlagCacheService flagCacheService;

    public SyncService(FlagCacheService flagCacheService) {
        this.flagCacheService = flagCacheService;
    }

    public SyncResponse sync(String appName, String since) {
        List<Flag> flags = flagCacheService.getFlagsByApp(appName);

        if (since != null && !since.isEmpty()) {
            Instant sinceTime = Instant.parse(since);
            flags = flags.stream()
                    .filter(f -> f.getUpdatedAt() != null && f.getUpdatedAt().isAfter(sinceTime))
                    .toList();
        }

        return new SyncResponse(flags.stream().map(FlagDto::from).toList());
    }
}

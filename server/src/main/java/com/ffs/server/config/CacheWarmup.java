package com.ffs.server.config;

import com.ffs.server.service.FlagCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmup implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(CacheWarmup.class);
    private final FlagCacheService cacheService;

    public CacheWarmup(FlagCacheService cacheService) {
        this.cacheService = cacheService;
        log.info("CacheWarmup bean created");
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting cache warmup...");
            cacheService.warmupAll();
            log.info("Cache warmup complete");
        } catch (Exception e) {
            log.error("Cache warmup failed: {}", e.getMessage(), e);
        }
    }
}

package com.ffs.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffs.server.mapper.FlagMapper;
import com.ffs.server.model.entity.Flag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlagCacheService {
    private static final Logger log = LoggerFactory.getLogger(FlagCacheService.class);
    private static final String CACHE_PREFIX = "flags::";
    private static final Duration TTL = Duration.ofHours(24);
    private static final TypeReference<List<Flag>> LIST_TYPE = new TypeReference<>() {};

    private final FlagMapper flagMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public FlagCacheService(FlagMapper flagMapper,
                            RedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper) {
        this.flagMapper = flagMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Flag> getFlagsByApp(String appName) {
        String key = CACHE_PREFIX + appName;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                return objectMapper.readValue(json, LIST_TYPE);
            } catch (Exception e) {
                log.warn("Failed to deserialize cache for app={}, falling back to MySQL", appName, e);
            }
        }
        List<Flag> flags = flagMapper.findEnabledByAppName(appName);
        if (!flags.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(flags), TTL);
            } catch (Exception e) {
                log.warn("Failed to cache flags for app={}", appName, e);
            }
        }
        return flags;
    }

    public void evict(String appName) {
        redisTemplate.delete(CACHE_PREFIX + appName);
    }

    public void warmupAll() {
        List<Flag> all = flagMapper.findAllEnabled();
        Map<String, List<Flag>> grouped = all.stream()
                .collect(Collectors.groupingBy(Flag::getAppName));
        for (var entry : grouped.entrySet()) {
            try {
                String key = CACHE_PREFIX + entry.getKey();
                String json = objectMapper.writeValueAsString(entry.getValue());
                redisTemplate.opsForValue().set(key, json, TTL);
                log.info("Cache warmup: app={}, flags={}", entry.getKey(), entry.getValue().size());
            } catch (Exception e) {
                log.warn("Cache warmup failed for app={}", entry.getKey(), e);
            }
        }
    }
}

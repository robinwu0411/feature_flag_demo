package com.ffs.server.controller;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            status.put("database", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            status.put("database", "DOWN: " + e.getMessage());
        }
        try {
            redisConnectionFactory.getConnection().ping();
            status.put("redis", "UP");
        } catch (Exception e) {
            status.put("redis", "DOWN: " + e.getMessage());
        }
        boolean allUp = status.values().stream().allMatch(v -> v.equals("UP"));
        return ResponseEntity.status(allUp ? 200 : 503).body(status);
    }
}

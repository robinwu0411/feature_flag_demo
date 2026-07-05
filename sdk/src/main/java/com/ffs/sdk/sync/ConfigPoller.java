package com.ffs.sdk.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.model.FlagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigPoller {
    private static final Logger log = LoggerFactory.getLogger(ConfigPoller.class);
    private final String serverUrl;
    private final String appId;
    private final long intervalMs;
    private final FlagCache cache;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ScheduledExecutorService scheduler;
    private Instant lastSyncTimestamp;
    private volatile boolean running = false;

    public ConfigPoller(String serverUrl, String appId, long intervalMs, FlagCache cache) {
        this.serverUrl = serverUrl; this.appId = appId; this.intervalMs = intervalMs; this.cache = cache;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        syncFull();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ff-sdk-poller"); t.setDaemon(true); return t;
        });
        scheduler.scheduleWithFixedDelay(this::syncIncremental, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdown();
    }

    private void syncFull() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/eval/sync?appId=" + appId))
                    .GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                cache.putAll(parseFlags(resp.body()));
                lastSyncTimestamp = Instant.now();
                log.info("SDK initial sync: {} flags", cache.size());
            }
        } catch (Exception e) { log.warn("SDK initial sync failed: {}", e.getMessage()); }
    }

    private void syncIncremental() {
        try {
            String url = serverUrl + "/api/v1/eval/sync?appId=" + appId;
            if (lastSyncTimestamp != null) url += "&since=" + lastSyncTimestamp.toString();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Map<String, FlagConfig> updated = parseFlags(resp.body());
                if (!updated.isEmpty()) { cache.merge(updated); log.debug("SDK sync updated {} flags", updated.size()); }
                lastSyncTimestamp = Instant.now();
            }
        } catch (Exception e) { log.warn("SDK sync error: {}", e.getMessage()); }
    }

    private Map<String, FlagConfig> parseFlags(String body) {
        Map<String, FlagConfig> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode arr = root.get("flags");
            if (arr != null && arr.isArray())
                for (JsonNode n : arr) {
                    FlagConfig f = objectMapper.treeToValue(n, FlagConfig.class);
                    result.put(f.getKey(), f);
                }
        } catch (Exception e) { log.warn("SDK parse error: {}", e.getMessage()); }
        return result;
    }
}

package com.ffs.sdk.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.config.ClientConfig;
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
    private final int maxRetries;
    private final long retryBaseDelayMs;
    private final long retryMaxDelayMs;
    private final FlagCache cache;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private ScheduledExecutorService scheduler;
    private Instant lastSyncTimestamp;
    private volatile boolean running = false;

    public ConfigPoller(ClientConfig config, FlagCache cache) {
        this.serverUrl = config.getServerUrl();
        this.appId = config.getAppId();
        this.intervalMs = config.getSyncIntervalMs();
        this.maxRetries = config.getMaxRetries();
        this.retryBaseDelayMs = config.getRetryBaseDelayMs();
        this.retryMaxDelayMs = config.getRetryMaxDelayMs();
        this.cache = cache;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        syncWithRetry(null, maxRetries);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ff-sdk-poller"); t.setDaemon(true); return t;
        });
        scheduler.scheduleWithFixedDelay(() -> syncWithRetry(lastSyncTimestamp, 1),
                intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdown();
    }

    private void syncWithRetry(Instant since, int retries) {
        int attempt = 0;
        long delay = retryBaseDelayMs;
        while (attempt <= retries) {
            attempt++;
            try {
                doSync(since);
                return;
            } catch (Exception e) {
                if (attempt > retries) {
                    log.warn("SDK sync failed after {} attempts: {}", attempt, e.getMessage());
                    return;
                }
                long wait = Math.min(delay, retryMaxDelayMs);
                log.debug("SDK sync attempt {}/{} failed, retrying in {}ms", attempt, retries, wait);
                try { Thread.sleep(wait); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                delay *= 2;
            }
        }
    }

    private void doSync(Instant since) throws Exception {
        String url = serverUrl + "/api/v1/eval/sync?appId=" + appId;
        if (since != null) url += "&since=" + since.toString();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url)).GET().timeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            Map<String, FlagConfig> parsed = parseFlags(resp.body());
            if (since == null) {
                cache.putAll(parsed);
                log.info("SDK initial sync: {} flags loaded", cache.size());
            } else if (!parsed.isEmpty()) {
                cache.merge(parsed);
                log.debug("SDK sync updated {} flags", parsed.size());
            }
            lastSyncTimestamp = Instant.now();
        }
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

package com.ffs.sdk;

import com.ffs.sdk.cache.FlagCache;
import com.ffs.sdk.config.ClientConfig;
import com.ffs.sdk.sync.ConfigPoller;

import java.time.Duration;

public class FeatureFlagClientBuilder {
    private String serverUrl = "http://localhost:8080";
    private String appId;
    private Duration syncInterval = Duration.ofSeconds(10);

    public FeatureFlagClientBuilder serverUrl(String v) { this.serverUrl = v; return this; }
    public FeatureFlagClientBuilder appId(String v) { this.appId = v; return this; }
    public FeatureFlagClientBuilder syncInterval(Duration v) { this.syncInterval = v; return this; }

    public FeatureFlagClient build() {
        if (appId == null) throw new IllegalArgumentException("appId is required");
        ClientConfig config = ClientConfig.builder()
                .serverUrl(serverUrl)
                .appId(appId)
                .syncInterval(syncInterval)
                .build();
        FlagCache cache = new FlagCache();
        ConfigPoller poller = new ConfigPoller(config, cache);
        return new FeatureFlagClient(config, cache, poller);
    }
}

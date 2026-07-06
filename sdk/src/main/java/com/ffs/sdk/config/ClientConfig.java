package com.ffs.sdk.config;

import java.time.Duration;

public class ClientConfig {
    private final String serverUrl;
    private final String appId;
    private final long syncIntervalMs;
    private final int maxRetries;
    private final long retryBaseDelayMs;
    private final long retryMaxDelayMs;

    private ClientConfig(Builder b) {
        this.serverUrl = b.serverUrl;
        this.appId = b.appId;
        this.syncIntervalMs = b.syncIntervalMs;
        this.maxRetries = b.maxRetries;
        this.retryBaseDelayMs = b.retryBaseDelayMs;
        this.retryMaxDelayMs = b.retryMaxDelayMs;
    }
    public String getServerUrl() { return serverUrl; }
    public String getAppId() { return appId; }
    public long getSyncIntervalMs() { return syncIntervalMs; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryBaseDelayMs() { return retryBaseDelayMs; }
    public long getRetryMaxDelayMs() { return retryMaxDelayMs; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String serverUrl = "http://localhost:8080";
        private String appId;
        private long syncIntervalMs = 10_000;
        private int maxRetries = 5;
        private long retryBaseDelayMs = 1_000;
        private long retryMaxDelayMs = 30_000;
        public Builder serverUrl(String v) { this.serverUrl = v; return this; }
        public Builder appId(String v) { this.appId = v; return this; }
        public Builder syncInterval(Duration d) { this.syncIntervalMs = d.toMillis(); return this; }
        public ClientConfig build() {
            if (appId == null) throw new IllegalArgumentException("appId is required");
            return new ClientConfig(this);
        }
    }
}

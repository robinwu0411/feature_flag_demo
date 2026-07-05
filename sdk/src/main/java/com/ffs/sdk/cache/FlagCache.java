package com.ffs.sdk.cache;

import com.ffs.sdk.model.FlagConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlagCache {
    private volatile Map<String, FlagConfig> flags = new ConcurrentHashMap<>();
    private volatile long lastSyncTime = 0;

    public FlagConfig get(String key) { return flags.get(key); }
    public void putAll(Map<String, FlagConfig> newFlags) {
        this.flags = new ConcurrentHashMap<>(newFlags);
        this.lastSyncTime = System.currentTimeMillis();
    }
    public void merge(Map<String, FlagConfig> updated) {
        this.flags.putAll(updated);
        this.lastSyncTime = System.currentTimeMillis();
    }
    public int size() { return flags.size(); }
    public boolean isEmpty() { return flags.isEmpty(); }
    public long cacheAgeMs() { return System.currentTimeMillis() - lastSyncTime; }
}

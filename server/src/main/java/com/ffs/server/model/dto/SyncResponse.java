package com.ffs.server.model.dto;

import java.time.Instant;
import java.util.List;

public class SyncResponse {
    private List<FlagDto> flags;
    private Instant serverTime;

    public SyncResponse(List<FlagDto> flags) {
        this.flags = flags;
        this.serverTime = Instant.now();
    }

    public List<FlagDto> getFlags() { return flags; }
    public Instant getServerTime() { return serverTime; }
}

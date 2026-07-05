package com.ffs.server.controller;

import com.ffs.server.model.dto.SyncResponse;
import com.ffs.server.service.SyncService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/eval")
public class SyncController {
    private final SyncService syncService;
    private final Counter syncRequestCounter;
    private final Timer syncTimer;

    public SyncController(SyncService syncService, MeterRegistry registry) {
        this.syncService = syncService;
        this.syncRequestCounter = Counter.builder("ff_sync_requests_total").register(registry);
        this.syncTimer = Timer.builder("ff_sync_duration_ms").register(registry);
    }

    @GetMapping("/sync")
    public ResponseEntity<SyncResponse> sync(
            @RequestParam("appId") String appId,
            @RequestParam(value = "since", required = false) String since) {
        syncRequestCounter.increment();
        long start = System.currentTimeMillis();
        try {
            return ResponseEntity.ok(syncService.sync(appId, since));
        } finally {
            syncTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        }
    }
}

package com.ffs.server.controller;

import com.ffs.server.model.dto.CreateFlagRequest;
import com.ffs.server.model.entity.Flag;
import com.ffs.server.service.FlagService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class ManagementController {
    private final FlagService flagService;
    private final Counter adminOpsCounter;
    private final Counter adminReadCounter;
    private final Timer adminWriteTimer;

    public ManagementController(FlagService flagService, MeterRegistry registry) {
        this.flagService = flagService;
        this.adminOpsCounter = Counter.builder("ff_admin_operations_total").register(registry);
        this.adminReadCounter = Counter.builder("ff_admin_read_operations_total").register(registry);
        this.adminWriteTimer = Timer.builder("ff_admin_write_duration_seconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @PostMapping("/flags")
    public ResponseEntity<Flag> createFlag(@RequestBody CreateFlagRequest request) {
        Timer.Sample sample = Timer.start();
        adminOpsCounter.increment();
        ResponseEntity<Flag> resp = ResponseEntity.status(HttpStatus.CREATED).body(flagService.create(request));
        sample.stop(adminWriteTimer);
        return resp;
    }

    @GetMapping("/flags")
    public ResponseEntity<List<Flag>> listFlags() {
        adminReadCounter.increment();
        return ResponseEntity.ok(flagService.listAll());
    }

    @GetMapping("/flags/{id}")
    public ResponseEntity<Flag> getFlag(@PathVariable Long id) {
        adminReadCounter.increment();
        return ResponseEntity.ok(flagService.getById(id));
    }

    @PutMapping("/flags/{id}")
    public ResponseEntity<Flag> updateFlag(@PathVariable Long id, @RequestBody CreateFlagRequest request) {
        Timer.Sample sample = Timer.start();
        adminOpsCounter.increment();
        ResponseEntity<Flag> resp = ResponseEntity.ok(flagService.update(id, request));
        sample.stop(adminWriteTimer);
        return resp;
    }

    @DeleteMapping("/flags/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable Long id) {
        Timer.Sample sample = Timer.start();
        adminOpsCounter.increment();
        flagService.delete(id);
        sample.stop(adminWriteTimer);
        return ResponseEntity.noContent().build();
    }
}

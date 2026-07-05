package com.ffs.server.controller;

import com.ffs.server.model.dto.*;
import com.ffs.server.model.entity.*;
import com.ffs.server.service.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class ManagementController {
    private final FlagService flagService;
    private final ApplicationService applicationService;
    private final AuditService auditService;
    private final Counter adminOpsCounter;

    public ManagementController(FlagService flagService,
                                ApplicationService applicationService,
                                AuditService auditService,
                                MeterRegistry registry) {
        this.flagService = flagService;
        this.applicationService = applicationService;
        this.auditService = auditService;
        this.adminOpsCounter = Counter.builder("ff_admin_operations_total").register(registry);
    }

    @PostMapping("/flags")
    public ResponseEntity<Flag> createFlag(@RequestBody CreateFlagRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.status(HttpStatus.CREATED).body(flagService.create(request));
    }

    @GetMapping("/flags")
    public ResponseEntity<List<Flag>> listFlags() {
        return ResponseEntity.ok(flagService.listAll());
    }

    @GetMapping("/flags/{id}")
    public ResponseEntity<Flag> getFlag(@PathVariable Long id) {
        return ResponseEntity.ok(flagService.getById(id));
    }

    @PutMapping("/flags/{id}")
    public ResponseEntity<Flag> updateFlag(@PathVariable Long id, @RequestBody CreateFlagRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.update(id, request));
    }

    @DeleteMapping("/flags/{id}")
    public ResponseEntity<Void> deleteFlag(@PathVariable Long id) {
        adminOpsCounter.increment();
        flagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flags/{id}/rules")
    public ResponseEntity<TargetingRule> addRule(@PathVariable Long id, @RequestBody CreateRuleRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.status(HttpStatus.CREATED).body(flagService.addRule(id, request));
    }

    @GetMapping("/flags/{id}/rules")
    public ResponseEntity<List<TargetingRule>> getRules(@PathVariable Long id) {
        return ResponseEntity.ok(flagService.getRules(id));
    }

    @PutMapping("/flags/{id}/rules/{ruleId}")
    public ResponseEntity<TargetingRule> updateRule(@PathVariable Long id, @PathVariable Long ruleId,
                                                     @RequestBody CreateRuleRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.updateRule(id, ruleId, request));
    }

    @DeleteMapping("/flags/{id}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id, @PathVariable Long ruleId) {
        adminOpsCounter.increment();
        flagService.deleteRule(id, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flags/{id}/rollout")
    public ResponseEntity<Rollout> setRollout(@PathVariable Long id, @RequestBody CreateRolloutRequest request) {
        adminOpsCounter.increment();
        return ResponseEntity.ok(flagService.setRollout(id, request));
    }

    @PostMapping("/flags/{id}/bind")
    public ResponseEntity<Void> bindFlag(@PathVariable Long id, @RequestBody BindFlagRequest request) {
        adminOpsCounter.increment();
        flagService.bindApplication(id, request.getApplicationId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applications")
    public ResponseEntity<Application> createApplication(@RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.create(request.getName(), request.getDescription()));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<Application>> listApplications() {
        return ResponseEntity.ok(applicationService.listAll());
    }

    @GetMapping("/flags/{id}/audit")
    public ResponseEntity<List<AuditLog>> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getAuditLogs(id));
    }
}

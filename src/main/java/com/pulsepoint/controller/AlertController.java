package com.pulsepoint.controller;

import com.pulsepoint.model.Alert;
import com.pulsepoint.model.AlertRule;
import com.pulsepoint.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {
    private final AlertService alertService;

    @PostMapping("/api/sources/{sourceId}/alerts")
    public ResponseEntity<AlertRule> insert(@PathVariable Long sourceId , @RequestBody AlertRule b ){
         return ResponseEntity.status(201).body(alertService.addRule(sourceId,b));
    }


    @PostMapping("/api/sources/{sourceId}/rules")
    public ResponseEntity<AlertRule> addRule(
            @PathVariable Long sourceId,
            @Valid @RequestBody AlertRule rule) {
        return ResponseEntity.status(201).body(alertService.addRule(sourceId, rule));
    }

    @GetMapping("/api/sources/{sourceId}/rules")
    public ResponseEntity<List<AlertRule>> getRules(@PathVariable Long sourceId) {
        return ResponseEntity.ok(alertService.getRulesForSource(sourceId));
    }

    @GetMapping("/api/alerts")
    public ResponseEntity<List<Alert>> getAlerts(
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved) {
        return ResponseEntity.ok(alertService.getAlerts(sourceId, severity, resolved));
    }

    @PatchMapping("/api/alerts/{alertId}/resolve")
    public ResponseEntity<Alert> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.resolveAlert(alertId));
    }
}

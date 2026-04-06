package com.pulsepoint.service;

import com.pulsepoint.model.Alert;
import com.pulsepoint.model.AlertRule;
import com.pulsepoint.model.Source;
import com.pulsepoint.repository.AlertRepository;
import com.pulsepoint.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final SourceService sourceService;


    public AlertRule addRule(Long sourceId, AlertRule rule){
        Source source = sourceService.findSourceOrThrow(sourceId);
        rule.setSource(source);
        rule.setActive(true);
        return alertRuleRepository.save(rule);
    }

    public List<AlertRule> getRulesForSource(Long sourceId) {
        Source source = sourceService.findSourceOrThrow(sourceId);
        return alertRuleRepository.findBySource(source);
    }

    public List<Alert> getAlerts(Long sourceId, String severity, Boolean resolved) {
        List<Alert> alerts = sourceId != null
                ? alertRepository.findBySourceOrderByTriggeredAtDesc(sourceService.findSourceOrThrow(sourceId))
                : alertRepository.findAllByOrderByTriggeredAtDesc();

        List<Alert> filtered = new ArrayList<>();
        for (Alert alert : alerts) {
            if (severity != null && !alert.getSeverity().name().equalsIgnoreCase(severity)) continue;
            if (resolved != null && (alert.getResolvedAt() != null) != resolved) continue;
            filtered.add(alert);
        }
        return filtered;
    }

    public Alert resolveAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alert not found with id: " + alertId));
        if (alert.getResolvedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Alert " + alertId + " is already resolved");
        }
        alert.setResolvedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }
}

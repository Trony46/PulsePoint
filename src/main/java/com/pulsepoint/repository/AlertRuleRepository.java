package com.pulsepoint.repository;

import com.pulsepoint.model.AlertRule;
import com.pulsepoint.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findBySourceAndMetricAndActiveTrue(Source source, String metric);
    List<AlertRule> findBySource(Source source);
}

package com.pulsepoint.repository;

import com.pulsepoint.model.Alert;
import com.pulsepoint.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert,Long> {
    List<Alert> findBySourceOrderByTriggeredAtDesc(Source source);
    List<Alert> findAllByOrderByTriggeredAtDesc();
}

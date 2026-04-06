package com.pulsepoint.repository;

import com.pulsepoint.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {
}

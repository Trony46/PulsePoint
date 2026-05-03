package com.pulsepoint.repository;

import com.pulsepoint.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SourceRepository extends JpaRepository<Source, Long> {
    Optional<Source> findByApiKey(String apiKey);

}

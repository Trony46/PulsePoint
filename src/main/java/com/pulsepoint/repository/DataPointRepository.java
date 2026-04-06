package com.pulsepoint.repository;

import com.pulsepoint.model.DataPoint;
import com.pulsepoint.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DataPointRepository extends JpaRepository<DataPoint, Long> {

    List<DataPoint> findBySourceAndMetricAndTimestampBetweenOrderByTimestampDesc(
            Source source, String metric, LocalDateTime from, LocalDateTime to
    );
    Optional<DataPoint> findFirstBySourceAndMetricOrderByTimestampDesc(
            Source source, String metric
    );
    @Query("SELECT DISTINCT d.metric FROM DataPoint d WHERE d.source = :source")
    List<String> findDistinctMetricsBySource(@Param("source") Source source);

    @Query("SELECT AVG(d.value) FROM DataPoint d WHERE d.source = :source AND d.metric = :metric AND d.timestamp BETWEEN :from AND :to")
    Double findAverageValue(@Param("source") Source source, @Param("metric") String metric,
                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MIN(d.value) FROM DataPoint d WHERE d.source = :source AND d.metric = :metric AND d.timestamp BETWEEN :from AND :to")
    Double findMinValue(@Param("source") Source source, @Param("metric") String metric,
                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT MAX(d.value) FROM DataPoint d WHERE d.source = :source AND d.metric = :metric AND d.timestamp BETWEEN :from AND :to")
    Double findMaxValue(@Param("source") Source source, @Param("metric") String metric,
                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    Long countBySourceAndMetricAndTimestampBetween(
            Source source, String metric, LocalDateTime from, LocalDateTime to
    );

}

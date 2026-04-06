package com.pulsepoint.controller;

import com.pulsepoint.model.DataPoint;
import com.pulsepoint.model.Summary;
import com.pulsepoint.service.IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IngestController {
    private final IngestService ingestService;

    @PostMapping("/api/ingest/{id}")
    public ResponseEntity<DataPoint> ingest(@PathVariable Long id , @RequestBody DataPoint dp){
        return new ResponseEntity<>(ingestService.ingest(id,dp), HttpStatus.ACCEPTED);
    }

    @PostMapping("/api/ingest/{id}/batch")
    public ResponseEntity<List<DataPoint>> ingestBatch(@PathVariable Long id , @RequestBody List<DataPoint> dp ){
        return new ResponseEntity<>(ingestService.ingestBatch(id,dp),HttpStatus.ACCEPTED);
    }

    @GetMapping("/api/sources/{sourceId}/data")
    public ResponseEntity<List<DataPoint>> getHistory(
            @PathVariable Long sourceId,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ingestService.getHistory(sourceId, metric, from, to));
    }

    @GetMapping("/api/sources/{sourceId}/latest")
    public ResponseEntity<List<DataPoint>> getLatest(@PathVariable Long sourceId) {
        return ResponseEntity.ok(ingestService.getLatestReadings(sourceId));
    }

    @GetMapping("/api/sources/{sourceId}/summary")
    public ResponseEntity<Summary> getSummary(
            @PathVariable Long sourceId,
            @RequestParam String metric,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ingestService.getSummary(sourceId, metric, from, to));
    }
}

package com.pulsepoint.service;

import com.pulsepoint.enums.RuleOperator;
import com.pulsepoint.model.*;
import com.pulsepoint.repository.AlertRepository;
import com.pulsepoint.repository.AlertRuleRepository;
import com.pulsepoint.repository.DataPointRepository;
import com.pulsepoint.repository.SourceRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngestService {
    private final SourceService sourceService;
    private final SourceRepository sourceRepository;
    private final DataPointRepository dataPointRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;

    public DataPoint ingest(Long sourceId, DataPoint dataPoint){
        Source s = sourceService.findSourceOrThrow(sourceId);
        dataPoint.setSource(s);
        if(dataPoint.getTimestamp()==null)
            dataPoint.setTimestamp(LocalDateTime.now());

        DataPoint saved = dataPointRepository.save(dataPoint);
        s.setLastSeenAt(LocalDateTime.now());
        sourceRepository.save(s);
        //
        evaluateAlertRules(s, saved);
        //
        return saved;
    }

    public List<DataPoint> ingestBatch(Long sourceId, List<DataPoint> dataPoints){
        ArrayList<DataPoint> saved = new ArrayList<>();
        for (DataPoint dp :dataPoints)
                saved.add(ingest(sourceId,dp));
        return saved;
    }

    public List<DataPoint> getHistory(Long id, String metric, LocalDateTime from, LocalDateTime to){
        Source source = sourceService.findSourceOrThrow(id);
        return dataPointRepository.findBySourceAndMetricAndTimestampBetweenOrderByTimestampDesc(source,metric,from,to);
    }

    public List<DataPoint> getLatestReadings(Long sourceId){
        Source source = sourceService.findSourceOrThrow(sourceId);
        List<String> metrics = dataPointRepository.findDistinctMetricsBySource(source);
        List<DataPoint> result = new ArrayList<>();
        for(String m: metrics){
            Optional<DataPoint> dp =  dataPointRepository.findFirstBySourceAndMetricOrderByTimestampDesc(source,m);
            if (dp.isPresent()) {
                result.add(dp.get());
            }
        }
        return result;
    }

    public Summary getSummary(Long sourceId, String metric, LocalDateTime from, LocalDateTime to){
        Source source = sourceService.findSourceOrThrow(sourceId);
        Summary summary ;
        return new Summary(
                metric,
                dataPointRepository.findAverageValue(source,metric,from,to),
                dataPointRepository.findMinValue(source,metric,from,to),
                dataPointRepository.findMaxValue(source,metric,from,to),
                dataPointRepository.countBySourceAndMetricAndTimestampBetween(source,metric,from,to),
                from,
                to
        );
    }

    ///////////
    private void evaluateAlertRules(Source source, DataPoint dataPoint) {
        List<AlertRule> rules = alertRuleRepository
                .findBySourceAndMetricAndActiveTrue(source, dataPoint.getMetric());
        for (AlertRule rule : rules) {
            if (checkRule(rule.getOperator(), dataPoint.getValue(), rule.getThreshold())) {
                createAlert(source, rule, dataPoint);
            }
        }
    }

    private boolean checkRule(@NotNull RuleOperator operator, Double incoming, Double threshold) {
        switch (operator) {
            case GT:  return incoming > threshold;
            case LT:  return incoming < threshold;
            case GTE: return incoming >= threshold;
            case LTE: return incoming <= threshold;
            case EQ:  return incoming.equals(threshold);
            default:  return false;
        }
    }

    private void createAlert(Source source, AlertRule rule, DataPoint dataPoint) {
        Alert alert = new Alert();
        alert.setSource(source);
        alert.setRule(rule);
        alert.setMetric(dataPoint.getMetric());
        alert.setTriggeredValue(dataPoint.getValue());
        alert.setSeverity(rule.getSeverity());
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setResolvedAt(null);
        alertRepository.save(alert);
    }
}

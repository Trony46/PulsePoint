package com.pulsepoint.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Summary {
    private String metric;
    private Double average;
    private Double minimum;
    private Double maximum;
    private Long totalReadings;
    private LocalDateTime from;
    private LocalDateTime to;
}


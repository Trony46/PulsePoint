package com.pulsepoint.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.pulsepoint.enums.RuleOperator;
import com.pulsepoint.enums.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id",nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Source source;

    @ManyToOne
    @JoinColumn(name = "rule_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AlertRule rule;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String metric;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private double triggeredValue;

    @Enumerated(EnumType.STRING)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Severity severity;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime triggeredAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime resolvedAt;

}

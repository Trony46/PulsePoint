package com.pulsepoint.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.pulsepoint.enums.RuleOperator;
import com.pulsepoint.enums.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_id",nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Source source;

    @NotBlank
    private String metric;//

    @NotNull
    @Enumerated(EnumType.STRING)
    private RuleOperator operator;//

    @NotNull
    private double threshold;//

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;//

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean active;
}

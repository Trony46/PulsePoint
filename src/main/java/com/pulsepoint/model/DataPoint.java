package com.pulsepoint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_points",
        indexes = {
        @Index(columnList = "source_id,metric,timestamp")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPoint {

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
    private double value;//

    private String unit;//

    private LocalDateTime timestamp;
}

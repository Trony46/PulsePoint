package com.pulsepoint.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "sources")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    private String name;//

    @NotBlank
    private String type;//

    private String description;//

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean active;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime registeredAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastSeenAt;


}

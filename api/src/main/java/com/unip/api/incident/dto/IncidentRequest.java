package com.unip.api.incident.dto;

import com.unip.api.incident.IncidentType;
import com.unip.api.incident.Severity;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record IncidentRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull IncidentType type,
        @NotNull Severity severity,
        @NotNull Long sectorId,
        @NotNull @PastOrPresent LocalDate occurredOn
) {}
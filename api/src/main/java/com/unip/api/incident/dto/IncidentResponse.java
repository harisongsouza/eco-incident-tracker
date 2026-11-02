package com.unip.api.incident.dto;

import com.unip.api.incident.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentResponse(
        Long id, String title, String description, IncidentType type,
        Severity severity, IncidentStatus status, String sector,
        LocalDate occurredOn, LocalDateTime registeredAt, int cycle
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(i.getId(), i.getTitle(), i.getDescription(),
                i.getType(), i.getSeverity(), i.getStatus(), i.getSector().getName(),
                i.getOccurredOn(), i.getRegisteredAt(), i.getCycle());
    }
}

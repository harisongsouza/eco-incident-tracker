package com.unip.client.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentDto(
        Long id, String title, String description, String type, String severity,
        String status, String sector, LocalDate occurredOn, LocalDateTime registeredAt,
        int cycle, String rootCause, LocalDateTime closedAt, String registeredBy
) {}
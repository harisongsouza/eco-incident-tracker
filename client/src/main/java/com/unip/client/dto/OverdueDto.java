package com.unip.client.dto;

import java.time.LocalDate;

public record OverdueDto(Long actionId, Long incidentId, String incidentTitle,
                         String description, String responsible,
                         LocalDate deadline, long daysOverdue) {}
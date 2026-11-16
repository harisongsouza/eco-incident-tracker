package com.unip.api.report.dto;

import com.unip.api.action.CorrectiveAction;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record OverdueActionResponse(Long actionId, Long incidentId, String incidentTitle,
                                    String description, String responsible,
                                    LocalDate deadline, long daysOverdue) {
    public static OverdueActionResponse from(CorrectiveAction a, LocalDate today) {
        return new OverdueActionResponse(a.getId(), a.getIncident().getId(),
                a.getIncident().getTitle(), a.getDescription(), a.getResponsible(),
                a.getDeadline(), ChronoUnit.DAYS.between(a.getDeadline(), today));
    }
}
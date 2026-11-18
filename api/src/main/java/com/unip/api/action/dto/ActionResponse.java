package com.unip.api.action.dto;

import com.unip.api.action.ActionStatus;
import com.unip.api.action.CorrectiveAction;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActionResponse(
        Long id, Long incidentId, String description, Long responsibleId, String responsible,
        LocalDate deadline, ActionStatus status, int cycle,
        String completionNote, LocalDateTime completedAt
) {
    public static ActionResponse from(CorrectiveAction a) {
        return new ActionResponse(a.getId(), a.getIncident().getId(), a.getDescription(),
                a.getResponsible().getId(), a.getResponsible().getName(),
                a.getDeadline(), a.getStatus(), a.getCycle(),
                a.getCompletionNote(), a.getCompletedAt());
    }
}
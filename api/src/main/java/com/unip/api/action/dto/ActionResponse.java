package com.unip.api.action.dto;

import com.unip.api.action.ActionStatus;
import com.unip.api.action.CorrectiveAction;
import java.time.LocalDate;

public record ActionResponse(
        Long id, Long incidentId, String description, String responsible,
        LocalDate deadline, ActionStatus status, int cycle
) {
    public static ActionResponse from(CorrectiveAction a) {
        return new ActionResponse(a.getId(), a.getIncident().getId(), a.getDescription(),
                a.getResponsible(), a.getDeadline(), a.getStatus(), a.getCycle());
    }
}
package com.unip.api.action.dto;

import com.unip.api.action.CorrectiveAction;
import com.unip.api.incident.IncidentStatus;
import java.time.LocalDate;

public record MyActionResponse(Long actionId, Long incidentId, String incidentTitle,
                               IncidentStatus incidentStatus, String description,
                               LocalDate deadline, int cycle) {
    public static MyActionResponse from(CorrectiveAction a) {
        return new MyActionResponse(a.getId(), a.getIncident().getId(), a.getIncident().getTitle(),
                a.getIncident().getStatus(), a.getDescription(), a.getDeadline(), a.getCycle());
    }
}
package com.unip.api.check.dto;

import com.unip.api.check.EffectivenessCheck;
import com.unip.api.incident.IncidentStatus;
import java.time.LocalDateTime;

public record CheckResponse(
        Long id, Long incidentId, boolean effective, String opinion,
        int cycle, LocalDateTime checkedAt, String checkedBy, IncidentStatus incidentStatus
) {
    public static CheckResponse from(EffectivenessCheck c) {
        return new CheckResponse(c.getId(), c.getIncident().getId(), c.isEffective(),
                c.getOpinion(), c.getCycle(), c.getCheckedAt(), c.getCheckedBy().getName(),
                c.getIncident().getStatus());
    }
}
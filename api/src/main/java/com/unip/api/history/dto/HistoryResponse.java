package com.unip.api.history.dto;

import com.unip.api.history.StatusHistory;
import com.unip.api.incident.IncidentStatus;
import java.time.LocalDateTime;

public record HistoryResponse(IncidentStatus previousStatus, IncidentStatus newStatus,
                              LocalDateTime changedAt, String comment, String changedBy) {
    public static HistoryResponse from(StatusHistory h) {
        return new HistoryResponse(h.getPreviousStatus(), h.getNewStatus(),
                h.getChangedAt(), h.getComment(), h.getChangedBy().getName());
    }
}
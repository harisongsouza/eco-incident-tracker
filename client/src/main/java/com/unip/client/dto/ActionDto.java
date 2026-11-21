package com.unip.client.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActionDto(
        Long id, Long incidentId, String description, Long responsibleId, String responsible,
        LocalDate deadline, String status, int cycle,
        String completionNote, LocalDateTime completedAt
) {
    /** Overdue = still pending and past the deadline (calculated, never stored). */
    public boolean overdue() {
        return "PENDING".equals(status) && deadline.isBefore(LocalDate.now());
    }
}
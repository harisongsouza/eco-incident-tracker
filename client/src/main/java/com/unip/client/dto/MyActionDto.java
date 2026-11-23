package com.unip.client.dto;

import java.time.LocalDate;

public record MyActionDto(Long actionId, Long incidentId, String incidentTitle,
                          String incidentStatus, String description,
                          LocalDate deadline, int cycle) {

    /** Every action in this list is pending, so overdue only depends on the deadline. */
    public boolean overdue() {
        return deadline.isBefore(LocalDate.now());
    }
}
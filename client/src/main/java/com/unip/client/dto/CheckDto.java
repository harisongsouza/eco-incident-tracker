package com.unip.client.dto;

import java.time.LocalDateTime;

public record CheckDto(
        Long id, Long incidentId, boolean effective, String opinion,
        int cycle, LocalDateTime checkedAt, String checkedBy, String incidentStatus
) {}
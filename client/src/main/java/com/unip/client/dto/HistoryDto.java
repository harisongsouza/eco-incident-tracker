package com.unip.client.dto;

import java.time.LocalDateTime;

public record HistoryDto(
        String previousStatus, String newStatus, LocalDateTime changedAt,
        String comment, String changedBy
) {}
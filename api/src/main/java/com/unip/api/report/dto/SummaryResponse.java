package com.unip.api.report.dto;

import java.util.Map;

public record SummaryResponse(long total, Map<String, Long> byStatus,
                              Map<String, Long> byType, Map<String, Long> bySector) {}
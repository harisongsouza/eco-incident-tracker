package com.unip.client.dto;

import java.util.Map;

public record SummaryDto(long total, Map<String, Long> byStatus,
                         Map<String, Long> byType, Map<String, Long> bySector) {

    /** Share of the total, 0 to 100, used by the bars in the report. */
    public int percent(long count) {
        return total == 0 ? 0 : (int) Math.round(count * 100.0 / total);
    }
}
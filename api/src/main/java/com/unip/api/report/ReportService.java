package com.unip.api.report;

import com.unip.api.action.ActionStatus;
import com.unip.api.action.CorrectiveActionRepository;
import com.unip.api.incident.IncidentRepository;
import com.unip.api.incident.IncidentStatus;
import com.unip.api.incident.IncidentType;
import com.unip.api.report.dto.OverdueActionResponse;
import com.unip.api.report.dto.SummaryResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final IncidentRepository incidents;
    private final CorrectiveActionRepository actions;

    public ReportService(IncidentRepository incidents, CorrectiveActionRepository actions) {
        this.incidents = incidents;
        this.actions = actions;
    }

    public SummaryResponse summary() {
        Map<String, Long> byStatus = zeroed(IncidentStatus.values());
        incidents.countByStatus().forEach(r -> byStatus.put(((IncidentStatus) r[0]).name(), (Long) r[1]));

        Map<String, Long> byType = zeroed(IncidentType.values());
        incidents.countByType().forEach(r -> byType.put(((IncidentType) r[0]).name(), (Long) r[1]));

        Map<String, Long> bySector = new LinkedHashMap<>();
        incidents.countBySector().forEach(r -> bySector.put((String) r[0], (Long) r[1]));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new SummaryResponse(total, byStatus, byType, bySector);
    }

    public List<OverdueActionResponse> overdue() {
        LocalDate today = LocalDate.now();
        return actions.findOverdue(today, ActionStatus.PENDING,
                        List.of(IncidentStatus.UNDER_ANALYSIS, IncidentStatus.IN_PROGRESS))
                .stream().map(a -> OverdueActionResponse.from(a, today)).toList();
    }

    private Map<String, Long> zeroed(Enum<?>[] values) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Enum<?> v : values) map.put(v.name(), 0L);
        return map;
    }
}
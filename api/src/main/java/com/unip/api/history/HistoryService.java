package com.unip.api.history;

import com.unip.api.incident.IncidentService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HistoryService {

    private final StatusHistoryRepository history;
    private final IncidentService incidentService;

    public HistoryService(StatusHistoryRepository history, IncidentService incidentService) {
        this.history = history;
        this.incidentService = incidentService;
    }

    public List<StatusHistory> list(Long incidentId) {
        incidentService.findVisible(incidentId); // 404 if the incident doesn't exist
        return history.findByIncidentIdOrderByChangedAtAsc(incidentId);
    }
}
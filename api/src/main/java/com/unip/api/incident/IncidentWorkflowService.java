package com.unip.api.incident;

import com.unip.api.history.StatusHistory;
import com.unip.api.history.StatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentWorkflowService {

    private final IncidentService incidentService;
    private final StatusHistoryRepository history;

    public IncidentWorkflowService(IncidentService incidentService,
                                   StatusHistoryRepository history) {
        this.incidentService = incidentService;
        this.history = history;
    }

    @Transactional
    public Incident startAnalysis(Long incidentId) {
        Incident incident = incidentService.findById(incidentId); // 404 if it doesn't exist
        IncidentStatus previous = incident.getStatus();           // must be read BEFORE the change

        incident.transitionTo(IncidentStatus.UNDER_ANALYSIS);     // 409 if the transition is not allowed

        history.save(new StatusHistory(incident, previous,
                incident.getStatus(), "Analysis started"));
        return incident;
    }
}
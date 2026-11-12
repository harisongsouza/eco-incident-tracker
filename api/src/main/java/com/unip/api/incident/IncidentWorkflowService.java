package com.unip.api.incident;

import com.unip.api.action.CorrectiveActionRepository;
import com.unip.api.history.StatusHistory;
import com.unip.api.history.StatusHistoryRepository;
import com.unip.api.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentWorkflowService {

    private final IncidentService incidentService;
    private final StatusHistoryRepository history;
    private final CorrectiveActionRepository actions;

    public IncidentWorkflowService(IncidentService incidentService,
                                   StatusHistoryRepository history,
                                   CorrectiveActionRepository actions) {
        this.incidentService = incidentService;
        this.history = history;
        this.actions = actions;
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

    @Transactional
    public Incident startExecution(Long incidentId) {
        Incident incident = incidentService.findById(incidentId);

        if (incident.getRootCause() == null || incident.getRootCause().isBlank()) {
            throw new BusinessRuleException("Root cause must be registered before starting execution");
        }
        if (actions.countByIncidentIdAndCycle(incidentId, incident.getCycle()) == 0) {
            throw new BusinessRuleException("At least one corrective action is required");
        }

        IncidentStatus previous = incident.getStatus();
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
        history.save(new StatusHistory(incident, previous, incident.getStatus(), "Execution started"));
        return incident;
    }
}
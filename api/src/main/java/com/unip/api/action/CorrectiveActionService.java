package com.unip.api.action;

import com.unip.api.action.dto.ActionRequest;
import com.unip.api.incident.Incident;
import com.unip.api.incident.IncidentService;
import com.unip.api.incident.IncidentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository actions;
    private final IncidentService incidentService;

    public CorrectiveActionService(CorrectiveActionRepository actions,
                                   IncidentService incidentService) {
        this.actions = actions;
        this.incidentService = incidentService;
    }

    @Transactional
    public CorrectiveAction create(Long incidentId, ActionRequest req) {
        Incident incident = incidentService.findById(incidentId);
        incident.requireStatus(IncidentStatus.UNDER_ANALYSIS, "add a corrective action");
        return actions.save(new CorrectiveAction(
                incident, req.description(), req.responsible(), req.deadline()));
    }

    public List<CorrectiveAction> list(Long incidentId) {
        incidentService.findById(incidentId); // 404 if the incident doesn't exist
        return actions.findByIncidentIdOrderByIdAsc(incidentId);
    }
}
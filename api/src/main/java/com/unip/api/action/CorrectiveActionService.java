package com.unip.api.action;

import com.unip.api.action.dto.ActionRequest;
import com.unip.api.incident.Incident;
import com.unip.api.incident.IncidentService;
import com.unip.api.incident.IncidentStatus;
import com.unip.api.user.User;
import com.unip.api.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CorrectiveActionService {

    private final CorrectiveActionRepository actions;
    private final IncidentService incidentService;
    private final UserService userService;

    public CorrectiveActionService(CorrectiveActionRepository actions,
                                   IncidentService incidentService,
                                   UserService userService) {
        this.actions = actions;
        this.incidentService = incidentService;
        this.userService = userService;
    }

    @Transactional
    public CorrectiveAction create(Long incidentId, ActionRequest req) {
        Incident incident = incidentService.findById(incidentId);
        incident.requireStatus(IncidentStatus.UNDER_ANALYSIS, "add a corrective action");
        User responsible = userService.findActiveById(req.responsibleId());
        return actions.save(new CorrectiveAction(
                incident, req.description(), responsible, req.deadline()));
    }

    public List<CorrectiveAction> list(Long incidentId) {
        incidentService.findVisible(incidentId);
        return actions.findByIncidentIdOrderByIdAsc(incidentId);
    }
}
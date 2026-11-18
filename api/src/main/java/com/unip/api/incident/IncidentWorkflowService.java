package com.unip.api.incident;

import com.unip.api.action.ActionStatus;
import com.unip.api.action.CorrectiveAction;
import com.unip.api.action.CorrectiveActionRepository;
import com.unip.api.check.EffectivenessCheck;
import com.unip.api.check.EffectivenessCheckRepository;
import com.unip.api.history.StatusHistory;
import com.unip.api.history.StatusHistoryRepository;
import com.unip.api.security.CurrentUser;
import com.unip.api.shared.exception.BusinessRuleException;
import com.unip.api.shared.exception.ForbiddenException;
import com.unip.api.shared.exception.ResourceNotFoundException;
import com.unip.api.user.Role;
import com.unip.api.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentWorkflowService {

    private final IncidentService incidentService;
    private final StatusHistoryRepository history;
    private final CorrectiveActionRepository actions;
    private final EffectivenessCheckRepository checks;
    private final CurrentUser currentUser;

    public IncidentWorkflowService(IncidentService incidentService,
                                   StatusHistoryRepository history,
                                   CorrectiveActionRepository actions,
                                   EffectivenessCheckRepository checks,
                                   CurrentUser currentUser) {
        this.incidentService = incidentService;
        this.history = history;
        this.actions = actions;
        this.checks = checks;
        this.currentUser = currentUser;
    }

    @Transactional
    public Incident startAnalysis(Long incidentId) {
        Incident incident = incidentService.findById(incidentId);
        move(incident, IncidentStatus.UNDER_ANALYSIS, "Analysis started");
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

        move(incident, IncidentStatus.IN_PROGRESS, "Execution started");
        return incident;
    }

    @Transactional
    public CorrectiveAction completeAction(Long actionId, String note) {
        CorrectiveAction action = actions.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Corrective action not found"));
        Incident incident = action.getIncident();

        User user = currentUser.get();
        boolean isResponsible = action.getResponsible().getId().equals(user.getId());
        if (!isResponsible && user.getRole() != Role.ENVIRONMENTAL_OFFICER) {
            throw new ForbiddenException(
                    "Only the responsible user or an environmental officer can complete this action");
        }

        incident.requireStatus(IncidentStatus.IN_PROGRESS, "complete an action");
        action.complete(note);

        long pending = actions.countByIncidentIdAndCycleAndStatus(
                incident.getId(), incident.getCycle(), ActionStatus.PENDING);
        if (pending == 0) { // last action of the cycle: automatic transition
            move(incident, IncidentStatus.AWAITING_VERIFICATION,
                    "All actions of cycle " + incident.getCycle() + " completed");
        }
        return action;
    }

    @Transactional
    public EffectivenessCheck verify(Long incidentId, boolean effective, String opinion) {
        Incident incident = incidentService.findById(incidentId);
        incident.requireStatus(IncidentStatus.AWAITING_VERIFICATION, "register a verification");

        // created BEFORE the cycle changes, so it records the cycle being verified
        EffectivenessCheck check = checks.save(
                new EffectivenessCheck(incident, effective, opinion, currentUser.get()));
        if (effective) {
            move(incident, IncidentStatus.CLOSED, "Verified: effective");
        } else {
            move(incident, IncidentStatus.UNDER_ANALYSIS, "Verified: ineffective, new cycle started");
            incident.startNextCycle();
        }
        return check;
    }

    @Transactional
    public Incident dismiss(Long incidentId, String justification) {
        Incident incident = incidentService.findById(incidentId);
        move(incident, IncidentStatus.DISMISSED, "Dismissed: " + justification);
        return incident;
    }

    private void move(Incident incident, IncidentStatus target, String comment) {
        IncidentStatus previous = incident.getStatus(); // read BEFORE the change
        incident.transitionTo(target);
        history.save(new StatusHistory(incident, previous, target, comment, currentUser.get()));
    }
}
package com.unip.api.incident;

import java.util.Map;
import java.util.Set;

public enum IncidentStatus {
    OPEN, UNDER_ANALYSIS, IN_PROGRESS, AWAITING_VERIFICATION, CLOSED, DISMISSED;

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED = Map.of(
            OPEN, Set.of(UNDER_ANALYSIS, DISMISSED),
            UNDER_ANALYSIS, Set.of(IN_PROGRESS, DISMISSED),
            IN_PROGRESS, Set.of(AWAITING_VERIFICATION),
            AWAITING_VERIFICATION, Set.of(CLOSED, UNDER_ANALYSIS)
    );

    public boolean canTransitionTo(IncidentStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
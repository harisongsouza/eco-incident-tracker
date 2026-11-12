package com.unip.api.incident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncidentStatusTest {

    @Test
    void openCanGoToUnderAnalysis() {
        assertTrue(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.UNDER_ANALYSIS));
    }

    @Test
    void openCannotGoDirectlyToClosed() {
        assertFalse(IncidentStatus.OPEN.canTransitionTo(IncidentStatus.CLOSED));
    }

    @Test
    void finalStatesGoNowhere() {
        for (IncidentStatus target : IncidentStatus.values()) {
            assertFalse(IncidentStatus.CLOSED.canTransitionTo(target));
            assertFalse(IncidentStatus.DISMISSED.canTransitionTo(target));
        }
    }
}
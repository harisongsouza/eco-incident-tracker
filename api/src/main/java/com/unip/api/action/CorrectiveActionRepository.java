package com.unip.api.action;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    long countByIncidentIdAndCycle(Long incidentId, int cycle);
    long countByIncidentIdAndCycleAndStatus(Long incidentId, int cycle, ActionStatus status);
    List<CorrectiveAction> findByIncidentIdOrderByIdAsc(Long incidentId);
}
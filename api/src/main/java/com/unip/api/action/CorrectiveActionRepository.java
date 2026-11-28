package com.unip.api.action;

import com.unip.api.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    long countByIncidentIdAndCycle(Long incidentId, int cycle);
    long countByIncidentIdAndCycleAndStatus(Long incidentId, int cycle, ActionStatus status);
    boolean existsByIncidentIdAndResponsibleId(Long incidentId, Long responsibleId);
    List<CorrectiveAction> findByIncidentIdOrderByIdAsc(Long incidentId);

    @Query("select a from CorrectiveAction a "
            + "where a.status = :pending and a.deadline < :today and a.incident.status in :active "
            + "order by a.deadline asc")
    List<CorrectiveAction> findOverdue(@Param("today") LocalDate today,
                                       @Param("pending") ActionStatus pending,
                                       @Param("active") Collection<IncidentStatus> active);

    @Query("select a from CorrectiveAction a "
            + "where a.responsible.id = :userId and a.status = :pending and a.incident.status in :active "
            + "order by a.deadline asc")
    List<CorrectiveAction> findPendingForUser(@Param("userId") Long userId,
                                              @Param("pending") ActionStatus pending,
                                              @Param("active") Collection<IncidentStatus> active);
}
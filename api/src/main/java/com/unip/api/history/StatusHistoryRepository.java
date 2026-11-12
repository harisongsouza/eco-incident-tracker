package com.unip.api.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByIncidentIdOrderByChangedAtAsc(Long incidentId);
}
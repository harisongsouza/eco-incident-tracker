package com.unip.api.check;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EffectivenessCheckRepository extends JpaRepository<EffectivenessCheck, Long> {
    List<EffectivenessCheck> findByIncidentIdOrderByIdAsc(Long incidentId);
}
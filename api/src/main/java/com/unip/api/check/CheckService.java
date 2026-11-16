package com.unip.api.check;

import com.unip.api.incident.IncidentService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CheckService {

    private final EffectivenessCheckRepository checks;
    private final IncidentService incidentService;

    public CheckService(EffectivenessCheckRepository checks, IncidentService incidentService) {
        this.checks = checks;
        this.incidentService = incidentService;
    }

    public List<EffectivenessCheck> list(Long incidentId) {
        incidentService.findById(incidentId);
        return checks.findByIncidentIdOrderByIdAsc(incidentId);
    }
}
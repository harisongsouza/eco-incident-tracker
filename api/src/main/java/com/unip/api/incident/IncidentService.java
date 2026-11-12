package com.unip.api.incident;

import com.unip.api.incident.dto.IncidentRequest;
import com.unip.api.sector.Sector;
import com.unip.api.sector.SectorRepository;
import com.unip.api.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final SectorRepository sectors;

    public IncidentService(IncidentRepository incidents, SectorRepository sectors) {
        this.incidents = incidents;
        this.sectors = sectors;
    }

    public Incident register(IncidentRequest req) {
        Sector sector = sectors.findById(req.sectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found"));
        return incidents.save(new Incident(req.title(), req.description(),
                req.type(), req.severity(), sector, req.occurredOn()));
    }

    public Incident findById(Long id) {
        return incidents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }
}
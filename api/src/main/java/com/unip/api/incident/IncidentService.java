package com.unip.api.incident;

import com.unip.api.incident.dto.IncidentRequest;
import com.unip.api.sector.Sector;
import com.unip.api.sector.SectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector not found"));
        return incidents.save(new Incident(req.title(), req.description(),
                req.type(), req.severity(), sector, req.occurredOn()));
    }

    public Incident findById(Long id) {
        return incidents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }
}
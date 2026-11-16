package com.unip.api.incident;

import com.unip.api.incident.dto.IncidentRequest;
import com.unip.api.sector.Sector;
import com.unip.api.sector.SectorRepository;
import com.unip.api.shared.exception.InvalidRequestException;
import com.unip.api.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

    @Transactional
    public Incident registerRootCause(Long id, String rootCause) {
        Incident incident = findById(id);
        incident.registerRootCause(rootCause);
        return incident; // saved automatically at the end of the transaction
    }

    public Incident findById(Long id) {
        return incidents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    public List<Incident> search(IncidentStatus status, IncidentType type,
                                 Long sectorId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        return incidents.findAll(IncidentSpecs.filter(status, type, sectorId, from, to),
                Sort.by(Sort.Direction.DESC, "registeredAt"));
    }
}
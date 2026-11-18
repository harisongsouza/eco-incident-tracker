package com.unip.api.incident;

import com.unip.api.action.CorrectiveActionRepository;
import com.unip.api.incident.dto.IncidentRequest;
import com.unip.api.sector.Sector;
import com.unip.api.sector.SectorRepository;
import com.unip.api.security.CurrentUser;
import com.unip.api.shared.exception.InvalidRequestException;
import com.unip.api.shared.exception.ResourceNotFoundException;
import com.unip.api.user.Role;
import com.unip.api.user.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidents;
    private final SectorRepository sectors;
    private final CorrectiveActionRepository actions;
    private final CurrentUser currentUser;

    public IncidentService(IncidentRepository incidents, SectorRepository sectors,
                           CorrectiveActionRepository actions, CurrentUser currentUser) {
        this.incidents = incidents;
        this.sectors = sectors;
        this.actions = actions;
        this.currentUser = currentUser;
    }

    public Incident register(IncidentRequest req) {
        Sector sector = sectors.findById(req.sectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found"));
        return incidents.save(new Incident(req.title(), req.description(),
                req.type(), req.severity(), sector, req.occurredOn(), currentUser.get()));
    }

    /** No permission check: used internally and by officer-only operations. */
    public Incident findById(Long id) {
        return incidents.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found"));
    }

    /** For reads: an employee only sees own or assigned incidents (404 hides existence). */
    public Incident findVisible(Long id) {
        Incident incident = findById(id);
        User user = currentUser.get();
        if (user.getRole() == Role.EMPLOYEE
                && !incident.getRegisteredBy().getId().equals(user.getId())
                && !actions.existsByIncidentIdAndResponsibleId(id, user.getId())) {
            throw new ResourceNotFoundException("Incident not found");
        }
        return incident;
    }

    @Transactional
    public Incident registerRootCause(Long id, String rootCause) {
        Incident incident = findById(id);
        incident.registerRootCause(rootCause);
        return incident;
    }

    public List<Incident> search(IncidentStatus status, IncidentType type,
                                 Long sectorId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' must not be after 'to'");
        }
        Specification<Incident> spec = IncidentSpecs.filter(status, type, sectorId, from, to)
                .and(IncidentSpecs.visibleTo(currentUser.get()));
        return incidents.findAll(spec, Sort.by(Sort.Direction.DESC, "registeredAt"));
    }
}
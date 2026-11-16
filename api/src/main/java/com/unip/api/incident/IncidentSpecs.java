package com.unip.api.incident;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class IncidentSpecs {

    private IncidentSpecs() {}

    public static Specification<Incident> filter(IncidentStatus status, IncidentType type,
                                                 Long sectorId, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (sectorId != null) predicates.add(cb.equal(root.get("sector").get("id"), sectorId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("occurredOn"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("occurredOn"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
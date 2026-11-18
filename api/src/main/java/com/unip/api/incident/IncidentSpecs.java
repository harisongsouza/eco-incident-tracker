package com.unip.api.incident;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.unip.api.action.CorrectiveAction;
import com.unip.api.user.Role;
import com.unip.api.user.User;

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

    public static Specification<Incident> visibleTo(User user) {
        return (root, query, cb) -> {
            if (user.getRole() != Role.EMPLOYEE) {
                return cb.conjunction(); // officers and managers see everything
            }
            Subquery<Long> assigned = query.subquery(Long.class);
            Root<CorrectiveAction> action = assigned.from(CorrectiveAction.class);
            assigned.select(action.<Long>get("id")).where(
                    cb.equal(action.get("incident"), root),
                    cb.equal(action.get("responsible"), user));

            return cb.or(cb.equal(root.get("registeredBy"), user), cb.exists(assigned));
        };
    }
}
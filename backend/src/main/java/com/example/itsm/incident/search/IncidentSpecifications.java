package com.example.itsm.incident.search;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.sla.SlaStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> build(
            IncidentQuery query,
            Long effectiveReporterId,
            Long effectiveTechnicianId,
            Instant now
    ) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.search() != null && !query.search().isBlank()) {
                String pattern = "%" + query.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("reference")), pattern)
                ));
            }

            if (query.status() != null) {
                predicates.add(builder.equal(root.get("status"), query.status()));
            }
            if (query.priority() != null) {
                predicates.add(builder.equal(root.get("priority"), query.priority()));
            }
            if (query.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), query.categoryId()));
            }
            if (query.serviceId() != null) {
                predicates.add(builder.equal(root.get("service").get("id"), query.serviceId()));
            }

            Long technicianId = effectiveTechnicianId != null ? effectiveTechnicianId : query.technicianId();
            if (technicianId != null) {
                predicates.add(builder.equal(root.get("assignedTechnician").get("id"), technicianId));
            }

            Long reporterId = effectiveReporterId != null ? effectiveReporterId : query.reporterId();
            if (reporterId != null) {
                predicates.add(builder.equal(root.get("reporter").get("id"), reporterId));
            }

            if (query.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), query.to()));
            }
            if (query.duplicatesOnly()) {
                predicates.add(builder.isNotNull(root.get("duplicateOf")));
            }
            if (query.escalatedOnly()) {
                predicates.add(builder.greaterThan(root.get("escalationLevel"), 0));
            }

            if (query.slaStatus() != null) {
                predicates.add(slaPredicate(query.slaStatus(), root, builder, now));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate slaPredicate(
            SlaStatus slaStatus,
            jakarta.persistence.criteria.Root<Incident> root,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            Instant now
    ) {
        Predicate responseBreached = builder.isTrue(root.get("responseBreached"));
        Predicate resolutionBreached = builder.isTrue(root.get("resolutionBreached"));
        Predicate responseOverdue = builder.and(
                builder.isNull(root.get("acknowledgedAt")),
                builder.lessThan(root.get("responseDueAt"), now)
        );
        Predicate resolutionOverdue = builder.and(
                builder.isNull(root.get("resolvedAt")),
                builder.lessThan(root.get("resolutionDueAt"), now)
        );
        Predicate breached = builder.or(responseBreached, resolutionBreached, responseOverdue, resolutionOverdue);

        Predicate unresolved = builder.not(root.get("status").in(IncidentStatusGroups.RESOLVED_STATUSES));
        Predicate resolved = root.get("status").in(IncidentStatusGroups.RESOLVED_STATUSES);

        Predicate responseWarning = builder.and(
                builder.isNull(root.get("acknowledgedAt")),
                builder.lessThan(root.get("responseWarningAt"), now)
        );
        Predicate resolutionWarning = builder.and(
                builder.isNull(root.get("resolvedAt")),
                builder.lessThan(root.get("resolutionWarningAt"), now)
        );
        Predicate atRisk = builder.and(
                builder.not(breached),
                builder.or(responseWarning, resolutionWarning)
        );

        return switch (slaStatus) {
            case BREACHED -> breached;
            case AT_RISK -> builder.and(unresolved, atRisk);
            case ON_TRACK -> builder.and(unresolved, builder.not(breached), builder.not(atRisk));
            case MET -> builder.and(
                    resolved,
                    builder.isFalse(root.get("responseBreached")),
                    builder.isFalse(root.get("resolutionBreached"))
            );
        };
    }
}

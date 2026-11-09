package com.example.itsm.audit;

import com.example.itsm.incident.IncidentStatus;

import java.time.Instant;

public record AuditResponse(
        Long id,
        Long incidentId,
        String action,
        String actorName,
        String fromState,
        String toState,
        String description,
        Instant createdAt,
        String incidentReference
) {

    public static AuditResponse from(IncidentAudit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getIncident().getId(),
                audit.getAction().name(),
                audit.getActor() == null ? "System" : audit.getActor().getName(),
                audit.getFromState() == null ? null : audit.getFromState().name(),
                audit.getToState() == null ? null : audit.getToState().name(),
                audit.getDescription(),
                audit.getCreatedAt(),
                audit.getIncident().getReference()
        );
    }
}

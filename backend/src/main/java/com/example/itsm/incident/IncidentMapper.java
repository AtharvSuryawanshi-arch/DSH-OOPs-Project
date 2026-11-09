package com.example.itsm.incident;

import com.example.itsm.audit.AuditResponse;
import com.example.itsm.incident.dto.CommentResponse;
import com.example.itsm.incident.dto.IncidentResponse;
import com.example.itsm.sla.SlaStatusResolver;
import com.example.itsm.user.Employee;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

@Component
public class IncidentMapper {

    private final SlaStatusResolver slaStatusResolver;
    private final Clock clock;

    public IncidentMapper(SlaStatusResolver slaStatusResolver, Clock clock) {
        this.slaStatusResolver = slaStatusResolver;
        this.clock = clock;
    }

    public IncidentResponse toResponse(Incident incident) {
        return toResponse(incident, null, null);
    }

    public IncidentResponse toResponse(
            Incident incident,
            List<CommentResponse> comments,
            List<AuditResponse> audit
    ) {
        String department = incident.getReporter() instanceof Employee employee
                ? employee.getDepartment()
                : null;

        return new IncidentResponse(
                incident.getId(),
                incident.getReference(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getStatus().getLabel(),
                incident.getPriority().name(),
                incident.getPriority().getLabel(),
                incident.getPriorityScore(),
                incident.getPriorityRationale(),
                incident.getImpact().name(),
                incident.getUrgency().name(),
                incident.getCategory().getId(),
                incident.getCategory().getName(),
                incident.getService().getId(),
                incident.getService().getName(),
                incident.getService().getBusinessCriticality().name(),
                incident.getReporter().getId(),
                incident.getReporter().getName(),
                incident.getReporter().getEmail(),
                department,
                incident.getAssignedTechnician() == null ? null : incident.getAssignedTechnician().getId(),
                incident.getAssignedTechnician() == null ? null : incident.getAssignedTechnician().getName(),
                incident.getAffectedUsers(),
                incident.isAutoClassified(),
                incident.getClassificationConfidence(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getClosedAt(),
                incident.getResponseDueAt(),
                incident.getResolutionDueAt(),
                slaStatusResolver.resolve(incident, clock.instant()).name(),
                incident.isResponseBreached(),
                incident.isResolutionBreached(),
                incident.getEscalationLevel(),
                incident.getDuplicateOf() == null ? null : incident.getDuplicateOf().getReference(),
                incident.getDuplicateScore(),
                incident.getResolutionNotes(),
                incident.getClosureNotes(),
                incident.getFeedbackRating(),
                incident.getFeedbackComment(),
                incident.getVersion(),
                comments,
                audit
        );
    }
}

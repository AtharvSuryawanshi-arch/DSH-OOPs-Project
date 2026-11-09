package com.example.itsm.incident.dto;

import com.example.itsm.audit.AuditResponse;

import java.time.Instant;
import java.util.List;

public record IncidentResponse(
        Long id,
        String reference,
        String title,
        String description,
        String status,
        String statusLabel,
        String priority,
        String priorityLabel,
        int priorityScore,
        String priorityRationale,
        String impact,
        String urgency,
        Long categoryId,
        String categoryName,
        Long serviceId,
        String serviceName,
        String serviceCriticality,
        Long reporterId,
        String reporterName,
        String reporterEmail,
        String reporterDepartment,
        Long technicianId,
        String technicianName,
        int affectedUsers,
        boolean autoClassified,
        Double classificationConfidence,
        Instant createdAt,
        Instant updatedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant responseDueAt,
        Instant resolutionDueAt,
        String slaStatus,
        boolean responseBreached,
        boolean resolutionBreached,
        int escalationLevel,
        String duplicateOfReference,
        Double duplicateScore,
        String resolutionNotes,
        String closureNotes,
        Integer feedbackRating,
        String feedbackComment,
        long version,
        List<CommentResponse> comments,
        List<AuditResponse> audit
) {
}

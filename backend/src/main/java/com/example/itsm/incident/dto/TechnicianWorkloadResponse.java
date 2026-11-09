package com.example.itsm.incident.dto;

import java.time.Instant;
import java.util.List;

public record TechnicianWorkloadResponse(
        Long technicianId,
        String technicianName,
        int activeIncidents,
        Integer maxCapacity,
        boolean available,
        int assignedOpen,
        int resolvedByTechnician,
        List<IncidentResponse> workItems,
        Instant generatedAt
) {
}

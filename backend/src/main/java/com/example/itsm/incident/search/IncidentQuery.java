package com.example.itsm.incident.search;

import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.Priority;
import com.example.itsm.sla.SlaStatus;

import java.time.Instant;

public record IncidentQuery(
        String search,
        IncidentStatus status,
        Priority priority,
        Long categoryId,
        Long serviceId,
        Long technicianId,
        Long reporterId,
        SlaStatus slaStatus,
        Instant from,
        Instant to,
        boolean duplicatesOnly,
        boolean escalatedOnly,
        int page,
        int size
) {
    public IncidentQuery {
        size = Math.min(Math.max(size, 1), 100);
        page = Math.max(page, 0);
    }
}

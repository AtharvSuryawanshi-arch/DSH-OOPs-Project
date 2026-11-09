package com.example.itsm.incident.dto;

import java.time.Instant;

public record DuplicateResponse(
        Long incidentId,
        String reference,
        String title,
        String status,
        String priority,
        double score,
        Instant createdAt
) {
}

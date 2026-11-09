package com.example.itsm.intelligence;

import java.time.Instant;

public record ResolutionSuggestion(
        Long incidentId,
        String reference,
        String title,
        String resolutionNotes,
        Instant resolvedAt,
        double score
) {
}

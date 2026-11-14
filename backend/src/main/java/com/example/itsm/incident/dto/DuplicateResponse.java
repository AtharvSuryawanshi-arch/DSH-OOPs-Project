package com.example.itsm.incident.dto;

import com.example.itsm.intelligence.DuplicateMatch;

import java.time.Instant;

/**
 * A possible duplicate. When the viewer may not open the matching incident, its id and title are
 * withheld ({@code accessible = false}): the viewer still learns that a similar report exists, but
 * not what someone else wrote.
 */
public record DuplicateResponse(
        Long incidentId,
        String reference,
        String title,
        String status,
        String priority,
        double score,
        Instant createdAt,
        boolean accessible
) {

    public static DuplicateResponse of(DuplicateMatch match, boolean accessible) {
        var incident = match.incident();
        return new DuplicateResponse(
                accessible ? incident.getId() : null,
                incident.getReference(),
                accessible ? incident.getTitle() : null,
                incident.getStatus().name(),
                incident.getPriority().name(),
                match.score(),
                incident.getCreatedAt(),
                accessible
        );
    }
}

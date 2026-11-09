package com.example.itsm.sla;

import java.time.Instant;

public record SlaDeadlines(
        Instant responseDueAt,
        Instant resolutionDueAt,
        Instant responseWarningAt,
        Instant resolutionWarningAt
) {
}

package com.example.itsm.incident;

import java.util.Set;

public final class IncidentStatusGroups {

    public static final Set<IncidentStatus> UNRESOLVED = Set.of(
            IncidentStatus.NEW,
            IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
    );

    public static final Set<IncidentStatus> ACTIVE_WORK = Set.of(
            IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
    );

    public static final Set<IncidentStatus> RESOLVED_STATUSES = Set.of(
            IncidentStatus.RESOLVED,
            IncidentStatus.VERIFIED,
            IncidentStatus.CLOSED
    );

    public static final Set<IncidentStatus> DUPLICATE_SCOPE = Set.of(
            IncidentStatus.NEW,
            IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS,
            IncidentStatus.RESOLVED
    );

    public static final Set<IncidentStatus> TERMINAL = Set.of(
            IncidentStatus.CLOSED,
            IncidentStatus.CANCELLED
    );

    private IncidentStatusGroups() {
    }
}

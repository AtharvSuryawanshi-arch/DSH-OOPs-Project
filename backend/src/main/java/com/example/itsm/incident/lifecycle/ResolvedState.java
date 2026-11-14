package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;

import java.time.Instant;

/** The technician says it is fixed; waiting for the reporter to confirm. */
class ResolvedState extends AbstractIncidentState {

    ResolvedState() {
        super(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void verify(Incident incident) {
        moveTo(incident, IncidentStatus.VERIFIED);
    }

    /** Still broken: back to investigation. The resolution deadline stays the original one. */
    @Override
    public void reopen(Incident incident, Instant at) {
        boolean breached = incident.getResolutionDueAt() != null && at.isAfter(incident.getResolutionDueAt());
        incident.reopen(breached);
        moveTo(incident, IncidentStatus.IN_PROGRESS);
    }
}

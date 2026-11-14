package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;

/** Being investigated. Work has started, so it can no longer be cancelled. */
class InProgressState extends AbstractIncidentState {

    InProgressState() {
        super(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED);
    }

    /** Reassignment: a different technician, same status. */
    @Override
    public void assign(Incident incident, Technician technician) {
        incident.assignTechnician(technician);
    }

    /** Records the fix and whether the resolution target was missed. */
    @Override
    public void resolve(Incident incident, String resolutionNotes, Instant at) {
        boolean breached = incident.isResolutionBreached()
                || (incident.getResolutionDueAt() != null && at.isAfter(incident.getResolutionDueAt()));
        incident.resolve(resolutionNotes, at, breached);
        moveTo(incident, IncidentStatus.RESOLVED);
    }
}

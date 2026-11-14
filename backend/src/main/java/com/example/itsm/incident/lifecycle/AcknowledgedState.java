package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;

/** The technician has picked it up; investigation has not started yet. */
class AcknowledgedState extends AbstractIncidentState {

    AcknowledgedState() {
        super(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.CANCELLED);
    }

    /** Reassignment: a different technician, same status. */
    @Override
    public void assign(Incident incident, Technician technician) {
        incident.assignTechnician(technician);
    }

    @Override
    public void start(Incident incident) {
        moveTo(incident, IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void cancel(Incident incident, String reason, Instant at) {
        moveTo(incident, IncidentStatus.CANCELLED);
        incident.close(reason, at);
    }
}

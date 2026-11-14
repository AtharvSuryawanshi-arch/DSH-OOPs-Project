package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;

/** A technician has it but has not picked it up yet; the response SLA clock is running. */
class AssignedState extends AbstractIncidentState {

    AssignedState() {
        super(IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.CANCELLED);
    }

    /** Reassignment: a different technician, same status. */
    @Override
    public void assign(Incident incident, Technician technician) {
        incident.assignTechnician(technician);
    }

    /** Records the response time and whether the response target was missed. */
    @Override
    public void acknowledge(Incident incident, Instant at) {
        boolean breached = incident.isResponseBreached()
                || (incident.getResponseDueAt() != null && at.isAfter(incident.getResponseDueAt()));
        incident.acknowledge(at, breached);
        moveTo(incident, IncidentStatus.ACKNOWLEDGED);
    }

    @Override
    public void cancel(Incident incident, String reason, Instant at) {
        moveTo(incident, IncidentStatus.CANCELLED);
        incident.close(reason, at);
    }
}

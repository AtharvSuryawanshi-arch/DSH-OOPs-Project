package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;

/** Categorised and prioritised; waiting for a technician. */
class ClassifiedState extends AbstractIncidentState {

    ClassifiedState() {
        super(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED, IncidentStatus.CANCELLED);
    }

    @Override
    public void assign(Incident incident, Technician technician) {
        incident.assignTechnician(technician);
        moveTo(incident, IncidentStatus.ASSIGNED);
    }

    @Override
    public void cancel(Incident incident, String reason, Instant at) {
        moveTo(incident, IncidentStatus.CANCELLED);
        incident.close(reason, at);
    }
}

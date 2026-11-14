package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;

import java.time.Instant;

/** Just reported; triage has not run yet. */
class NewState extends AbstractIncidentState {

    NewState() {
        super(IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.CANCELLED);
    }

    @Override
    public void classify(Incident incident) {
        moveTo(incident, IncidentStatus.CLASSIFIED);
    }

    @Override
    public void cancel(Incident incident, String reason, Instant at) {
        moveTo(incident, IncidentStatus.CANCELLED);
        incident.close(reason, at);
    }
}

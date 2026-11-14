package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;

import java.time.Instant;

/** The reporter confirmed the fix; the service desk can close it. */
class VerifiedState extends AbstractIncidentState {

    VerifiedState() {
        super(IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
    }

    @Override
    public void close(Incident incident, String closureNotes, Instant at) {
        incident.close(closureNotes, at);
        moveTo(incident, IncidentStatus.CLOSED);
    }
}

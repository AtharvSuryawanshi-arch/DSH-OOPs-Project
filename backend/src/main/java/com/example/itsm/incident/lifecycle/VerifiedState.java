package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class VerifiedState extends AbstractIncidentState {

    VerifiedState() {
        super(IncidentStatus.VERIFIED, IncidentStatus.CLOSED);
    }
}

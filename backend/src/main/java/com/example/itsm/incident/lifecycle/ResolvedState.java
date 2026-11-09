package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class ResolvedState extends AbstractIncidentState {

    ResolvedState() {
        super(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS);
    }
}

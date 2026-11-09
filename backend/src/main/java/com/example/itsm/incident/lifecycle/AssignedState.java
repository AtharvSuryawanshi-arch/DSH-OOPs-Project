package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class AssignedState extends AbstractIncidentState {

    AssignedState() {
        super(IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED, IncidentStatus.CANCELLED);
    }
}

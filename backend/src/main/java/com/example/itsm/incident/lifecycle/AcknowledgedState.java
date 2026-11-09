package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class AcknowledgedState extends AbstractIncidentState {

    AcknowledgedState() {
        super(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS, IncidentStatus.CANCELLED);
    }
}

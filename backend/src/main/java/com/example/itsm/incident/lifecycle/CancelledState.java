package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class CancelledState extends AbstractIncidentState {

    CancelledState() {
        super(IncidentStatus.CANCELLED);
    }
}

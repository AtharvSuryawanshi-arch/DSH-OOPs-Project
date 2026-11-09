package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class ClosedState extends AbstractIncidentState {

    ClosedState() {
        super(IncidentStatus.CLOSED);
    }
}

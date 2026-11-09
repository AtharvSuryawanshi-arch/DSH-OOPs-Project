package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class NewState extends AbstractIncidentState {

    NewState() {
        super(IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.CANCELLED);
    }
}

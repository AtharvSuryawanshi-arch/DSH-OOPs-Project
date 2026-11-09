package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class ClassifiedState extends AbstractIncidentState {

    ClassifiedState() {
        super(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED, IncidentStatus.CANCELLED);
    }
}

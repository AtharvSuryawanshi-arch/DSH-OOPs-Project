package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

class InProgressState extends AbstractIncidentState {

    InProgressState() {
        super(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED);
    }
}

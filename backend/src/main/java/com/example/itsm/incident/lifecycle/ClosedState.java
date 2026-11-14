package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

/** Final state: every action is refused, as inherited from {@link AbstractIncidentState}. */
class ClosedState extends AbstractIncidentState {

    ClosedState() {
        super(IncidentStatus.CLOSED);
    }
}

package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

import java.util.Set;

abstract class AbstractIncidentState implements IncidentState {

    private final IncidentStatus status;
    private final Set<IncidentStatus> allowedTransitions;

    protected AbstractIncidentState(IncidentStatus status, IncidentStatus... allowedTransitions) {
        this.status = status;
        this.allowedTransitions = Set.of(allowedTransitions);
    }

    @Override
    public IncidentStatus status() {
        return status;
    }

    @Override
    public Set<IncidentStatus> allowedTransitions() {
        return allowedTransitions;
    }
}

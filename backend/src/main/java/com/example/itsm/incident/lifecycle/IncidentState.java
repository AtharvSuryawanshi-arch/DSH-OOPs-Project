package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.IncidentStatus;

import java.util.Set;

public interface IncidentState {

    IncidentStatus status();

    Set<IncidentStatus> allowedTransitions();

    default boolean canTransitionTo(IncidentStatus target) {
        return allowedTransitions().contains(target);
    }
}

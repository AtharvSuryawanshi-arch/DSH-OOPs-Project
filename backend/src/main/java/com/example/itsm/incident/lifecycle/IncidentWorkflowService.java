package com.example.itsm.incident.lifecycle;

import com.example.itsm.common.InvalidStateTransitionException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import org.springframework.stereotype.Service;

@Service
public class IncidentWorkflowService {

    private final IncidentStateFactory stateFactory;

    public IncidentWorkflowService(IncidentStateFactory stateFactory) {
        this.stateFactory = stateFactory;
    }

    public void validate(Incident incident, IncidentStatus target) {
        IncidentState state = stateFactory.stateFor(incident.getStatus());
        if (!state.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(incident.getStatus().name(), target.name());
        }
    }

    public void changeStatus(Incident incident, IncidentStatus target) {
        validate(incident, target);
        incident.changeStatus(target);
    }
}

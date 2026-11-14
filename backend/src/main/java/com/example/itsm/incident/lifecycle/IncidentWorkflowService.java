package com.example.itsm.incident.lifecycle;

import com.example.itsm.common.InvalidStateTransitionException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * The State pattern's "context": looks up the incident's current state and asks it to perform an action.
 * Each method is one line on purpose; what actually happens is decided by the state class.
 */
@Service
public class IncidentWorkflowService {

    private final IncidentStateFactory stateFactory;

    public IncidentWorkflowService(IncidentStateFactory stateFactory) {
        this.stateFactory = stateFactory;
    }

    public IncidentState stateOf(Incident incident) {
        return stateFactory.stateFor(incident.getStatus());
    }

    public void classify(Incident incident) {
        stateOf(incident).classify(incident);
    }

    public void assign(Incident incident, Technician technician) {
        stateOf(incident).assign(incident, technician);
    }

    public void acknowledge(Incident incident, Instant at) {
        stateOf(incident).acknowledge(incident, at);
    }

    public void start(Incident incident) {
        stateOf(incident).start(incident);
    }

    public void resolve(Incident incident, String resolutionNotes, Instant at) {
        stateOf(incident).resolve(incident, resolutionNotes, at);
    }

    public void verify(Incident incident) {
        stateOf(incident).verify(incident);
    }

    public void reopen(Incident incident, Instant at) {
        stateOf(incident).reopen(incident, at);
    }

    public void close(Incident incident, String closureNotes, Instant at) {
        stateOf(incident).close(incident, closureNotes, at);
    }

    public void cancel(Incident incident, String reason, Instant at) {
        stateOf(incident).cancel(incident, reason, at);
    }

    /** Low-level guarded status change, kept for callers that only need the transition check. */
    public void changeStatus(Incident incident, IncidentStatus target) {
        if (!stateOf(incident).canTransitionTo(target)) {
            throw new InvalidStateTransitionException(incident.getStatus().name(), target.name());
        }
        incident.changeStatus(target);
    }
}

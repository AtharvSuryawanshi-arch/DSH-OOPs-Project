package com.example.itsm.incident.lifecycle;

import com.example.itsm.common.InvalidStateTransitionException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;
import java.util.Set;

/**
 * Base class for the concrete states. By default every action is refused; each concrete state
 * overrides only the actions that make sense in that state. An illegal step such as closing a NEW
 * incident therefore cannot happen: no state implements it.
 */
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

    @Override
    public void classify(Incident incident) {
        throw refuse("classify");
    }

    @Override
    public void assign(Incident incident, Technician technician) {
        throw refuse("assign");
    }

    @Override
    public void acknowledge(Incident incident, Instant at) {
        throw refuse("acknowledge");
    }

    @Override
    public void start(Incident incident) {
        throw refuse("start work on");
    }

    @Override
    public void resolve(Incident incident, String resolutionNotes, Instant at) {
        throw refuse("resolve");
    }

    @Override
    public void verify(Incident incident) {
        throw refuse("verify");
    }

    @Override
    public void reopen(Incident incident, Instant at) {
        throw refuse("reopen");
    }

    @Override
    public void close(Incident incident, String closureNotes, Instant at) {
        throw refuse("close");
    }

    @Override
    public void cancel(Incident incident, String reason, Instant at) {
        throw refuse("cancel");
    }

    /** Moves the incident on, double-checking that the target is one this state allows. */
    protected void moveTo(Incident incident, IncidentStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidStateTransitionException(status.name(), target.name());
        }
        incident.changeStatus(target);
    }

    private InvalidStateTransitionException refuse(String action) {
        return InvalidStateTransitionException.forAction(action, status.getLabel());
    }
}

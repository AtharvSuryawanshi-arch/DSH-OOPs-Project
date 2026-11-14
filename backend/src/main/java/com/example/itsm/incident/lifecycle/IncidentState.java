package com.example.itsm.incident.lifecycle;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.Technician;

import java.time.Instant;
import java.util.Set;

/**
 * State pattern: one implementation per {@link IncidentStatus}.
 *
 * <p>Every lifecycle action is a method here. The incident's current state decides what the action
 * does: a state that supports it updates the incident and moves it on; every other state refuses with
 * an {@link com.example.itsm.common.InvalidStateTransitionException}. Services never check
 * {@code if (status == ...)} themselves; they ask the current state to perform the action.
 */
public interface IncidentState {

    IncidentStatus status();

    /** The statuses this state can move to (used as a safety check, in tests and in the docs). */
    Set<IncidentStatus> allowedTransitions();

    default boolean canTransitionTo(IncidentStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Triage has categorised and prioritised the incident. */
    void classify(Incident incident);

    /** Give the incident to a technician, or hand it to a different one. */
    void assign(Incident incident, Technician technician);

    /** The assigned technician confirms they have picked it up. */
    void acknowledge(Incident incident, Instant at);

    /** The technician starts investigating. */
    void start(Incident incident);

    /** The technician records a fix. */
    void resolve(Incident incident, String resolutionNotes, Instant at);

    /** The reporter confirms the fix works. */
    void verify(Incident incident);

    /** The reporter says it is still broken: back to investigation. */
    void reopen(Incident incident, Instant at);

    /** The service desk closes a verified incident. */
    void close(Incident incident, String closureNotes, Instant at);

    /** The service desk cancels an incident before investigation has started. */
    void cancel(Incident incident, String reason, Instant at);
}

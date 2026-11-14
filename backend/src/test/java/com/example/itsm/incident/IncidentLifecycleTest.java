package com.example.itsm.incident;

import com.example.itsm.support.TestIncidents;
import com.example.itsm.incident.lifecycle.IncidentStateFactory;
import com.example.itsm.incident.lifecycle.IncidentWorkflowService;
import com.example.itsm.common.InvalidStateTransitionException;
import com.example.itsm.user.Technician;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IncidentLifecycleTest {

    private final IncidentStateFactory factory = new IncidentStateFactory();
    private final IncidentWorkflowService workflow = new IncidentWorkflowService(factory);

    @Test
    void newIncidentCanBeClassifiedButNotClosed() {
        assertThat(factory.stateFor(IncidentStatus.NEW).canTransitionTo(IncidentStatus.CLASSIFIED)).isTrue();
        assertThat(factory.stateFor(IncidentStatus.NEW).canTransitionTo(IncidentStatus.CLOSED)).isFalse();
    }

    @Test
    void assignedIncidentCanOnlyBeAcknowledged() {
        assertThat(factory.stateFor(IncidentStatus.ASSIGNED).canTransitionTo(IncidentStatus.ACKNOWLEDGED)).isTrue();
        assertThat(factory.stateFor(IncidentStatus.ASSIGNED).canTransitionTo(IncidentStatus.RESOLVED)).isFalse();
    }

    @Test
    void resolvedIncidentCanBeVerifiedOrReopened() {
        assertThat(factory.stateFor(IncidentStatus.RESOLVED).canTransitionTo(IncidentStatus.VERIFIED)).isTrue();
        assertThat(factory.stateFor(IncidentStatus.RESOLVED).canTransitionTo(IncidentStatus.IN_PROGRESS)).isTrue();
        assertThat(factory.stateFor(IncidentStatus.RESOLVED).canTransitionTo(IncidentStatus.CLOSED)).isFalse();
    }

    @Test
    void closedIncidentHasNoTransitions() {
        assertThat(factory.stateFor(IncidentStatus.CLOSED).allowedTransitions()).isEmpty();
    }

    @Test
    void workflowRejectsInvalidTransitionAndAllowsValidOne() {
        Incident incident = TestIncidents.unclassified();

        assertThatThrownBy(() -> workflow.changeStatus(incident, IncidentStatus.CLOSED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("NEW")
                .hasMessageContaining("CLOSED");

        workflow.changeStatus(incident, IncidentStatus.CLASSIFIED);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
    }

    @Test
    void eachStateDecidesWhatAnActionDoes() {
        Incident incident = TestIncidents.unclassified();
        Technician first = mock(Technician.class);
        Technician second = mock(Technician.class);
        Instant now = TestIncidents.CREATED.plusSeconds(60);

        // A NEW incident cannot be verified or closed: those states refuse.
        assertThatThrownBy(() -> workflow.verify(incident))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("You cannot verify an incident that is New.");
        assertThatThrownBy(() -> workflow.close(incident, "done", now))
                .isInstanceOf(InvalidStateTransitionException.class);

        workflow.classify(incident);
        // Same assign() call, different behaviour: ClassifiedState assigns and moves on...
        workflow.assign(incident, first);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        // ...while AssignedState only hands it to someone else.
        workflow.assign(incident, second);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(incident.getAssignedTechnician()).isSameAs(second);

        assertThatThrownBy(() -> workflow.resolve(incident, "too early", now))
                .hasMessage("You cannot resolve an incident that is Assigned.");

        workflow.acknowledge(incident, now);
        assertThat(incident.getAcknowledgedAt()).isEqualTo(now);
        workflow.start(incident);
        // Investigation has started, so it can no longer be cancelled.
        assertThatThrownBy(() -> workflow.cancel(incident, "no longer needed", now))
                .hasMessage("You cannot cancel an incident that is In progress.");

        workflow.resolve(incident, "Restarted the service.", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // The reporter says it is still broken: ResolvedState sends it back to investigation.
        workflow.reopen(incident, now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);

        workflow.resolve(incident, "Replaced the faulty node.", now);
        workflow.verify(incident);
        workflow.close(incident, "Confirmed by the reporter.", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThatThrownBy(() -> workflow.reopen(incident, now))
                .hasMessage("You cannot reopen an incident that is Closed.");
    }
}

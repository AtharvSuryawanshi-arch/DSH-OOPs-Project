package com.example.itsm.incident;

import com.example.itsm.support.TestIncidents;
import com.example.itsm.incident.lifecycle.IncidentStateFactory;
import com.example.itsm.incident.lifecycle.IncidentWorkflowService;
import com.example.itsm.common.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}

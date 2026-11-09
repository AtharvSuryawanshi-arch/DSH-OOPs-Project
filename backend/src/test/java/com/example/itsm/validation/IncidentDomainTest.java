package com.example.itsm.validation;

import com.example.itsm.incident.Incident;
import com.example.itsm.support.TestIncidents;
import com.example.itsm.user.Employee;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentDomainTest {

    @Test
    void acknowledgeStoresTimestampAndBreachFlag() {
        Incident incident = TestIncidents.unclassified();
        Instant at = TestIncidents.CREATED.plusSeconds(120);

        incident.acknowledge(at, true);

        assertThat(incident.getAcknowledgedAt()).isEqualTo(at);
        assertThat(incident.isResponseBreached()).isTrue();
    }

    @Test
    void resolveStoresNotesAndTimestamp() {
        Incident incident = TestIncidents.unclassified();
        Instant at = TestIncidents.CREATED.plusSeconds(900);

        incident.resolve("Restarted the service", at, false);

        assertThat(incident.getResolvedAt()).isEqualTo(at);
        assertThat(incident.getResolutionNotes()).isEqualTo("Restarted the service");
        assertThat(incident.isResolutionBreached()).isFalse();
    }

    @Test
    void feedbackIsStoredOnTheIncident() {
        Incident incident = TestIncidents.unclassified();

        incident.recordFeedback(5, "Excellent support");

        assertThat(incident.getFeedbackRating()).isEqualTo(5);
        assertThat(incident.getFeedbackComment()).isEqualTo("Excellent support");
    }

    @Test
    void duplicateLinkIsRecordedWithSimilarity() {
        Incident original = TestIncidents.unclassified();
        Incident duplicate = Incident.create(
                "INC-TEST-0002",
                "Payment app down",
                "Checkout failures",
                new Employee("Reporter Two", "two@example.test", "hash", null, "Ops"),
                original.getCategory(),
                original.getService(),
                original.getImpact(),
                original.getUrgency(),
                25,
                TestIncidents.CREATED.plusSeconds(60)
        );

        duplicate.markDuplicate(original, 0.81);

        assertThat(duplicate.getDuplicateOf()).isSameAs(original);
        assertThat(duplicate.getDuplicateScore()).isEqualTo(0.81);
    }
}

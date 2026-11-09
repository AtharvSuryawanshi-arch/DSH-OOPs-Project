package com.example.itsm.sla;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.support.TestIncidents;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SlaStatusResolverTest {

    private final SlaStatusResolver resolver = new SlaStatusResolver();
    private final Instant base = Instant.parse("2026-11-09T10:00:00Z");

    private Incident incident(SlaDeadlines deadlines) {
        Incident incident = TestIncidents.unclassified();
        incident.applySla(deadlines);
        incident.changeStatus(IncidentStatus.CLASSIFIED);
        return incident;
    }

    @Test
    void reportsOnTrackBeforeWarningWindow() {
        Incident incident = incident(new SlaDeadlines(
                base.plusSeconds(300),
                base.plusSeconds(1800),
                base.plusSeconds(240),
                base.plusSeconds(1440)
        ));

        assertThat(resolver.resolve(incident, base.plusSeconds(60))).isEqualTo(SlaStatus.ON_TRACK);
    }

    @Test
    void reportsAtRiskAfterWarningWindow() {
        Incident incident = incident(new SlaDeadlines(
                base.plusSeconds(300),
                base.plusSeconds(1800),
                base.plusSeconds(240),
                base.plusSeconds(1440)
        ));

        assertThat(resolver.resolve(incident, base.plusSeconds(250))).isEqualTo(SlaStatus.AT_RISK);
    }

    @Test
    void reportsBreachedAfterResponseDeadlineWithoutAcknowledgement() {
        Incident incident = incident(new SlaDeadlines(
                base.plusSeconds(300),
                base.plusSeconds(1800),
                base.plusSeconds(240),
                base.plusSeconds(1440)
        ));

        assertThat(resolver.resolve(incident, base.plusSeconds(301))).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void reportsMetWhenResolvedWithinTarget() {
        Incident incident = incident(new SlaDeadlines(
                base.plusSeconds(300),
                base.plusSeconds(1800),
                base.plusSeconds(240),
                base.plusSeconds(1440)
        ));
        incident.acknowledge(base.plusSeconds(120), false);
        incident.resolve("Fixed", base.plusSeconds(900), false);
        incident.changeStatus(IncidentStatus.RESOLVED);

        assertThat(resolver.resolve(incident, base.plusSeconds(1000))).isEqualTo(SlaStatus.MET);
    }

    @Test
    void reportsBreachedWhenResolvedAfterTarget() {
        Incident incident = incident(new SlaDeadlines(
                base.plusSeconds(300),
                base.plusSeconds(1800),
                base.plusSeconds(240),
                base.plusSeconds(1440)
        ));
        incident.acknowledge(base.plusSeconds(120), false);
        incident.resolve("Fixed late", base.plusSeconds(1900), true);
        incident.changeStatus(IncidentStatus.RESOLVED);

        assertThat(resolver.resolve(incident, base.plusSeconds(2000))).isEqualTo(SlaStatus.BREACHED);
    }
}

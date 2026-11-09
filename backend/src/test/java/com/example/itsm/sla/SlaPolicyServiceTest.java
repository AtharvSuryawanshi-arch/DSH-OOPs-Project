package com.example.itsm.sla;

import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.catalog.ItService;
import com.example.itsm.incident.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SlaPolicyServiceTest {

    private final SlaPolicyService service = new SlaPolicyService(
            mock(SlaPolicyRepository.class),
            mock(com.example.itsm.catalog.CatalogService.class)
    );

    @Test
    void computesResponseResolutionAndWarningDeadlines() {
        SlaPolicy policy = new SlaPolicy(
                "P2 high",
                Priority.P2,
                null,
                15,
                120,
                80
        );
        Instant createdAt = Instant.parse("2026-11-09T10:00:00Z");

        SlaDeadlines deadlines = service.computeDeadlines(policy, createdAt);

        assertThat(Duration.between(createdAt, deadlines.responseDueAt()).toMinutes()).isEqualTo(15);
        assertThat(Duration.between(createdAt, deadlines.resolutionDueAt()).toMinutes()).isEqualTo(120);
        assertThat(Duration.between(createdAt, deadlines.responseWarningAt()).toMinutes()).isEqualTo(12);
        assertThat(Duration.between(createdAt, deadlines.resolutionWarningAt()).toMinutes()).isEqualTo(96);
    }

    @Test
    void serviceSpecificPolicyWinsOverGlobalPolicy() {
        ItService serviceEntity = new ItService(
                "PAYMENT",
                "Payment",
                "Payments",
                BusinessCriticality.CRITICAL
        );
        SlaPolicy policy = new SlaPolicy("Fast P1", Priority.P1, serviceEntity, 2, 15, 70);

        SlaDeadlines deadlines = service.computeDeadlines(
                policy,
                Instant.parse("2026-11-09T10:00:00Z")
        );

        assertThat(deadlines.responseDueAt()).isEqualTo(Instant.parse("2026-11-09T10:02:00Z"));
        assertThat(deadlines.resolutionWarningAt()).isEqualTo(Instant.parse("2026-11-09T10:10:30Z"));
    }
}

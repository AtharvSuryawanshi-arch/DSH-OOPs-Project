package com.example.itsm.incident.priority;

import com.example.itsm.catalog.BusinessCriticality;
import com.example.itsm.incident.Impact;
import com.example.itsm.incident.Priority;
import com.example.itsm.incident.Urgency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedPriorityStrategyTest {

    private final WeightedPriorityStrategy strategy = new WeightedPriorityStrategy();

    @Test
    void extremeImpactOnCriticalServiceIsP1() {
        PriorityResult result = strategy.calculate(new PriorityContext(
                Impact.EXTREME,
                Urgency.CRITICAL,
                BusinessCriticality.CRITICAL,
                500
        ));

        assertThat(result.priority()).isEqualTo(Priority.P1);
        assertThat(result.score()).isEqualTo(74);
        assertThat(result.rationale()).contains("Impact Extreme", "P1");
    }

    @Test
    void lowImpactOnLowServiceIsP4() {
        PriorityResult result = strategy.calculate(new PriorityContext(
                Impact.LOW,
                Urgency.LOW,
                BusinessCriticality.LOW,
                1
        ));

        assertThat(result.priority()).isEqualTo(Priority.P4);
        assertThat(result.score()).isEqualTo(1);
    }

    @Test
    void affectedUserBonusCanLiftPriority() {
        PriorityResult base = strategy.calculate(new PriorityContext(
                Impact.HIGH,
                Urgency.HIGH,
                BusinessCriticality.HIGH,
                1
        ));
        PriorityResult widespread = strategy.calculate(new PriorityContext(
                Impact.HIGH,
                Urgency.HIGH,
                BusinessCriticality.HIGH,
                500
        ));

        assertThat(base.priority()).isEqualTo(Priority.P2);
        assertThat(widespread.score()).isEqualTo(37);
        assertThat(widespread.priority()).isEqualTo(Priority.P1);
    }

    @Test
    void mediumIncidentFallsIntoP3Band() {
        PriorityResult result = strategy.calculate(new PriorityContext(
                Impact.MEDIUM,
                Urgency.MEDIUM,
                BusinessCriticality.MEDIUM,
                1
        ));

        assertThat(result.score()).isEqualTo(8);
        assertThat(result.priority()).isEqualTo(Priority.P3);
    }
}

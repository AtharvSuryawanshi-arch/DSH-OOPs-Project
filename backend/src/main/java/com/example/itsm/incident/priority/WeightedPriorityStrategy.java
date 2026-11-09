package com.example.itsm.incident.priority;

import com.example.itsm.incident.Priority;
import org.springframework.stereotype.Component;

@Component
public class WeightedPriorityStrategy implements PriorityStrategy {

    @Override
    public PriorityResult calculate(PriorityContext context) {
        int raw = context.impact().getWeight()
                * context.urgency().getWeight()
                * context.businessCriticality().getWeight();

        int userBonus;
        if (context.affectedUsers() >= 500) {
            userBonus = 10;
        } else if (context.affectedUsers() >= 100) {
            userBonus = 6;
        } else if (context.affectedUsers() >= 25) {
            userBonus = 3;
        } else if (context.affectedUsers() >= 5) {
            userBonus = 1;
        } else {
            userBonus = 0;
        }

        int score = raw + userBonus;
        Priority priority;
        if (score >= 36) {
            priority = Priority.P1;
        } else if (score >= 18) {
            priority = Priority.P2;
        } else if (score >= 6) {
            priority = Priority.P3;
        } else {
            priority = Priority.P4;
        }

        // A total outage of a business-critical service is always P1.
        if (context.impact().name().equals("EXTREME")
                && context.businessCriticality().name().equals("CRITICAL")
                && priority.getRank() > Priority.P1.getRank()) {
            priority = Priority.P1;
        }

        String rationale = "Impact %s (%d) × Urgency %s (%d) × Criticality %s (%d) = %d%s → score %d, %s".formatted(
                context.impact().getLabel(), context.impact().getWeight(),
                context.urgency().getLabel(), context.urgency().getWeight(),
                context.businessCriticality().getLabel(), context.businessCriticality().getWeight(),
                raw,
                userBonus > 0 ? " + " + userBonus + " for " + context.affectedUsers() + " affected users" : "",
                score,
                priority.name()
        );

        return new PriorityResult(priority, score, rationale);
    }

    @Override
    public String name() {
        return "weighted-v1";
    }
}

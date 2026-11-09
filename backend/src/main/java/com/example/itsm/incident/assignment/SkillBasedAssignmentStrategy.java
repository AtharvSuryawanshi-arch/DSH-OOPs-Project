package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class SkillBasedAssignmentStrategy implements AssignmentStrategy {

    private static final Comparator<AssignmentCandidate> BEST_MATCH = Comparator
            .comparing(AssignmentCandidate::skillMatch).reversed()
            .thenComparing(AssignmentCandidate::recommendationScore, Comparator.reverseOrder())
            .thenComparing(candidate -> candidate.technician().getId());

    @Override
    public Optional<AssignmentCandidate> select(Incident incident, List<AssignmentCandidate> candidates) {
        return candidates.stream()
                .filter(AssignmentCandidate::hasCapacity)
                .min((left, right) -> BEST_MATCH.compare(left, right));
    }

    @Override
    public AssignmentStrategyType type() {
        return AssignmentStrategyType.SKILL_BASED;
    }
}

package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class LoadBasedAssignmentStrategy implements AssignmentStrategy {

    private static final Comparator<AssignmentCandidate> LEAST_LOADED = Comparator
            .comparingLong(AssignmentCandidate::activeIncidents)
            .thenComparing(AssignmentCandidate::skillMatch, Comparator.reverseOrder())
            .thenComparing(candidate -> candidate.technician().getId());

    @Override
    public Optional<AssignmentCandidate> select(Incident incident, List<AssignmentCandidate> candidates) {
        return candidates.stream()
                .filter(AssignmentCandidate::hasCapacity)
                .min(LEAST_LOADED);
    }

    @Override
    public AssignmentStrategyType type() {
        return AssignmentStrategyType.LOAD_BASED;
    }
}

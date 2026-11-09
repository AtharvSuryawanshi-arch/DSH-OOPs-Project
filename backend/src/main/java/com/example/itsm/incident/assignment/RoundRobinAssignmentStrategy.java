package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinAssignmentStrategy implements AssignmentStrategy {

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public Optional<AssignmentCandidate> select(Incident incident, List<AssignmentCandidate> candidates) {
        List<AssignmentCandidate> available = candidates.stream()
                .filter(AssignmentCandidate::hasCapacity)
                .sorted(Comparator.comparing(candidate -> candidate.technician().getId()))
                .toList();
        if (available.isEmpty()) {
            return Optional.empty();
        }
        int index = Math.floorMod(cursor.getAndIncrement(), available.size());
        return Optional.of(available.get(index));
    }

    @Override
    public AssignmentStrategyType type() {
        return AssignmentStrategyType.ROUND_ROBIN;
    }
}

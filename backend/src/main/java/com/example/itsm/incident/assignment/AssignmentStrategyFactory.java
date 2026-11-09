package com.example.itsm.incident.assignment;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class AssignmentStrategyFactory {

    private final Map<AssignmentStrategyType, AssignmentStrategy> strategies = new EnumMap<>(AssignmentStrategyType.class);

    public AssignmentStrategyFactory(List<AssignmentStrategy> discoveredStrategies) {
        discoveredStrategies.forEach(strategy -> strategies.put(strategy.type(), strategy));
    }

    public AssignmentStrategy get(AssignmentStrategyType type) {
        AssignmentStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No assignment strategy registered for " + type);
        }
        return strategy;
    }

    public List<AssignmentStrategy> all() {
        return List.copyOf(strategies.values());
    }
}

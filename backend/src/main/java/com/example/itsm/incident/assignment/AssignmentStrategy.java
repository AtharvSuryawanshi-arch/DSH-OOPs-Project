package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;

import java.util.List;
import java.util.Optional;

public interface AssignmentStrategy {

    AssignmentStrategyType type();

    Optional<AssignmentCandidate> select(Incident incident, List<AssignmentCandidate> candidates);
}

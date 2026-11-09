package com.example.itsm.intelligence;

import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.IncidentStatusGroups;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class DuplicateDetectionService {

    private static final double CANDIDATE_THRESHOLD = 0.30;

    private final IncidentRepository incidentRepository;
    private final Clock clock;

    public DuplicateDetectionService(IncidentRepository incidentRepository, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DuplicateMatch> findMatches(
            String title,
            String description,
            Long serviceId,
            Long excludeIncidentId
    ) {
        Instant since = clock.instant().minus(30, ChronoUnit.DAYS);
        List<Incident> candidates = incidentRepository.findAllDetailed().stream()
                .filter(candidate -> IncidentStatusGroups.DUPLICATE_SCOPE.contains(candidate.getStatus()))
                .filter(candidate -> candidate.getCreatedAt() != null && candidate.getCreatedAt().isAfter(since))
                .filter(candidate -> excludeIncidentId == null || !candidate.getId().equals(excludeIncidentId))
                .toList();

        return candidates.stream()
                .map(candidate -> {
                    double score = score(title, description, serviceId, candidate);
                    return new DuplicateMatch(candidate, score);
                })
                .filter(match -> match.score() >= CANDIDATE_THRESHOLD)
                .sorted(Comparator.comparingDouble(DuplicateMatch::score).reversed())
                .limit(5)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DuplicateMatch> findMatchesFor(Long incidentId) {
        Incident incident = incidentRepository.findDetailedById(incidentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Incident", incidentId));
        return findMatches(
                incident.getTitle(),
                incident.getDescription(),
                incident.getService().getId(),
                incident.getId()
        );
    }

    private double score(String title, String description, Long serviceId, Incident candidate) {
        double score = TextSimilarity.incidentSimilarity(
                title,
                description,
                candidate.getTitle(),
                candidate.getDescription()
        );
        if (serviceId != null && serviceId.equals(candidate.getService().getId())) {
            score += 0.18;
        }
        if (candidate.getStatus() == IncidentStatus.RESOLVED) {
            score -= 0.05;
        }
        return Math.min(1.0, Math.max(0.0, Math.round(score * 100.0) / 100.0));
    }
}

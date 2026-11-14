package com.example.itsm.intelligence;

import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.IncidentStatusGroups;
import org.springframework.data.domain.PageRequest;
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
    private static final int LOOKBACK_DAYS = 30;
    private static final int MAX_CANDIDATES = 500;
    /** Ids start at 1, so this excludes nothing when there is no incident to exclude yet. */
    private static final long NO_INCIDENT = -1L;

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
        Instant since = clock.instant().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        // The database does the cheap filtering (status, age, exclusion) and returns at most the
        // newest MAX_CANDIDATES rows, instead of loading every incident into memory.
        List<Incident> candidates = incidentRepository.findDuplicateCandidates(
                IncidentStatusGroups.DUPLICATE_SCOPE,
                since,
                excludeIncidentId == null ? NO_INCIDENT : excludeIncidentId,
                PageRequest.of(0, MAX_CANDIDATES)
        );

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

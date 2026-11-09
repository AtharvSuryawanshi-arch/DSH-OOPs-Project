package com.example.itsm.intelligence;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResolutionSuggestionService {

    public List<ResolutionSuggestion> suggest(
            String title,
            String description,
            List<com.example.itsm.incident.Incident> resolvedIncidents
    ) {
        return resolvedIncidents.stream()
                .map(incident -> new ResolutionSuggestion(
                        incident.getId(),
                        incident.getReference(),
                        incident.getTitle(),
                        incident.getResolutionNotes(),
                        incident.getResolvedAt(),
                        Math.round(TextSimilarity.incidentSimilarity(
                                title,
                                description,
                                incident.getTitle(),
                                incident.getDescription()
                        ) * 100.0) / 100.0
                ))
                .filter(suggestion -> suggestion.score() >= 0.15)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(5)
                .toList();
    }
}

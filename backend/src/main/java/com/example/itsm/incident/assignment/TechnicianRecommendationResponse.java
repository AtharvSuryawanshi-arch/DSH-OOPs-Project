package com.example.itsm.incident.assignment;

import java.util.List;

public record TechnicianRecommendationResponse(
        Long technicianId,
        String name,
        List<String> skills,
        long activeIncidents,
        Integer maxCapacity,
        boolean skillMatch,
        long serviceHistory,
        double score,
        boolean available,
        boolean hasCapacity
) {
}

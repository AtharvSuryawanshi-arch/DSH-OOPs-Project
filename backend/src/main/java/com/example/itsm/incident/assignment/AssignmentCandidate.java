package com.example.itsm.incident.assignment;

import com.example.itsm.user.Technician;

public record AssignmentCandidate(
        Technician technician,
        long activeIncidents,
        long serviceHistory,
        boolean skillMatch
) {

    public boolean hasCapacity() {
        return technician.isAvailable()
                && technician.getMaxCapacity() != null
                && activeIncidents < technician.getMaxCapacity();
    }

    public double loadRatio() {
        if (technician.getMaxCapacity() == null || technician.getMaxCapacity() <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) activeIncidents / technician.getMaxCapacity());
    }

    public double recommendationScore() {
        double score = 0;
        if (skillMatch) {
            score += 40;
        }
        score += (1.0 - loadRatio()) * 30;
        int experience = technician.getExperienceYears() == null ? 0 : technician.getExperienceYears();
        score += Math.min(experience, 10) / 10.0 * 15;
        score += Math.min(serviceHistory, 5) / 5.0 * 15;
        if (!technician.isAvailable()) {
            score -= 20;
        }
        if (!hasCapacity()) {
            score -= 20;
        }
        return Math.round(score * 10.0) / 10.0;
    }
}

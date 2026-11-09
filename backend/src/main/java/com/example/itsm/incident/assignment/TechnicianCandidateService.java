package com.example.itsm.incident.assignment;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.user.Technician;
import com.example.itsm.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TechnicianCandidateService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    public TechnicianCandidateService(UserRepository userRepository, IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<AssignmentCandidate> candidatesFor(Incident incident) {
        List<Technician> technicians = userRepository.findActiveTechniciansWithSkills();
        return technicians.stream()
                .map(technician -> {
                    long active = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                            technician.getId(),
                            IncidentStatusGroups.ACTIVE_WORK
                    );
                    long history = incidentRepository.countByAssignedTechnicianIdAndServiceIdAndStatusIn(
                            technician.getId(),
                            incident.getService().getId(),
                            IncidentStatusGroups.RESOLVED_STATUSES
                    );
                    return new AssignmentCandidate(
                            technician,
                            active,
                            history,
                            technician.hasSkill(incident.getCategory())
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicianRecommendationResponse> recommendationsFor(Incident incident) {
        return candidatesFor(incident).stream()
                .sorted((left, right) -> Double.compare(
                        right.recommendationScore(),
                        left.recommendationScore()
                ))
                .map(candidate -> new TechnicianRecommendationResponse(
                        candidate.technician().getId(),
                        candidate.technician().getName(),
                        candidate.technician().getSkills().stream().map(skill -> skill.getName()).toList(),
                        candidate.activeIncidents(),
                        candidate.technician().getMaxCapacity(),
                        candidate.skillMatch(),
                        candidate.serviceHistory(),
                        candidate.recommendationScore(),
                        candidate.technician().isAvailable(),
                        candidate.hasCapacity()
                ))
                .toList();
    }
}

package com.example.itsm.incident.assignment;

import com.example.itsm.audit.AuditAction;
import com.example.itsm.audit.AuditService;
import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.incident.lifecycle.IncidentWorkflowService;
import com.example.itsm.notification.NotificationEvents;
import com.example.itsm.settings.SettingsService;
import com.example.itsm.user.Technician;
import com.example.itsm.user.User;
import com.example.itsm.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final TechnicianCandidateService candidateService;
    private final AssignmentStrategyFactory strategyFactory;
    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentWorkflowService workflowService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public AssignmentService(
            TechnicianCandidateService candidateService,
            AssignmentStrategyFactory strategyFactory,
            SettingsService settingsService,
            UserRepository userRepository,
            IncidentRepository incidentRepository,
            IncidentWorkflowService workflowService,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.candidateService = candidateService;
        this.strategyFactory = strategyFactory;
        this.settingsService = settingsService;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Optional<Incident> autoAssign(Incident incident) {
        if (incident.getAssignedTechnician() != null || incident.getStatus() != IncidentStatus.CLASSIFIED) {
            return Optional.empty();
        }

        AssignmentStrategyType strategyType = settingsService.assignmentStrategy();
        List<AssignmentCandidate> candidates = candidateService.candidatesFor(incident);
        Optional<AssignmentCandidate> selected = strategyFactory.get(strategyType).select(incident, candidates);

        selected.ifPresent(candidate -> assignInternal(
                incident,
                candidate.technician(),
                null,
                strategyType.getLabel() + " strategy"
        ));

        return selected.map(candidate -> incident);
    }

    @Transactional
    public Incident assignManually(Incident incident, Long technicianId, User actor) {
        // Fail early with a clear message; the state classes would refuse anyway.
        if (!incident.getStatus().isActiveWork()) {
            throw new BusinessRuleException(
                    "ASSIGNMENT_NOT_ALLOWED",
                    "Technicians cannot be assigned once an incident is resolved, closed or cancelled."
            );
        }

        Technician technician = userRepository.findTechnicianWithSkills(technicianId)
                .orElseThrow(() -> ResourceNotFoundException.of("Technician", technicianId));

        if (!technician.isActive() || !technician.isAvailable()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_UNAVAILABLE",
                    technician.getName() + " is not currently available."
            );
        }

        long active = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                technician.getId(),
                IncidentStatusGroups.ACTIVE_WORK
        );
        boolean alreadyAssigned = incident.getAssignedTechnician() != null
                && incident.getAssignedTechnician().getId().equals(technician.getId());
        if (!alreadyAssigned && technician.getMaxCapacity() != null && active >= technician.getMaxCapacity()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_AT_CAPACITY",
                    technician.getName() + " is already at maximum workload."
            );
        }

        assignInternal(incident, technician, actor, "Manual assignment");
        return incident;
    }

    private void assignInternal(Incident incident, Technician technician, User actor, String reason) {
        // Both automatic and manual assignment pass through here, so this rule always holds.
        if (incident.getReporter().getId().equals(technician.getId())) {
            throw new BusinessRuleException(
                    "SELF_ASSIGNMENT_NOT_ALLOWED",
                    technician.getName() + " reported this incident and cannot also be assigned to it."
            );
        }

        Technician previous = incident.getAssignedTechnician();
        boolean reassignment = previous != null;
        if (reassignment && previous.getId().equals(technician.getId())) {
            return;
        }

        IncidentStatus fromStatus = incident.getStatus();
        if (fromStatus == IncidentStatus.NEW) {
            workflowService.classify(incident);
        }
        // Polymorphism: ClassifiedState assigns and moves to ASSIGNED; Assigned/Acknowledged/InProgress
        // states just swap the technician; resolved or closed states refuse.
        workflowService.assign(incident, technician);

        auditService.record(
                incident,
                actor,
                reassignment ? AuditAction.REASSIGNED : AuditAction.ASSIGNED,
                fromStatus,
                incident.getStatus(),
                (reassignment
                        ? "Reassigned from " + previous.getName() + " to " + technician.getName()
                        : "Assigned to " + technician.getName())
                        + " (" + reason + ")."
        );

        eventPublisher.publishEvent(new NotificationEvents.IncidentAssigned(incident, actor));
    }
}

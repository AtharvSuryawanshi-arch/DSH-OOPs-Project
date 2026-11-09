package com.example.itsm.audit;

import com.example.itsm.common.PageResponse;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final IncidentAuditRepository auditRepository;
    private final Clock clock;

    public AuditService(IncidentAuditRepository auditRepository, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(
            Incident incident,
            User actor,
            AuditAction action,
            IncidentStatus fromState,
            IncidentStatus toState,
            String description
    ) {
        auditRepository.save(new IncidentAudit(
                incident,
                actor,
                action,
                fromState,
                toState,
                description,
                clock.instant()
        ));
    }

    @Transactional
    public void recordAt(
            Incident incident,
            User actor,
            AuditAction action,
            IncidentStatus fromState,
            IncidentStatus toState,
            String description,
            Instant timestamp
    ) {
        auditRepository.save(new IncidentAudit(
                incident,
                actor,
                action,
                fromState,
                toState,
                description,
                timestamp
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> forIncident(Long incidentId) {
        return auditRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(AuditResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> globalAudit(Pageable pageable) {
        return PageResponse.from(
                auditRepository.findAllByOrderByCreatedAtDesc(pageable),
                AuditResponse::from
        );
    }
}

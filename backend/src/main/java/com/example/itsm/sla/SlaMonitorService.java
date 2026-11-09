package com.example.itsm.sla;

import com.example.itsm.audit.AuditAction;
import com.example.itsm.audit.AuditService;
import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.incident.Priority;
import com.example.itsm.notification.NotificationEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SlaMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitorService.class);

    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final long escalationIntervalMinutes;

    public SlaMonitorService(
            IncidentRepository incidentRepository,
            AuditService auditService,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            @Value("${app.sla.escalation-interval-minutes:15}") long escalationIntervalMinutes
    ) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.escalationIntervalMinutes = Math.max(1, escalationIntervalMinutes);
    }

    @Scheduled(
            fixedDelayString = "${app.sla.monitor-interval-ms:30000}",
            initialDelayString = "${app.sla.monitor-initial-delay-ms:15000}"
    )
    @Transactional
    public void scan() {
        Instant now = clock.instant();
        List<Incident> monitored = incidentRepository.findByStatusIn(IncidentStatusGroups.UNRESOLVED);
        int changes = 0;

        for (Incident incident : monitored) {
            changes += checkResponseWarning(incident, now);
            changes += checkResolutionWarning(incident, now);
            changes += checkResponseBreach(incident, now);
            changes += checkResolutionBreach(incident, now);
            changes += checkEscalation(incident, now);
        }

        if (changes > 0) {
            log.info("SLA monitor processed {} change(s) across {} open incident(s).", changes, monitored.size());
        }
    }

    private int checkResponseWarning(Incident incident, Instant now) {
        if (incident.getAcknowledgedAt() == null
                && !incident.isResponseWarningSent()
                && incident.getResponseWarningAt() != null
                && now.isAfter(incident.getResponseWarningAt())) {
            incident.markResponseWarningSent();
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_WARNING,
                    incident.getStatus(),
                    incident.getStatus(),
                    "Response SLA warning threshold crossed."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaWarning(incident, "RESPONSE"));
            return 1;
        }
        return 0;
    }

    private int checkResolutionWarning(Incident incident, Instant now) {
        if (incident.getResolvedAt() == null
                && !incident.isResolutionWarningSent()
                && incident.getResolutionWarningAt() != null
                && now.isAfter(incident.getResolutionWarningAt())) {
            incident.markResolutionWarningSent();
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_WARNING,
                    incident.getStatus(),
                    incident.getStatus(),
                    "Resolution SLA warning threshold crossed."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaWarning(incident, "RESOLUTION"));
            return 1;
        }
        return 0;
    }

    private int checkResponseBreach(Incident incident, Instant now) {
        if (incident.getAcknowledgedAt() == null
                && !incident.isResponseBreached()
                && incident.getResponseDueAt() != null
                && now.isAfter(incident.getResponseDueAt())) {
            incident.markResponseBreached();
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_BREACHED,
                    incident.getStatus(),
                    incident.getStatus(),
                    "Response SLA deadline passed without acknowledgement."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaBreached(incident, "RESPONSE"));
            return 1;
        }
        return 0;
    }

    private int checkResolutionBreach(Incident incident, Instant now) {
        if (incident.getResolvedAt() == null
                && !incident.isResolutionBreached()
                && incident.getResolutionDueAt() != null
                && now.isAfter(incident.getResolutionDueAt())) {
            incident.markResolutionBreached();
            auditService.record(
                    incident,
                    null,
                    AuditAction.SLA_BREACHED,
                    incident.getStatus(),
                    incident.getStatus(),
                    "Resolution SLA deadline passed."
            );
            eventPublisher.publishEvent(new NotificationEvents.SlaBreached(incident, "RESOLUTION"));
            return 1;
        }
        return 0;
    }

    private int checkEscalation(Incident incident, Instant now) {
        if (incident.getAcknowledgedAt() != null
                || incident.getResponseDueAt() == null
                || incident.getPriority().getRank() > Priority.P2.getRank()
                || !now.isAfter(incident.getResponseDueAt())) {
            return 0;
        }

        long minutesPast = Duration.between(incident.getResponseDueAt(), now).toMinutes();
        int level = (int) Math.min(3, 1 + minutesPast / escalationIntervalMinutes);
        if (level <= incident.getEscalationLevel()) {
            return 0;
        }

        incident.escalate(level, now);
        String description = "Auto-escalated to level " + level
                + " because the incident was not acknowledged within its response SLA.";
        auditService.record(
                incident,
                null,
                AuditAction.ESCALATED,
                incident.getStatus(),
                incident.getStatus(),
                description
        );
        eventPublisher.publishEvent(new NotificationEvents.Escalated(incident, level));
        return 1;
    }
}

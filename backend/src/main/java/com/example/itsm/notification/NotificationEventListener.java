package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final NotificationRecipients recipients;

    public NotificationEventListener(NotificationService notificationService, NotificationRecipients recipients) {
        this.notificationService = notificationService;
        this.recipients = recipients;
    }

    @EventListener
    public void onCreated(NotificationEvents.IncidentCreated event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getReporter());
        if (incident.getPriority().isCritical()) {
            recipients.admins().forEach(admin -> add(targets, admin));
        }

        notify(
                targets.values(),
                incident,
                NotificationType.INCIDENT_CREATED,
                "Incident " + incident.getReference() + " created",
                "%s was classified as %s (%s). The SLA resolution target is %s."
                        .formatted(incident.getReference(), incident.getPriority().name(), incident.getPriority().getLabel(),
                                incident.getResolutionDueAt())
        );
    }

    @EventListener
    public void onAssigned(NotificationEvents.IncidentAssigned event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getAssignedTechnician());
        add(targets, incident.getReporter());

        notify(
                targets.values(),
                incident,
                NotificationType.INCIDENT_ASSIGNED,
                "Incident " + incident.getReference() + " assigned",
                "%s was assigned to %s."
                        .formatted(incident.getReference(), incident.getAssignedTechnician().getName())
        );
    }

    @EventListener
    public void onStatusChanged(NotificationEvents.StatusChanged event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getReporter());
        add(targets, incident.getAssignedTechnician());
        if (event.to() == com.example.itsm.incident.IncidentStatus.CLOSED) {
            recipients.admins().forEach(admin -> add(targets, admin));
        }
        targets.remove(event.actor() == null ? null : event.actor().getId());

        String actor = event.actor() == null ? "System" : event.actor().getName();
        notify(
                targets.values(),
                incident,
                NotificationType.STATUS_CHANGED,
                "Incident " + incident.getReference() + " is now " + event.to().getLabel(),
                "%s moved from %s to %s by %s."
                        .formatted(incident.getReference(), event.from().getLabel(), event.to().getLabel(), actor)
        );
    }

    @EventListener
    public void onCommentAdded(NotificationEvents.CommentAdded event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        if (event.internal()) {
            add(targets, incident.getAssignedTechnician());
            recipients.admins().forEach(admin -> add(targets, admin));
        } else {
            add(targets, incident.getReporter());
            add(targets, incident.getAssignedTechnician());
            if (incident.getAssignedTechnician() == null) {
                recipients.admins().forEach(admin -> add(targets, admin));
            }
        }
        targets.remove(event.actor() == null ? null : event.actor().getId());

        notify(
                targets.values(),
                incident,
                NotificationType.COMMENT_ADDED,
                "New " + (event.internal() ? "internal note" : "comment") + " on " + incident.getReference(),
                "%s added a comment to %s.".formatted(event.actor().getName(), incident.getReference())
        );
    }

    @EventListener
    public void onSlaWarning(NotificationEvents.SlaWarning event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getAssignedTechnician());
        recipients.admins().forEach(admin -> add(targets, admin));

        notify(
                targets.values(),
                incident,
                NotificationType.SLA_WARNING,
                "SLA warning on " + incident.getReference(),
                "The %s SLA for %s is approaching its deadline."
                        .formatted(event.target().toLowerCase(), incident.getReference())
        );
    }

    @EventListener
    public void onSlaBreached(NotificationEvents.SlaBreached event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getReporter());
        add(targets, incident.getAssignedTechnician());
        recipients.admins().forEach(admin -> add(targets, admin));

        notify(
                targets.values(),
                incident,
                NotificationType.SLA_BREACHED,
                "SLA breached on " + incident.getReference(),
                "The %s SLA target for %s has been exceeded."
                        .formatted(event.target().toLowerCase(), incident.getReference())
        );
    }

    @EventListener
    public void onEscalated(NotificationEvents.Escalated event) {
        Incident incident = event.incident();
        notify(
                recipients.admins(),
                incident,
                NotificationType.ESCALATION,
                "Incident " + incident.getReference() + " escalated",
                "%s escalated to level %d because it has not been acknowledged within its response SLA."
                        .formatted(incident.getReference(), event.level())
        );
    }

    @EventListener
    public void onDuplicateDetected(NotificationEvents.DuplicateDetected event) {
        Incident incident = event.incident();
        Map<Long, User> targets = new LinkedHashMap<>();
        add(targets, incident.getReporter());
        recipients.admins().forEach(admin -> add(targets, admin));

        notify(
                targets.values(),
                incident,
                NotificationType.DUPLICATE_DETECTED,
                "Possible duplicate: " + incident.getReference(),
                "%s looks similar to %s and has been linked for review."
                        .formatted(incident.getReference(), event.original().getReference())
        );
    }

    private void notify(
            Iterable<User> targets,
            Incident incident,
            NotificationType type,
            String title,
            String message
    ) {
        for (User target : targets) {
            if (target == null || !target.isActive()) {
                continue;
            }
            notificationService.send(NotificationChannel.IN_APP, target, incident, type, title, message);
            notificationService.send(NotificationChannel.EMAIL, target, incident, type, title, message);
            log.info("[SIMULATED EMAIL] To: {} <{}> | Subject: {} | {}",
                    target.getName(), target.getEmail(), title, message);
        }
    }

    private void add(Map<Long, User> targets, User user) {
        if (user != null) {
            targets.put(user.getId(), user);
        }
    }
}

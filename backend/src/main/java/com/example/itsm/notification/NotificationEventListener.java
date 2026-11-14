package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Observer: reacts to domain events by deciding WHO should hear about them and WHAT to say.
 * HOW the message is delivered (in-app, email, …) is left to {@link NotificationDispatcher}.
 */
@Component
public class NotificationEventListener {

    private final NotificationDispatcher dispatcher;
    private final NotificationRecipients recipients;

    public NotificationEventListener(NotificationDispatcher dispatcher, NotificationRecipients recipients) {
        this.dispatcher = dispatcher;
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
        dispatcher.dispatch(targets, incident, type, title, message);
    }

    private void add(Map<Long, User> targets, User user) {
        if (user != null) {
            targets.put(user.getId(), user);
        }
    }
}

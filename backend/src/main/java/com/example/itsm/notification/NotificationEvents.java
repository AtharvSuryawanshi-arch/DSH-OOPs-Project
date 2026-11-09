package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.incident.IncidentStatus;
import com.example.itsm.user.User;

public final class NotificationEvents {

    private NotificationEvents() {
    }

    public record IncidentCreated(Incident incident, User actor) {
    }

    public record IncidentAssigned(Incident incident, User actor) {
    }

    public record StatusChanged(Incident incident, IncidentStatus from, IncidentStatus to, User actor) {
    }

    public record CommentAdded(Incident incident, User actor, boolean internal) {
    }

    public record SlaWarning(Incident incident, String target) {
    }

    public record SlaBreached(Incident incident, String target) {
    }

    public record Escalated(Incident incident, int level) {
    }

    public record DuplicateDetected(Incident incident, Incident original) {
    }
}

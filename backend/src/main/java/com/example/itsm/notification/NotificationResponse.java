package com.example.itsm.notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String channel,
        String type,
        String title,
        String message,
        boolean read,
        Instant createdAt,
        Long incidentId,
        String incidentReference
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel().name(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getIncident() == null ? null : notification.getIncident().getId(),
                notification.getIncident() == null ? null : notification.getIncident().getReference()
        );
    }
}

package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;

/** One message for one person, independent of how it is delivered. */
public record NotificationMessage(
        User recipient,
        Incident incident,
        NotificationType type,
        String title,
        String body
) {
}

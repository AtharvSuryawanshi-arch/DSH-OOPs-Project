package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationFactory {

    public Notification create(
            NotificationChannel channel,
            User recipient,
            Incident incident,
            NotificationType type,
            String title,
            String message,
            Instant createdAt
    ) {
        return new Notification(
                recipient,
                incident,
                channel,
                type,
                trim(title, 200),
                trim(message, 1000),
                createdAt
        );
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }
}

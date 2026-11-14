package com.example.itsm.notification;

import org.springframework.stereotype.Component;

/** Stores the notification so it appears in the bell and on the Notifications page. */
@Component
public class InAppNotificationSender implements NotificationSender {

    private final NotificationService notificationService;

    public InAppNotificationSender(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void deliver(NotificationMessage message) {
        notificationService.send(
                channel(),
                message.recipient(),
                message.incident(),
                message.type(),
                message.title(),
                message.body()
        );
    }
}

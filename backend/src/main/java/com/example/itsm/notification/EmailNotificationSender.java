package com.example.itsm.notification;

import org.springframework.stereotype.Component;

/**
 * Adapter: turns a {@link NotificationMessage} into what an email provider needs (address, name,
 * subject, body) and hands it to the {@link EmailGateway}. A copy is also stored so the user can see
 * which emails were sent.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private final EmailGateway emailGateway;
    private final NotificationService notificationService;

    public EmailNotificationSender(EmailGateway emailGateway, NotificationService notificationService) {
        this.emailGateway = emailGateway;
        this.notificationService = notificationService;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(NotificationMessage message) {
        emailGateway.send(
                message.recipient().getEmail(),
                message.recipient().getName(),
                "[I-ITSM] " + message.title(),
                message.body()
        );
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

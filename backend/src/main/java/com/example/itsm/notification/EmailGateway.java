package com.example.itsm.notification;

/**
 * The outside email service, as the application sees it (target of the Adapter pattern).
 * {@link LoggingEmailGateway} simulates it; a real provider (SMTP, SendGrid, SES) would be another
 * implementation of this interface, with no change to {@link EmailNotificationSender}.
 */
public interface EmailGateway {

    void send(String toAddress, String toName, String subject, String body);
}

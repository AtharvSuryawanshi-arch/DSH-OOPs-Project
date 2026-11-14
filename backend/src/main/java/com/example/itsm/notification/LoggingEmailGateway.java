package com.example.itsm.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Simulated email provider: writes the email to the application log instead of sending it. */
@Component
public class LoggingEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailGateway.class);

    @Override
    public void send(String toAddress, String toName, String subject, String body) {
        log.info("[SIMULATED EMAIL] To: {} <{}> | Subject: {} | {}", toName, toAddress, subject, body);
    }
}

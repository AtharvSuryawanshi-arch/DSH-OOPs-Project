package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sends each message through every registered {@link NotificationSender}. Spring injects all
 * implementations, so the dispatcher never names a specific channel (polymorphism instead of if/else).
 */
@Component
public class NotificationDispatcher {

    private final List<NotificationSender> senders;

    public NotificationDispatcher(List<NotificationSender> senders) {
        this.senders = List.copyOf(senders);
    }

    public void dispatch(Iterable<User> recipients, Incident incident, NotificationType type, String title, String body) {
        for (User recipient : recipients) {
            if (recipient == null || !recipient.isActive()) {
                continue;
            }
            NotificationMessage message = new NotificationMessage(recipient, incident, type, title, body);
            for (NotificationSender sender : senders) {
                sender.deliver(message);
            }
        }
    }

    /** The channels currently wired in, e.g. for documentation or diagnostics. */
    public List<NotificationChannel> channels() {
        return senders.stream().map(NotificationSender::channel).toList();
    }
}

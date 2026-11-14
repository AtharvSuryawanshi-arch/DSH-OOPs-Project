package com.example.itsm.notification;

import com.example.itsm.incident.Incident;
import com.example.itsm.user.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationChannelsTest {

    /** A pretend extra channel: shows a new mechanism needs only a new NotificationSender class. */
    private static final class RecordingSender implements NotificationSender {
        private final NotificationChannel channel;
        private final List<NotificationMessage> delivered = new ArrayList<>();

        RecordingSender(NotificationChannel channel) {
            this.channel = channel;
        }

        @Override
        public NotificationChannel channel() {
            return channel;
        }

        @Override
        public void deliver(NotificationMessage message) {
            delivered.add(message);
        }
    }

    private User user(String name, boolean active) {
        User user = mock(User.class);
        when(user.getName()).thenReturn(name);
        when(user.getEmail()).thenReturn(name.toLowerCase() + "@example.test");
        when(user.isActive()).thenReturn(active);
        return user;
    }

    @Test
    void dispatcherSendsEveryMessageThroughEveryRegisteredChannel() {
        RecordingSender inApp = new RecordingSender(NotificationChannel.IN_APP);
        RecordingSender email = new RecordingSender(NotificationChannel.EMAIL);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(inApp, email));
        User neha = user("Neha", true);
        User former = user("Former", false);

        dispatcher.dispatch(List.of(neha, former), mock(Incident.class), NotificationType.STATUS_CHANGED, "Resolved", "It is fixed.");

        assertThat(dispatcher.channels()).containsExactly(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        // Each active recipient gets one message per channel; deactivated users get nothing.
        assertThat(inApp.delivered).extracting(NotificationMessage::recipient).containsExactly(neha);
        assertThat(email.delivered).extracting(NotificationMessage::recipient).containsExactly(neha);
        assertThat(inApp.delivered.getFirst().title()).isEqualTo("Resolved");
    }

    @Test
    void emailSenderAdaptsTheMessageForTheEmailGatewayAndKeepsACopy() {
        EmailGateway gateway = mock(EmailGateway.class);
        NotificationService store = mock(NotificationService.class);
        EmailNotificationSender sender = new EmailNotificationSender(gateway, store);
        User neha = user("Neha", true);
        Incident incident = mock(Incident.class);

        sender.deliver(new NotificationMessage(neha, incident, NotificationType.SLA_WARNING, "SLA warning", "Deadline soon."));

        verify(gateway).send("neha@example.test", "Neha", "[I-ITSM] SLA warning", "Deadline soon.");
        verify(store).send(NotificationChannel.EMAIL, neha, incident, NotificationType.SLA_WARNING, "SLA warning", "Deadline soon.");
    }
}

package com.example.itsm.notification;

import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.Incident;
import com.example.itsm.security.CurrentUserService;
import com.example.itsm.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationFactory notificationFactory,
            CurrentUserService currentUserService,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationFactory = notificationFactory;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    public void send(
            NotificationChannel channel,
            User recipient,
            Incident incident,
            NotificationType type,
            String title,
            String message
    ) {
        if (recipient == null || !recipient.isActive()) {
            return;
        }
        notificationRepository.save(notificationFactory.create(
                channel,
                recipient,
                incident,
                type,
                title,
                message,
                clock.instant()
        ));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications() {
        return notificationRepository
                .findTop100ByRecipientIdOrderByCreatedAtDesc(currentUserService.id())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientIdAndReadFalse(currentUserService.id());
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = notificationRepository
                .findByIdAndRecipientId(id, currentUserService.id())
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        notification.markRead();
    }

    @Transactional
    public int markAllRead() {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(currentUserService.id());
        unread.forEach(Notification::markRead);
        return unread.size();
    }
}

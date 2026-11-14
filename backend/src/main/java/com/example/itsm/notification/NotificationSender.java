package com.example.itsm.notification;

/**
 * One notification mechanism (Strategy pattern). Every Spring bean implementing this interface is
 * picked up by {@link NotificationDispatcher}, so adding a mechanism (SMS, push, chat) means adding one
 * class, without changing the event listener or any incident code.
 */
public interface NotificationSender {

    /** The channel this sender delivers through; also stored on the notification record. */
    NotificationChannel channel();

    void deliver(NotificationMessage message);
}

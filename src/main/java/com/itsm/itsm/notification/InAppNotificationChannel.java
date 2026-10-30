package com.itsm.itsm.notification;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.User;
import org.springframework.stereotype.Component;

// Channel 1: saves the message so it appears on the user's Notifications page
@Component
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    public InAppNotificationChannel(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void send(User recipient, Incident incident, String message) {
        notificationRepository.save(new Notification(recipient, incident, message));
    }
}

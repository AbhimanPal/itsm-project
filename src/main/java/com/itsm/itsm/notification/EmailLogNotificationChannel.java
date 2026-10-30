package com.itsm.itsm.notification;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Channel 2: a simulated email. It writes the email to the console log
// instead of really sending it (the PDF allows simulation).
@Component
public class EmailLogNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailLogNotificationChannel.class);

    @Override
    public void send(User recipient, Incident incident, String message) {
        log.info("[EMAIL] To: {} | Subject: {} update | {}", recipient.getEmail(), incident.getNumber(), message);
    }
}

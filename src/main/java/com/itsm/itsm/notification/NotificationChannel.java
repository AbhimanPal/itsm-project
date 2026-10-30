package com.itsm.itsm.notification;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.User;

// One way of delivering a message (in-app, email, SMS, ...).
// Adding a new channel = writing one new class that implements this.
public interface NotificationChannel {

    void send(User recipient, Incident incident, String message);
}

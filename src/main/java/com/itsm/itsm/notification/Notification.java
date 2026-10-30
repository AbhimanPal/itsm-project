package com.itsm.itsm.notification;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// An in-app message shown on the user's Notifications page
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User recipient;

    @ManyToOne
    private Incident incident;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private boolean seen;

    protected Notification() {
    }

    public Notification(User recipient, Incident incident, String message) {
        this.recipient = recipient;
        this.incident = incident;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public void markSeen() {
        seen = true;
    }

    public Long getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isSeen() {
        return seen;
    }
}

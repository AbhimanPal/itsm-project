package com.itsm.itsm.audit;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.IncidentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

// One line of history: when, who, what, which incident, status before and after.
// Records are only ever added, never changed, so there are no setters.
@Entity
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @ManyToOne(optional = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    private IncidentStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private IncidentStatus newStatus;

    @Column(length = 1000)
    private String description;

    protected AuditRecord() {
    }

    public AuditRecord(String actor, String action, Incident incident,
                       IncidentStatus oldStatus, IncidentStatus newStatus, String description) {
        this.timestamp = LocalDateTime.now();
        this.actor = actor;
        this.action = action;
        this.incident = incident;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public Incident getIncident() {
        return incident;
    }

    public IncidentStatus getOldStatus() {
        return oldStatus;
    }

    public IncidentStatus getNewStatus() {
        return newStatus;
    }

    public String getDescription() {
        return description;
    }
}

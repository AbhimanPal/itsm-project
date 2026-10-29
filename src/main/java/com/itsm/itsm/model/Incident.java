package com.itsm.itsm.model;

import com.itsm.itsm.exception.InvalidStateTransitionException;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    // Associations: an incident refers to other objects that exist on their own
    @ManyToOne(optional = false)
    private Employee reporter;

    @ManyToOne(optional = false)
    private Category category;

    @ManyToOne(optional = false)
    private ITService service;

    @ManyToOne
    private Technician assignedTechnician;

    private int impact;          // 1-3
    private int urgency;         // 1-3
    private int usersAffected;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    // Encapsulation: there is no setStatus(). The only way to change the
    // status is changeStatus(), which checks the lifecycle rules.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 2000)
    private String resolution;

    private LocalDateTime closedAt;

    protected Incident() {
    }

    public Incident(String title, String description, Employee reporter, Category category,
                    ITService service, int impact, int urgency, int usersAffected) {
        this.title = title;
        this.description = description;
        this.reporter = reporter;
        this.category = category;
        this.service = service;
        this.impact = impact;
        this.urgency = urgency;
        this.usersAffected = usersAffected;
        this.status = IncidentStatus.NEW;
        this.createdAt = LocalDateTime.now();
    }

    public void changeStatus(IncidentStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStateTransitionException(status, next);
        }
        status = next;
        if (next == IncidentStatus.CLOSED) {
            closedAt = LocalDateTime.now();
        }
    }

    // Shown to users as INC0001, INC0002, ...
    public String getNumber() {
        return id == null ? "INC(new)" : String.format("INC%04d", id);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Employee getReporter() {
        return reporter;
    }

    public Category getCategory() {
        return category;
    }

    public ITService getService() {
        return service;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public int getImpact() {
        return impact;
    }

    public int getUrgency() {
        return urgency;
    }

    public int getUsersAffected() {
        return usersAffected;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getResolution() {
        return resolution;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}

package com.itsm.itsm.model;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.InvalidStateTransitionException;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Association to another incident: set when duplicate detection thinks
    // this report is about the same problem as an earlier open incident
    @ManyToOne
    private Incident duplicateOf;

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

    // SLA deadlines, set once from the matching SlaPolicy
    private LocalDateTime responseDueAt;
    private LocalDateTime resolutionDueAt;

    // When the technician actually responded and resolved
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;

    // Filled in by the SLA scheduler, so each warning and breach is only reported once
    private LocalDateTime slaWarningSentAt;
    private LocalDateTime slaBreachedAt;

    // Escalation when nobody acknowledges the incident in time.
    // Stored as null for incidents saved before this existed; the getter turns that into NONE.
    @Enumerated(EnumType.STRING)
    private EscalationLevel escalationLevel;
    private LocalDateTime escalatedAt;

    // Feedback from the reporter after the fix: rating 1-5 and a comment
    private Integer feedbackRating;

    @Column(length = 1000)
    private String feedbackComment;

    // Composition: the incident owns its notes. cascade = save/delete them with the incident.
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<IncidentNote> notes = new ArrayList<>();

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
        if (next == IncidentStatus.ACKNOWLEDGED) {
            acknowledgedAt = LocalDateTime.now();
        } else if (next == IncidentStatus.RESOLVED) {
            resolvedAt = LocalDateTime.now();
        } else if (next == IncidentStatus.IN_PROGRESS) {
            resolvedAt = null; // reopened: it is not resolved any more
        } else if (next == IncidentStatus.CLOSED) {
            closedAt = LocalDateTime.now();
        }
    }

    public void applySla(SlaPolicy policy) {
        responseDueAt = createdAt.plusMinutes(policy.getResponseMinutes());
        resolutionDueAt = createdAt.plusMinutes(policy.getResolutionMinutes());
    }

    // The admin overrides the calculated priority. The deadlines are worked out again
    // from the creation time with the new priority's SLA rule, and the 80% warning
    // can be sent again for the new deadlines. A violation already recorded stays recorded.
    public void changePriority(Priority newPriority, SlaPolicy policy) {
        if (!status.isOpen()) {
            throw new InvalidInputException("Only open incidents can change priority; this one is " + status + ".");
        }
        if (newPriority == priority) {
            throw new InvalidInputException("The incident already has priority " + priority + ".");
        }
        priority = newPriority;
        applySla(policy);
        slaWarningSentAt = null;
    }

    public void assignTo(Technician technician) {
        changeStatus(IncidentStatus.ASSIGNED);
        assignedTechnician = technician;
    }

    // Hand the incident to another technician while it still waits to be acknowledged
    // (used by escalation and by the admin). Once someone acknowledged it, it stays theirs.
    public void reassignTo(Technician technician) {
        if (status != IncidentStatus.ASSIGNED) {
            throw new InvalidStateTransitionException(status, IncidentStatus.ASSIGNED);
        }
        assignedTechnician = technician;
    }

    public void markAsDuplicateOf(Incident original) {
        duplicateOf = original;
    }

    public void resolve(String resolutionText) {
        changeStatus(IncidentStatus.RESOLVED);
        resolution = resolutionText;
    }

    public void giveFeedback(int rating, String comment) {
        feedbackRating = rating;
        feedbackComment = comment;
    }

    public void addNote(User author, NoteType type, String text) {
        notes.add(new IncidentNote(this, author, type, text));
    }

    public SlaStatus getSlaStatus() {
        return getSlaStatus(LocalDateTime.now());
    }

    // "now" is a parameter so tests can pretend it is a different time
    public SlaStatus getSlaStatus(LocalDateTime now) {
        if (resolutionDueAt == null) {
            return SlaStatus.NO_SLA;
        }
        if (acknowledgedAt != null && acknowledgedAt.isAfter(responseDueAt)) {
            return SlaStatus.BREACHED; // responded too late
        }
        if (resolvedAt != null) {
            return resolvedAt.isAfter(resolutionDueAt) ? SlaStatus.BREACHED : SlaStatus.MET;
        }
        if (now.isAfter(resolutionDueAt) || (acknowledgedAt == null && now.isAfter(responseDueAt))) {
            return SlaStatus.BREACHED;
        }
        long totalSeconds = Duration.between(createdAt, resolutionDueAt).toSeconds();
        long usedSeconds = Duration.between(createdAt, now).toSeconds();
        if (usedSeconds * 100 >= totalSeconds * 80) {
            return SlaStatus.AT_RISK; // 80% of the time is used up
        }
        return SlaStatus.ON_TRACK;
    }

    // ----- SLA monitoring (used by the scheduler) -----

    public boolean needsSlaWarning(LocalDateTime now) {
        return slaWarningSentAt == null && getSlaStatus(now) == SlaStatus.AT_RISK;
    }

    public void markSlaWarningSent(LocalDateTime now) {
        slaWarningSentAt = now;
    }

    public boolean needsBreachRecord(LocalDateTime now) {
        return slaBreachedAt == null && getSlaStatus(now) == SlaStatus.BREACHED;
    }

    public void recordBreach(LocalDateTime now) {
        slaBreachedAt = now;
    }

    // Which deadline was missed, for the audit trail and the notification
    public String describeBreach() {
        boolean respondedLate = acknowledgedAt == null || acknowledgedAt.isAfter(responseDueAt);
        return respondedLate ? "response deadline missed" : "resolution deadline missed";
    }

    // ----- Escalation -----

    public EscalationLevel getEscalationLevel() {
        return escalationLevel == null ? EscalationLevel.NONE : escalationLevel;
    }

    public void escalate(EscalationLevel level, LocalDateTime now) {
        escalationLevel = level;
        escalatedAt = now;
    }

    // When the next escalation step is due if still nobody acknowledges it:
    // first at the response deadline, then one more response window later.
    // null = nothing left to escalate to.
    public LocalDateTime getNextEscalationDueAt() {
        if (responseDueAt == null) {
            return null;
        }
        return switch (getEscalationLevel()) {
            case NONE -> responseDueAt;
            case TEAM_LEAD -> escalatedAt.plus(Duration.between(createdAt, responseDueAt));
            case ADMIN -> null;
        };
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

    public Incident getDuplicateOf() {
        return duplicateOf;
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

    public LocalDateTime getResponseDueAt() {
        return responseDueAt;
    }

    public LocalDateTime getResolutionDueAt() {
        return resolutionDueAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getSlaWarningSentAt() {
        return slaWarningSentAt;
    }

    public LocalDateTime getSlaBreachedAt() {
        return slaBreachedAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public Integer getFeedbackRating() {
        return feedbackRating;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    // Read-only view, so nobody can add notes without going through addNote()
    public List<IncidentNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }
}
